package mg.emit.tadytrano.infrastructure.config;

import lombok.RequiredArgsConstructor;
import mg.emit.tadytrano.domain.model.Annonce;
import mg.emit.tadytrano.domain.model.Photo;
import mg.emit.tadytrano.domain.model.Proprietaire;
import mg.emit.tadytrano.domain.model.Quartier;
import mg.emit.tadytrano.domain.model.Role;
import mg.emit.tadytrano.domain.model.TypeLocation;
import mg.emit.tadytrano.domain.model.TypeProprietaire;
import mg.emit.tadytrano.domain.model.Utilisateur;
import mg.emit.tadytrano.domain.model.Ville;
import mg.emit.tadytrano.domain.repository.AnnonceRepository;
import mg.emit.tadytrano.domain.repository.PhotoRepository;
import mg.emit.tadytrano.domain.repository.ProprietaireRepository;
import mg.emit.tadytrano.domain.repository.QuartierRepository;
import mg.emit.tadytrano.domain.repository.TypeLocationRepository;
import mg.emit.tadytrano.domain.repository.UtilisateurRepository;
import mg.emit.tadytrano.domain.repository.VilleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Donnees de demo pour le demarrage LOCAL (profil "local") uniquement.
 * Necessaire car le referentiel (villes, quartiers) n'a pas d'API de
 * creation : sans ce seed, aucune annonce ne pourrait etre publiee.
 *
 * Comptes crees :
 *   admin@tady.test / admin12345        (ADMIN)
 *   proprio@tady.test / proprio12345    (PROPRIETAIRE, profil agence)
 *   locataire@tady.test / locataire12345 (LOCATAIRE)
 */
@Configuration
@Profile("local")
@RequiredArgsConstructor
public class DonneesDeDemo {

    /** Base des photos de demo (fm=jpg : format decode par JavaFX). */
    private static final String UNSPLASH = "https://images.unsplash.com/";
    private static final String PARAMS = "?auto=format&fit=crop&w=800&q=80&fm=jpg";

    private final VilleRepository villeRepository;
    private final QuartierRepository quartierRepository;
    private final TypeLocationRepository typeLocationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ProprietaireRepository proprietaireRepository;
    private final AnnonceRepository annonceRepository;
    private final PhotoRepository photoRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner chargerDonneesDeDemo() {
        return args -> {
            // idempotent : H2 memoriel est vide a chaque demarrage, mais on
            // se protege si le profil est un jour couple a une base persistante
            if (typeLocationRepository.count() > 0) {
                return;
            }

            TypeLocation hotel = type("HOTEL", "Hotel");
            TypeLocation etage = type("ETAGE", "Etage");
            type("PIECE", "Par piece");

            Ville tana = ville("Antananarivo");
            Ville tamatave = ville("Toamasina");
            Ville antsirabe = ville("Antsirabe");

            Quartier analakely = quartier("Analakely", tana, -18.9140, 47.5237);
            Quartier ivandry = quartier("Ivandry", tana, -18.8790, 47.5280);
            Quartier ankorondrano = quartier("Ankorondrano", tana, -18.8660, 47.5140);
            Quartier centreTamatave = quartier("Centre-ville", tamatave, -18.1550, 49.4050);
            Quartier centreAntsirabe = quartier("Centre-ville", antsirabe, -19.8660, 47.0330);

            Utilisateur proprio = compte("proprio@tady.test", "proprio12345", "Ranaivo", "Hery",
                    Role.PROPRIETAIRE);
            compte("admin@tady.test", "admin12345", "Admin", "Tady", Role.ADMIN);
            compte("locataire@tady.test", "locataire12345", "Rakoto", "Jean", Role.LOCATAIRE);

            Proprietaire agence = new Proprietaire();
            agence.setUtilisateur(proprio);
            agence.setType(TypeProprietaire.AGENCE);
            agence.setNomAgence("Agence Tiana Immobilier");
            agence.setTelephone("034 12 345 67");
            agence.setEmailContact("contact@tiana.mg");
            agence.setQuartier(analakely);
            Proprietaire proprietaire = proprietaireRepository.save(agence);

            annonce(proprietaire, hotel, analakely, "Chambre meublee Analakely",
                    "Chambre en hotel-particulier, eau chaude et wifi, proche du centre.",
                    new BigDecimal("250000"), 22, 1, -18.9145, 47.5240,
                    "photo-1522771739844-6a9f6d5f14af",
                    "photo-1505693416388-ac5ce068fe85");
            annonce(proprietaire, etage, ivandry, "Etage de villa Ivandry",
                    "Etage complet de villa avec garage et jardin, quartier calme et securise.",
                    new BigDecimal("1800000"), 240, 5, -18.8795, 47.5285,
                    "photo-1613490493576-7fde63acd811",
                    "photo-1600585154340-be6161a56a0c");
            annonce(proprietaire, etage, ankorondrano, "Appartement Ankorondrano",
                    "Appartement 3 pieces au coeur des affaires, proche des banques.",
                    new BigDecimal("950000"), 85, 3, -18.8665, 47.5145,
                    "photo-1502672260266-1c1ef2d93688",
                    "photo-1560448204-e02f11c3d0e2",
                    "photo-1484154218962-a197022b5858");
            annonce(proprietaire, hotel, centreTamatave, "Studio pres du port",
                    "Studio simple a deux pas du port de Toamasina.",
                    new BigDecimal("350000"), 30, 1, -18.1555, 49.4055,
                    "photo-1598928506311-c55ded91a20c");
            annonce(proprietaire, etage, centreAntsirabe, "Duplex Centre-ville Antsirabe",
                    "Duplex lumineux, marche et gare a 5 minutes a pied.",
                    new BigDecimal("700000"), 95, 3, -19.8665, 47.0335,
                    "photo-1545324418-cc1a3fa10c00",
                    "photo-1512917774080-9991f1c4c750");
        };
    }

