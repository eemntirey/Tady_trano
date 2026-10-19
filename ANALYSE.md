# Tady Trano : analyse du projet

**Auteur : Jhonny (v1.0) · Mise à jour v1.2 · 19 octobre 2026**

État documentaire actualisé après la PR #1 (fusionnée dans `main`) et la préparation de la
PR sécurité : configuration commune sans secrets/identifiants de secours, garde de démarrage qui refuse les
variables absentes/vides et les secrets JWT de moins de 32 octets, migration V4 de désactivation
des comptes de démonstration persistants, et vérification CI des démarrages incomplets. Les résultats définitifs de la CI de la PR sécurité
restent à consulter avant fusion.

## 1. Vue d'ensemble

| | |
|---|---|
| **API** | Module `tady-trano-api` : Spring Boot, JWT, JPA/Flyway, contrôleurs REST |
| **Client** | Module `tady-trano-client` : JavaFX desktop, client HTTP/JSON, vues de recherche et de gestion |
| **Tests** | **42 au total : 34 API** (20 annonces/recherche + 11 authentification + 3 configuration sécurité) + 8 client (lecture JSON) |

- Les fonctionnalités F1 à F10 du cahier des charges sont toutes présentes.
- Le client utilise le nouveau CSS : plus aucun `setStyle` résiduel (les 2 relevés en v1.0
  étaient dans le code mort supprimé).

## 2. Points forts (inchangés)

- Architecture de l'API propre : couches domain, application, presentation et infrastructure,
  avec Flyway, JWT sans session et DTO.
- Sécurité solide pour du local : accès admin protégé deux fois, contrôle du propriétaire,
  contact masqué hors connexion, annonces masquées en 404, mots de passe hachés. Les tests de
  droits couvrent 403, 401 et 404.
- Recherche bien pensée : Haversine en SQL (avec protection de `acos`), annonces sans
  coordonnées en dernier, pagination plafonnée, index et `ON DELETE CASCADE`.
- Client réactif : appels réseau hors du thread JavaFX, délais de 5 s / 15 s.

## 3. Points à améliorer — état après patch

### 3.1 Code mort dans le client — ✅ fait (commit `daf4e9c`)
6 fichiers supprimés (−436 lignes) : `AppContext`, `NavigationManager`,
`AnnonceCardController`, `AnnonceCard.fxml`, `recherche.fxml` **et** `RechercheController`
(le `fx:controller` de `recherche.fxml`, lui aussi jamais chargé — aucune occurrence de
`FXMLLoader` dans le projet). Dossiers vides nettoyés, plus aucune référence résiduelle.

