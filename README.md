# Tady Trano

Application de recherche de maisons à louer : une **API REST** (Spring Boot) et un **client desktop** (JavaFX).

> **Projet local.** Il n'est pas prévu pour une mise en production à ce stade (voir [Limites actuelles](#limites-actuelles)).

## Fonctionnalités

- **Locataires** (foyer, étudiant, étranger) : recherche d'annonces avec filtres (ville, quartier, type, prix, nombre de pièces), tri par distance, liste paginée (15 par page), carte, favoris, contact direct du propriétaire ou de l'agence (réservé aux utilisateurs connectés).
- **Propriétaires et agences** : publication, modification, changement de statut et suppression de leurs annonces, gestion des photos.
- **Administrateur** : modération des annonces (statuts `ACTIVE`, `MASQUEE`, `LOUEE`).
- **Types de location** : hôtel, étage, par pièce.

## Architecture

```
Client JavaFX  --HTTP/JSON-->  API Spring Boot  --JDBC-->  MySQL / MariaDB
 (tady-trano-client)            (tady-trano-api)            (ou H2 en mémoire)
```

Projet Maven multi-module :

| Module | Rôle | Technologies |
|---|---|---|
| `tady-trano-api` | API REST, sécurité, accès aux données | Spring Boot 4.1.1, Spring Security + JWT (jjwt 0.12.6), JPA, Flyway, SpringDoc |
| `tady-trano-client` | Application desktop | JavaFX 27, AtlantaFX, `HttpClient` + Jackson, Leaflet (carte dans un `WebView`) |

## Prérequis

- **JDK** : l'API vise Java 21 (`java.version` dans le `pom.xml` racine). Le client utilise JavaFX 27 ; le projet est développé avec un JDK 27 (voir `javafx.version` dans `tady-trano-client/pom.xml`).
- **Maven** 3.9 ou plus récent.
- **MySQL ou MariaDB** : uniquement pour le mode avec base persistante (le mode rapide utilise H2 en mémoire).
- **Connexion internet** pour la carte (Leaflet et les tuiles OpenStreetMap sont chargés en ligne).

## Démarrage rapide (sans base de données)

Le profil `local` utilise une base H2 en mémoire et injecte des données de démonstration. Elle est vide à chaque démarrage.

**1. Lancer l'API** (depuis la racine du projet) :

```bash
mvn -pl tady-trano-api spring-boot:run -Dspring-boot.run.profiles=local
```

L'API écoute sur `http://localhost:8081`.

**2. Lancer le client** (dans un second terminal) :

```bash
mvn -pl tady-trano-client javafx:run
```

## Démarrage avec MySQL ou MariaDB

Le schéma est créé par Flyway (`V1__init.sql` à `V4__desactivation_comptes_demo.sql`). V2 conserve les données de démonstration historiques pour ne pas modifier une migration déjà appliquée ; V4 désactive les six comptes de démonstration connus sur les bases persistantes.

**1. Définir toutes les variables obligatoires hors du profil `local`.** L'API refuse de démarrer si une configuration de connexion ou de signature est absente. Aucun identifiant `root`, mot de passe de base ou secret JWT de secours n'est défini dans la configuration commune.

| Variable | Rôle | Défaut |
|---|---|---|
| `DB_URL` | URL JDBC complète | aucun (obligatoire) |
| `DB_USER` | Utilisateur dédié à l'application | aucun (obligatoire) |
| `DB_PASSWORD` | Mot de passe de la base | aucun (obligatoire) |
| `JWT_SECRET` | Secret de signature JWT à forte entropie (au moins 32 octets aléatoires) | aucun (obligatoire) |
| `JWT_EXPIRATION_MS` | Durée de vie des jetons | `86400000` (24 h) |
| `SERVER_PORT` | Port de l'API | `8081` |

Pour Windows (PowerShell), Linux ou macOS, définissez `DB_URL`, `DB_USER`, `DB_PASSWORD` et `JWT_SECRET` dans l'environnement avant le démarrage. Utilisez un utilisateur MySQL dédié, avec uniquement les privilèges requis, et un secret aléatoire propre à l'environnement ; ne réutilisez pas la valeur d'exemple des tests.

Exemple PowerShell (remplacez les valeurs entre chevrons par vos propres paramètres) :

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/tady_trano?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Indian/Antananarivo&characterEncoding=utf8"
$env:DB_USER = "<utilisateur_mysql_dedie>"
$env:DB_PASSWORD = "<mot_de_passe_mysql>"
$env:JWT_SECRET = "<secret_aleatoire_fort>"
mvn -pl tady-trano-api spring-boot:run
```

**2. Lancer le client** comme dans le démarrage rapide.

## Configurer l'adresse de l'API côté client

Par défaut, le client appelle `http://localhost:8081`. Pour changer l'adresse :