    private TypeLocation type(String code, String libelle) {
        TypeLocation type = new TypeLocation();
        type.setCode(code);
        type.setLibelle(libelle);
        return typeLocationRepository.save(type);
    }

    private Ville ville(String nom) {
        Ville ville = new Ville();
        ville.setNom(nom);
        return villeRepository.save(ville);
    }

    private Quartier quartier(String nom, Ville ville, Double lat, Double lng) {
        Quartier quartier = new Quartier();
        quartier.setNom(nom);
        quartier.setVille(ville);
        quartier.setLatitude(lat);
        quartier.setLongitude(lng);
        return quartierRepository.save(quartier);
    }

    private Utilisateur compte(String email, String motDePasse, String nom, String prenom, Role role) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(email);
        utilisateur.setMotDePasse(passwordEncoder.encode(motDePasse));
        utilisateur.setNom(nom);
        utilisateur.setPrenom(prenom);
        utilisateur.setRole(role);
        utilisateur.setActif(true);
        return utilisateurRepository.save(utilisateur);
    }

    private void annonce(Proprietaire proprietaire, TypeLocation type, Quartier quartier,
                         String titre, String description, BigDecimal prix,
                         int surface, int pieces, Double lat, Double lng,
                         String... photosUnsplash) {
        Annonce annonce = new Annonce();
        annonce.setProprietaire(proprietaire);
        annonce.setTypeLocation(type);
        annonce.setQuartier(quartier);
        annonce.setTitre(titre);
        annonce.setDescription(description);
        annonce.setPrix(prix);
        annonce.setSurface(surface);
        annonce.setNbPieces(pieces);
        annonce.setLatitude(lat);
        annonce.setLongitude(lng);
        annonceRepository.save(annonce);

        List<Photo> photos = new ArrayList<>();
        for (int i = 0; i < photosUnsplash.length; i++) {
            Photo photo = new Photo();
            photo.setAnnonce(annonce);
            photo.setUrl(UNSPLASH + photosUnsplash[i] + PARAMS);
            photo.setPrincipale(i == 0);
            photo.setOrdre(i + 1);
            photos.add(photo);
        }
        photoRepository.saveAll(photos);
    }
}
