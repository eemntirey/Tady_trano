package mg.emit.tadytrano.application.service;

import lombok.RequiredArgsConstructor;
import mg.emit.tadytrano.application.dto.AnnonceCreateRequest;
import mg.emit.tadytrano.application.dto.AnnonceDetailResponse;
import mg.emit.tadytrano.application.dto.AnnonceResumeResponse;
import mg.emit.tadytrano.application.dto.AnnonceUpdateRequest;
import mg.emit.tadytrano.application.dto.ContactProprietaireResponse;
import mg.emit.tadytrano.application.dto.FavoriResponse;
import mg.emit.tadytrano.application.dto.PageResponse;
import mg.emit.tadytrano.application.dto.PhotoRequest;
import mg.emit.tadytrano.application.dto.PhotoResponse;
import mg.emit.tadytrano.application.dto.StatutAnnonceRequest;
import mg.emit.tadytrano.application.exception.ApiException;
import mg.emit.tadytrano.domain.model.Annonce;
import mg.emit.tadytrano.domain.model.Favori;
import mg.emit.tadytrano.domain.model.Proprietaire;
import mg.emit.tadytrano.domain.model.Quartier;
import mg.emit.tadytrano.domain.model.Photo;
import mg.emit.tadytrano.domain.model.StatutAnnonce;
import mg.emit.tadytrano.domain.model.TypeLocation;
import mg.emit.tadytrano.domain.repository.AnnonceRepository;
import mg.emit.tadytrano.domain.repository.FavoriRepository;
import mg.emit.tadytrano.domain.repository.PhotoRepository;
import mg.emit.tadytrano.domain.repository.TypeLocationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cycle de vie des annonces : recherche publique, publication par les
 * proprietaires, gestion des photos.
 */
@Service
@RequiredArgsConstructor
public class AnnonceService {

    private static final int TAILLE_MAX_PAGE = 50;
    private static final int MAX_PHOTOS_PAR_ANNONCE = 10;
    private static final double RAYON_TERRE_KM = 6371.0;

    private final AnnonceRepository annonceRepository;
    private final PhotoRepository photoRepository;
    private final FavoriRepository favoriRepository;
    private final TypeLocationRepository typeLocationRepository;
    private final ProprietaireService proprietaireService;
    private final ReferentielService referentielService;

    /**
     * Recherche publique : uniquement les annonces actives, filtres optionnels,
     * pagination serveur (15 a 20 par page attendu par l'app desktop).
     * Si lat/lng sont fournis, le tri est par distance croissante.
     */
    @Transactional(readOnly = true)
    public PageResponse<AnnonceResumeResponse> rechercher(Long typeLocationId, Long quartierId, Long villeId,
                                                          BigDecimal prixMin, BigDecimal prixMax,
                                                          Integer nbPiecesMin, Integer nbPiecesMax,
                                                          Double lat, Double lng, int page, int taille) {
        int tailleEffective = Math.min(Math.max(taille, 1), TAILLE_MAX_PAGE);
        Page<Annonce> resultats = annonceRepository.rechercher(
                StatutAnnonce.ACTIVE, typeLocationId, quartierId, villeId,
                prixMin, prixMax, nbPiecesMin, nbPiecesMax, lat, lng,
                PageRequest.of(Math.max(page, 0), tailleEffective));

        Map<Long, String> photosPrincipales = photosPrincipalesParAnnonce(
                resultats.getContent().stream().map(Annonce::getId).toList());

        List<AnnonceResumeResponse> contenus = resultats.getContent().stream()
                .map(a -> resume(a, photosPrincipales.get(a.getId()), distanceKm(lat, lng, a)))
                .toList();

        return new PageResponse<>(contenus, resultats.getNumber(), resultats.getSize(),
                resultats.getTotalElements(), resultats.getTotalPages());
    }