### 3.2 Images : mémoire — ✅ fait (commit `daf4e9c`)
`Affichage` : décodage à la taille demandée (`new Image(url, largeur, hauteur, …)`) et cache
LRU borné à 100 entrées (`LinkedHashMap` en ordre d'accès, synchronisé). Clé = url +
dimensions (les appelants vont de 70×50 à 640×380).

### 3.3 Réponses reçues dans le désordre — ✅ fait (commits `daf4e9c`, `fd9ec03`)
Compteur de génération par vue, appliqué aux callbacks succès **et** erreur, avec capture de
la page au moment de la soumission. Présent dans les 4 écrans à pagination :
`VueRecherche`, `VueAdmin`, `VueFavoris`, `VueMesAnnonces`. `Travailleur` reste à thread
unique sans annulation (non requis).

### 3.4 Requêtes N+1 — ✅ fait (commit `daf4e9c`)
`AnnonceRepository.rechercher` : `JOIN FETCH` sur `quartier`, `ville`, `typeLocation` +
`countQuery` explicite sans fetch. `@EntityGraph` ajouté sur les autres listes
(`findByProprietaire…`, `findByStatut…`, `findAllBy…`, `findAllById`).

### 3.5 Performance de la recherche (pré-filtre zone rectangulaire) — ⬜ en attente
Le motif `(:x IS NULL OR …)` et le tri calculé restent un scan des annonces actives ;
suffisant jusqu'à quelques milliers d'annonces (mesuré en 3.6).

### 3.6 Tests — ✅ fait (voir « Mesures » plus bas)
- **Cas limites** : `AnnonceRechercheCasLimitesTest` (5 tests) — prix min > max → liste vide
  200, latitude sans longitude et longitude sans latitude → repli explicite sur le tri par
  date (durci dans la requête : guard `:lng IS NULL`), valeurs négatives de prix/pièces sans
  erreur, coordonnées négatives (hémisphère sud) triées par distance.
- **Jeu de 1 000 annonces** : `AnnonceRechercheFluiditeTest` — insertion par lots SQL dans
  une H2 dédiée, mesure des trois requêtes représentatives avec seuil large de 2 000 ms.
- **Refactor** : helpers partagés extraits dans `AnnonceTestSupport` (plus de duplication
  entre classes de test).
- Il reste à ajouter : tests directs des services (jamais écrits).

### 3.7 Lisibilité — ⬜ en attente
`VueMesAnnonces` fait toujours environ 504 lignes (`wc -l`) : à découper en plusieurs classes,
dans une PR dédiée sans changement de comportement.

## 4. Corrigé en dehors des points 3.x

- **Dialecte Hibernate** (commit `fd9ec03`) : `spring.jpa.properties.hibernate.dialect:
  MySQLDialect` supprimé de `application.yml` — il s'appliquait aussi à H2 et produisait le
  bruit « alter table … drop foreign key » dans les logs de test. Hibernate détecte le
  dialecte depuis la connexion. Les logs de test affichent désormais `H2Dialect`, sans erreur
  DDL. ⚠️ **À vérifier au premier démarrage local** : chercher `Database dialect: MySQLDialect`
  (ou `MariaDBDialect`) ; si Flyway puis `ddl-auto: validate` passent, tout est bon.

## 5. Mesures (nouveau)

| Mesure (1 000 annonces, H2 mémoire) | Résultat | Seuil |
|---|---|---|
| Première page (`page=0&taille=12`) | **144 ms** | 2 000 ms |
| Tri par distance (Haversine complet) | **136 ms** | 2 000 ms |
| Filtre tranche de prix | **53 ms** | 2 000 ms |

**42 tests attendus après le correctif sécurité** : 34 API + 8 client. Le statut réel de la CI de cette branche est la référence avant fusion.

## 6. Sécurité et risques résiduels

### 6.1 Démarrage et secrets

- La configuration commune (`application.yml`) n'a plus de défaut pour `DB_URL`, `DB_USER`,
  `DB_PASSWORD` ni `JWT_SECRET`. `ConfigurationDemarrageSecurisee` refuse hors des profils
  `local` et `test` les variables absentes/vides, une URL non JDBC et un secret JWT de moins de
  32 octets UTF-8. Le profil `local` garde ses valeurs de développement avec H2 volatile.
- Les détails d'erreur serveur sont masqués par défaut (`server.error.include-message: never`) ;
  `local` et `test` peuvent les afficher pour le diagnostic.
- Les comptes de démonstration V2 sont désactivés par la nouvelle migration V4 sur les bases
  persistantes. V2 n'est pas modifiée afin de préserver le checksum Flyway des installations
  existantes. La désactivation ne supprime aucune donnée.

### 6.2 Points toujours ouverts avant production

- V2 insère aussi des profils, annonces et photos fictifs : V4 désactive les comptes connus,
  mais ne purge pas ces données. Les examiner et les masquer/nettoyer avant une mise en ligne.
- Limitation des tentatives de connexion (rate-limit / temporisation anti-abus).
- Photos ajoutées par URL : pas encore de téléversement maîtrisé avec contrôle du type, taille
  et contenu.
- HTTPS, gestion des secrets et droits minimaux du compte MySQL restent à configurer dans
  l'environnement de déploiement.
- Les tests directs des services métier ne sont pas encore présents.
- Le pré-filtre géographique (3.5) et le découpage de `VueMesAnnonces` (3.7) restent reportés.

## 7. Prochaine étape

1. Après fusion de la PR sécurité, confirmer au premier démarrage MySQL : dialecte détecté,
   migrations Flyway V1–V4, `ddl-auto=validate`, et variables obligatoires présentes.
2. Ajouter des tests directs des services pour modération, verrou de statut et favoris.
3. Découper `VueMesAnnonces` dans une PR distincte.
4. Envisager l'optimisation géographique uniquement après accord sur le contrat de recherche.
