# Tady Trano : analyse du projet

**Auteur : Jhonny (v1.0) · Mise à jour v1.1 · 19 octobre 2026**

Analyse d'origine réalisée sur le dépôt à jour (7 commits, patch CSS inclus), sans exécution.
**v1.1** : mise à jour après les patches 3.1 → 3.4, la correction du dialecte Hibernate et
l'extension des gardes de réponses périmées ; ajout des tests du point 3.6.

## 1. Vue d'ensemble

| | |
|---|---|
| **API** | 59 fichiers Java, ~2 198 lignes (`src/main/java`, recomptées — méthode différente de la v1.0) |
| **Client** | 35 fichiers Java, ~2 325 lignes (4 fichiers supprimés par le patch 3.1) |
| **Tests** | **34 au total : 28 API** (19 annonces + 9 authentification) + 6 client (lecture JSON) |

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
`VueMesAnnonces` fait toujours 404 lignes : à découper en plusieurs classes.

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

**34 tests verts** (`mvn test`, BUILD SUCCESS) : 28 API + 6 client.

## 6. Reporté (usage local) — inchangé

- secret JWT par défaut dans `application.yml` ;
- comptes de démo de la migration V2 ;
- photos ajoutées par URL ;
- absence de limitation des tentatives de connexion.

## 7. Prochaine étape

1. Vérifier le démarrage local MySQL (ligne `Database dialect:` + Flyway + `validate`).
2. Point 3.5 (pré-filtre zone rectangulaire) si le volume dépasse quelques milliers d'annonces.
3. Point 3.7 (découpe de `VueMesAnnonces`) et tests directs des services.