    /** Detail public ; contact du proprietaire renseigne si l'utilisateur est connecte.
     *  Si l'annonce n'est pas ACTIVE, elle reste visible uniquement au proprietaire
     *  ou a un administrateur ; sinon 404. */
    @Transactional(readOnly = true)
    public AnnonceDetailResponse detail(Long id) {
        Annonce annonce = annonceRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Annonce introuvable"));

        if (annonce.getStatut() != StatutAnnonce.ACTIVE && !estProprietaireOuAdmin(annonce)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Annonce introuvable");
        }

        List<PhotoResponse> photos = photoRepository.findByAnnonceIdOrderByOrdreAsc(id).stream()
                .map(PhotoResponse::depuis).toList();

        ContactProprietaireResponse contact = estConnecte()
                ? ContactProprietaireResponse.depuis(annonce.getProprietaire())
                : null;

        return AnnonceDetailResponse.depuis(annonce, photos, contact);
    }

    @Transactional
    public AnnonceDetailResponse creer(String email, AnnonceCreateRequest demande) {
        Proprietaire proprietaire = proprietaireService.parEmail(email);
        TypeLocation typeLocation = typeLocation(demande.typeLocationId());
        Quartier quartier = referentielService.quartier(demande.quartierId());

        Annonce annonce = new Annonce();
        annonce.setProprietaire(proprietaire);
        annonce.setTypeLocation(typeLocation);
        annonce.setQuartier(quartier);
        appliquer(annonce, demande.titre(), demande.description(), demande.prix(),
                demande.surface(), demande.nbPieces(), demande.latitude(), demande.longitude());

        return detail(annonceRepository.save(annonce).getId());
    }

    @Transactional
    public AnnonceDetailResponse modifier(String email, Long id, AnnonceUpdateRequest demande) {
        Annonce annonce = annonceDeProprietaire(email, id);
        TypeLocation typeLocation = typeLocation(demande.typeLocationId());
        Quartier quartier = referentielService.quartier(demande.quartierId());

        annonce.setTypeLocation(typeLocation);
        annonce.setQuartier(quartier);
        appliquer(annonce, demande.titre(), demande.description(), demande.prix(),
                demande.surface(), demande.nbPieces(), demande.latitude(), demande.longitude());

        annonceRepository.save(annonce);
        return detail(id);
    }

    @Transactional
    public void changerStatut(String email, Long id, StatutAnnonceRequest demande) {
        Annonce annonce = annonceDeProprietaire(email, id);
        if (annonce.isMasqueeParAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Cette annonce est masquee par la moderation ; contactez l'administration.");
        }
        // Un proprietaire peut masquer/reactiver sa propre annonce, mais jamais lever
        // le verrou pose par une moderation administrative.
        annonce.setStatut(demande.statut());
        annonceRepository.save(annonce);
    }

    /**
     * Moderation admin (F10) : change le statut de n'importe quelle annonce
     * sans verifier le proprietaire. Reserve au role ADMIN.
     */
    @Transactional
    public void modererAnnonce(Long id, StatutAnnonceRequest demande) {
        Annonce annonce = annonceRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Annonce introuvable"));
        annonce.setStatut(demande.statut());
        annonce.setMasqueeParAdmin(demande.statut() == StatutAnnonce.MASQUEE);
        annonceRepository.save(annonce);
    }