```bash
# variable d'environnement
export TADY_API_URL="http://localhost:9000"

# ou propriété Java (prioritaire)
mvn -pl tady-trano-client javafx:run -Dtady.api.url=http://localhost:9000
```

## Comptes de démonstration

**Profil `local` (H2)**

| Rôle | Email | Mot de passe |
|---|---|---|
| Administrateur | `admin@tady.test` | `admin12345` |
| Propriétaire (agence) | `proprio@tady.test` | `proprio12345` |
| Locataire | `locataire@tady.test` | `locataire12345` |

**Base MySQL ou MariaDB (migrations V2 puis V4)**

V2 a historiquement inséré des comptes avec un mot de passe partagé `Pass1234!`. La migration V4 désactive ces six comptes connus, sans supprimer leurs lignes. Ne les réactivez pas pour un déploiement public. Les profils, annonces et photos de démonstration issus de V2 restent néanmoins présents dans la base persistante et doivent être examinés, masqués ou nettoyés avant la mise en production.

## Documentation de l'API

Swagger UI : <http://localhost:8081/swagger-ui.html>

| Endpoint | Accès | Description |
|---|---|---|
| `/api/auth/inscription`, `/api/auth/connexion` | Public | Création de compte et connexion (jeton JWT) |
| `GET /api/annonces`, `/api/annonces/{id}` | Public | Recherche avec filtres, tri par distance (`lat`, `lng`), pagination ; détail (annonces `ACTIVE` seulement) |
| `POST`, `PUT`, `PATCH`, `DELETE /api/annonces/...` | Propriétaire | Gestion de ses annonces et de leurs photos |
| `/api/favoris` | Connecté | Ajout, retrait et liste des favoris |
| `/api/proprietaires` | Propriétaire | Profil du propriétaire ou de l'agence |
| `/api/admin/annonces` | Administrateur | Liste de toutes les annonces et modération |
| `/api/villes`, `/api/quartiers`, `/api/types-location` | Public | Référentiels |

Les routes protégées attendent l'en-tête `Authorization: Bearer <jeton>`.

## Tests

```bash
mvn test
```

Les tests de l'API utilisent H2 (`application-test.yml`) et couvrent notamment l'inscription et la connexion, la recherche et le tri par distance, et les droits d'accès : autre propriétaire (403), locataire sur `/api/admin` (403), requête sans jeton (401), annonce masquée (404).

## Structure du projet

```
tady-trano/
├── pom.xml                       Parent Maven (Spring Boot 4.1.1, Java 21)
├── tady-trano-api/
│   └── src/main/java/mg/emit/tadytrano/
│       ├── domain/               model, repository
│       ├── application/          service, dto, exception
│       ├── presentation/rest/    contrôleurs REST
│       └── infrastructure/       config, security (JWT)
│   └── src/main/resources/
│       ├── application.yml       configuration (MySQL / MariaDB)
│       ├── application-local.yml profil H2 + données de démo
│       └── db/migration/         scripts Flyway V1 à V4
└── tady-trano-client/
    └── src/main/java/mg/emit/tadytrano/client/
        ├── api/                  client HTTP, session
        ├── vue/                  écrans (recherche, carte, détail, favoris, ...)
        ├── ui/                   navigation, thème, carte
        └── dto/                  objets échangés avec l'API
```

## Dépannage

| Problème | Cause probable et solution |
|---|---|
| L'API ne démarre pas, erreur sur `DB_PASSWORD` | Définir la variable `DB_PASSWORD`, ou utiliser le profil `local` |
| Le client ne se connecte pas | Vérifier que l'API tourne sur le port 8081, ou régler `TADY_API_URL` |
| Port 8081 déjà utilisé | Définir `SERVER_PORT` (et `TADY_API_URL` côté client) |
| La carte reste vide | Vérifier la connexion internet (Leaflet et OpenStreetMap sont chargés en ligne) |
| Erreur de version JavaFX ou du JDK | Utiliser un JDK compatible avec `javafx.version` du `pom.xml` du client |

## Limites actuelles

Projet encore à valider avant toute mise en production :

- activer HTTPS (API et base de données) et stocker les secrets dans un gestionnaire de secrets ;
- examiner et nettoyer/masquer les profils, annonces et photos de démonstration issus de V2 (V4 ne fait que désactiver les six comptes de connexion connus) ;
- limiter les tentatives de connexion et ajouter une protection contre les abus d'authentification ;
- remplacer l'ajout de photos par URL par un téléversement avec contrôle du type, de la taille et du contenu ;
- surveiller les performances de recherche et ne définir un éventuel rayon géographique qu'après validation du contrat produit.