    /**
     * Liste paginee de toutes les annonces (tous statuts), reservee a l'admin.
     */
    @Transactional(readOnly = true)
    public PageResponse<AnnonceResumeResponse> toutesLesAnnonces(StatutAnnonce statut, int page, int taille) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0),
                Math.min(Math.max(taille, 1), TAILLE_MAX_PAGE));
        Page<Annonce> resultats = (statut != null)
                ? annonceRepository.findByStatutOrderByDatePublicationDesc(statut, pageable)
                : annonceRepository.findAllByOrderByDatePublicationDesc(pageable);

        Map<Long, String> photosPrincipales = photosPrincipalesParAnnonce(
                resultats.getContent().stream().map(Annonce::getId).toList());

        List<AnnonceResumeResponse> contenus = resultats.getContent().stream()
                .map(a -> resume(a, photosPrincipales.get(a.getId()), null))
                .toList();

        return new PageResponse<>(contenus, resultats.getNumber(), resultats.getSize(),
                resultats.getTotalElements(), resultats.getTotalPages());
    }

    @Transactional
    public void supprimer(String email, Long id) {
        Annonce annonce = annonceDeProprietaireVerrouillee(email, id);
        favoriRepository.deleteByAnnonceId(id);
        photoRepository.deleteByAnnonceId(id);
        annonceRepository.delete(annonce);
    }

    /** Annonces du proprietaire courant, tous statuts confondus, par date decroissante. */
    @Transactional(readOnly = true)
    public PageResponse<AnnonceResumeResponse> mesAnnonces(String email, int page, int taille) {
        Proprietaire proprietaire = proprietaireService.parEmail(email);
        PageRequest pageable = PageRequest.of(Math.max(page, 0),
                Math.min(Math.max(taille, 1), TAILLE_MAX_PAGE));
        Page<Annonce> decoupe = annonceRepository
                .findByProprietaireIdOrderByDatePublicationDesc(proprietaire.getId(), pageable);

        Map<Long, String> photosPrincipales = photosPrincipalesParAnnonce(
                decoupe.getContent().stream().map(Annonce::getId).toList());

        List<AnnonceResumeResponse> contenus = decoupe.getContent().stream()
                .map(a -> resume(a, photosPrincipales.get(a.getId()), null))
                .toList();

        return new PageResponse<>(contenus, decoupe.getNumber(), decoupe.getSize(),
                decoupe.getTotalElements(), decoupe.getTotalPages());
    }

    // ------------------------------------------------------------------
    // Photos
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<PhotoResponse> photos(Long id) {
        Annonce annonce = annonceRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Annonce introuvable"));

        if (annonce.getStatut() != StatutAnnonce.ACTIVE && !estProprietaireOuAdmin(annonce)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Annonce introuvable");
        }

        return photoRepository.findByAnnonceIdOrderByOrdreAsc(id).stream()
                .map(PhotoResponse::depuis).toList();
    }

    @Transactional
    public PhotoResponse ajouterPhoto(String email, Long annonceId, PhotoRequest demande) {
        // Le verrou pessimiste sur l'annonce reste détenu jusqu'au commit de cette transaction.
        Annonce annonce = annonceDeProprietaireVerrouillee(email, annonceId);
        List<Photo> existantes = photoRepository.findByAnnonceIdOrderByOrdreAsc(annonceId);
        if (existantes.size() >= MAX_PHOTOS_PAR_ANNONCE) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Nombre maximum de photos atteint (" + MAX_PHOTOS_PAR_ANNONCE + ")");
        }

        boolean aUnePrincipale = existantes.stream().anyMatch(Photo::isPrincipale);
        if (demande.principale()) {
            existantes.forEach(p -> p.setPrincipale(false));
            photoRepository.saveAll(existantes);
        } else if (!existantes.isEmpty() && !aUnePrincipale) {
            existantes.get(0).setPrincipale(true);
            photoRepository.saveAll(existantes);
        }

        int ordreSuivant = existantes.stream().mapToInt(Photo::getOrdre).max().orElse(0) + 1;
        Photo photo = new Photo();
        photo.setAnnonce(annonce);
        photo.setUrl(demande.url());
        photo.setPrincipale(demande.principale() || existantes.isEmpty());
        photo.setOrdre(ordreSuivant);
        return PhotoResponse.depuis(photoRepository.save(photo));
    }

    @Transactional
    public void supprimerPhoto(String email, Long annonceId, Long photoId) {
        annonceDeProprietaireVerrouillee(email, annonceId);
        Photo photo = photoRepository.findById(photoId)
                .filter(p -> p.getAnnonce().getId().equals(annonceId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Photo introuvable"));
        boolean etaitPrincipale = photo.isPrincipale();
        photoRepository.delete(photo);
        photoRepository.flush();
        List<Photo> restantes = photoRepository.findByAnnonceIdOrderByOrdreAsc(annonceId);
        Photo principaleExistante = restantes.stream().filter(Photo::isPrincipale).findFirst().orElse(null);
        if (etaitPrincipale || principaleExistante == null) {
            principaleExistante = restantes.isEmpty() ? null : restantes.get(0);
        }
        for (int i = 0; i < restantes.size(); i++) {
            Photo restante = restantes.get(i);
            restante.setOrdre(i + 1);
            restante.setPrincipale(principaleExistante != null
                    && restante.getId().equals(principaleExistante.getId()));
        }
        photoRepository.saveAll(restantes);
    }

    // ------------------------------------------------------------------
    // Favoris
    // ------------------------------------------------------------------

    @Transactional
    public void ajouterFavori(Long utilisateurId, Long annonceId) {
        Annonce annonce = annonceRepository.findById(annonceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Annonce introuvable"));
        if (annonce.getStatut() != StatutAnnonce.ACTIVE) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Annonce introuvable");
        }
        if (favoriRepository.findByUtilisateurIdAndAnnonceId(utilisateurId, annonceId).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Annonce deja dans les favoris");
        }
        Favori favori = new Favori();
        favori.setUtilisateurId(utilisateurId);
        favori.setAnnonceId(annonceId);
        favoriRepository.save(favori);
    }

    /** Vrai si l'annonce est en favori pour l'utilisateur, quel que soit son statut
     *  (une annonce LOUEE reste en favori meme si elle n'est plus dans les listes). */
    @Transactional(readOnly = true)
    public boolean estFavori(Long utilisateurId, Long annonceId) {
        return favoriRepository.findByUtilisateurIdAndAnnonceId(utilisateurId, annonceId).isPresent();
    }

    @Transactional
    public void retirerFavori(Long utilisateurId, Long annonceId) {
        Favori favori = favoriRepository.findByUtilisateurIdAndAnnonceId(utilisateurId, annonceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Favori introuvable"));
        favoriRepository.delete(favori);
    }

    @Transactional(readOnly = true)
    public PageResponse<FavoriResponse> mesFavoris(Long utilisateurId, int page, int taille) {
        // Les favoris restent consultables quand un bien passe a LOUEE,
        // mais une annonce masquee par la moderation n'est pas exposee.
        Page<Favori> favoris = favoriRepository.findByUtilisateurIdAndAnnonce_StatutInOrderByDateAjoutDesc(
                utilisateurId, List.of(StatutAnnonce.ACTIVE, StatutAnnonce.LOUEE),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(taille, 1), TAILLE_MAX_PAGE)));

        List<Favori> contenus = favoris.getContent();
        Map<Long, Annonce> annonces = annonceRepository.findAllById(
                        contenus.stream().map(Favori::getAnnonceId).toList()).stream()
                .collect(Collectors.toMap(Annonce::getId, Function.identity()));
        Map<Long, String> photos = photosPrincipalesParAnnonce(annonces.keySet());

        List<FavoriResponse> reponses = contenus.stream()
                .filter(f -> annonces.containsKey(f.getAnnonceId()))
                .map(f -> FavoriResponse.depuis(f, resume(annonces.get(f.getAnnonceId()),
                        photos.get(f.getAnnonceId()), null)))
                .toList();

        return new PageResponse<>(reponses, favoris.getNumber(), favoris.getSize(),
                favoris.getTotalElements(), favoris.getTotalPages());
    }

    // ------------------------------------------------------------------
    // Prive
    // ------------------------------------------------------------------

    private void appliquer(Annonce annonce, String titre, String description, BigDecimal prix,
                           Integer surface, Integer nbPieces, Double latitude, Double longitude) {
        annonce.setTitre(titre);
        annonce.setDescription(description);
        annonce.setPrix(prix);
        annonce.setSurface(surface);
        annonce.setNbPieces(nbPieces);
        annonce.setLatitude(latitude);
        annonce.setLongitude(longitude);
    }

    /** Annonce que le compte courant possede, ou 403/404 selon le cas. */
    private Annonce annonceDeProprietaire(String email, Long annonceId) {
        Proprietaire proprietaire = proprietaireService.parEmail(email);
        return annonceRepository.findByIdAndProprietaireId(annonceId, proprietaire.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                        "Annonce non attribuee a ce proprietaire"));
    }

    /**
     * Verrouille la ligne de l'annonce jusqu'a la fin de la transaction pour
     * serialiser toutes les mutations de ses photos entre instances applicatives.
     */
    private Annonce annonceDeProprietaireVerrouillee(String email, Long annonceId) {
        Proprietaire proprietaire = proprietaireService.parEmail(email);
        return annonceRepository.findLockedByIdAndProprietaireId(annonceId, proprietaire.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                        "Annonce non attribuee a ce proprietaire"));
    }

    private TypeLocation typeLocation(Long id) {
        return typeLocationRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Type de location introuvable"));
    }

    private void retirerAnciennePrincipale(Long annonceId, Long photoConserveeId) {
        photoRepository.findByAnnonceIdAndPrincipaleTrue(annonceId)
                .filter(p -> !p.getId().equals(photoConserveeId))
                .ifPresent(p -> {
                    p.setPrincipale(false);
                    photoRepository.save(p);
                });
    }

    private AnnonceResumeResponse resume(Annonce annonce, String urlPhoto, Double distanceKm) {
        return AnnonceResumeResponse.depuis(annonce, urlPhoto, distanceKm);
    }

    private Map<Long, String> photosPrincipalesParAnnonce(Collection<Long> annonceIds) {
        if (annonceIds.isEmpty()) {
            return Map.of();
        }
        List<Photo> photos = photoRepository.findByAnnonceIdIn(annonceIds).stream()
                .sorted(Comparator.comparingInt(Photo::getOrdre).thenComparing(Photo::getId))
                .toList();
        Map<Long, String> resultat = new java.util.HashMap<>();
        for (Photo photo : photos) {
            if (photo.isPrincipale()) {
                resultat.putIfAbsent(photo.getAnnonce().getId(), photo.getUrl());
            }
        }
        for (Photo photo : photos) {
            resultat.putIfAbsent(photo.getAnnonce().getId(), photo.getUrl());
        }
        return resultat;
    }

    private boolean estProprietaireOuAdmin(Annonce annonce) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return false;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails user) {
            // Admin
            boolean estAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (estAdmin) {
                return true;
            }
            // Proprietaire : comparer l'email du proprietaire avec celui de l'utilisateur
            String emailProp = annonce.getProprietaire() != null
                    && annonce.getProprietaire().getUtilisateur() != null
                    ? annonce.getProprietaire().getUtilisateur().getEmail() : null;
            return user.getUsername().equalsIgnoreCase(emailProp);
        }
        return false;
    }

    private boolean estConnecte() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
    }

    /** Distance en kilometres par la formule de Haversine avec repli quartier (CDC 12), ou null si inapplicable. */
    static Double distanceKm(Double lat, Double lng, Annonce annonce) {
        Double aLat = annonce.getLatitude() != null ? annonce.getLatitude()
                : (annonce.getQuartier() != null ? annonce.getQuartier().getLatitude() : null);
        Double aLng = annonce.getLongitude() != null ? annonce.getLongitude()
                : (annonce.getQuartier() != null ? annonce.getQuartier().getLongitude() : null);
        if (lat == null || lng == null || aLat == null || aLng == null) {
            return null;
        }
        double dLat = Math.toRadians(aLat - lat);
        double dLng = Math.toRadians(aLng - lng);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(aLat))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        // Protection numerique : a peut depasser legerement 1 a cause des arrondis flottants.
        a = Math.max(0.0, Math.min(1.0, a));
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(RAYON_TERRE_KM * c * 10.0) / 10.0;
    }
}
