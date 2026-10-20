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

Le schéma est créé par Flyway (`V1__init.sql`), puis les données initiales sont insérées (`V2__donnees_initiales.sql`).

**1. Définir les variables d'environnement.** Hors profils `local` et `test`, les quatre variables `DB_URL`, `DB_USER`, `DB_PASSWORD` et `JWT_SECRET` sont obligatoires. L'API refuse une URL JDBC absente/invalide, une connexion TLS explicitement désactivée ou une configuration incomplète ; elle ne retombe pas sur localhost ni sur l'utilisateur `root`.

| Variable | Rôle | Défaut |
|---|---|---|
| `DB_URL` | URL JDBC MySQL/MariaDB avec TLS activé | **obligatoire, aucun fallback** |
| `DB_USER` | Compte SQL dédié à l'application | **obligatoire, aucun fallback** |
| `DB_PASSWORD` | Mot de passe du compte SQL | **obligatoire, aucun fallback** |
| `JWT_SECRET` | Secret de signature JWT d'au moins 32 caractères | **obligatoire, aucun fallback** |
| `JWT_EXPIRATION_MS` | Durée de vie des jetons | `86400000` (24 h) |
| `SERVER_PORT` | Port de l'API | `8081` |

Exemple sous Windows (PowerShell) :

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/tady_trano?sslMode=REQUIRED&serverTimezone=Indian/Antananarivo&characterEncoding=utf8"
$env:DB_USER = "tady_app"
$env:DB_PASSWORD = "remplacer_par_un_mot_de_passe_long"
$env:JWT_SECRET = "remplacer_par_un_secret_aleatoire_d_au_moins_32_caracteres"
mvn -pl tady-trano-api spring-boot:run
```

Exemple sous Linux ou macOS :

```bash
export DB_URL="jdbc:mysql://localhost:3306/tady_trano?sslMode=REQUIRED&serverTimezone=Indian/Antananarivo&characterEncoding=utf8"
export DB_USER="tady_app"
export DB_PASSWORD="remplacer_par_un_mot_de_passe_long"
export JWT_SECRET="remplacer_par_un_secret_aleatoire_d_au_moins_32_caracteres"
mvn -pl tady-trano-api spring-boot:run
```

Le profil `local` continue d'utiliser sa base H2 en mémoire ; les variables ci-dessus concernent le démarrage persistant hors profils `local`/`test`.

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

**Base MySQL ou MariaDB (migration V2)**

Tous les comptes de la migration V2 ont le mot de passe `Pass1234!`. Par exemple `admin@tady.mg` (administrateur), `rakoto.proprio@gmail.com` (propriétaire) et `famille.rasoa@yahoo.fr` (locataire).

## Documentation de l'API

Swagger UI : <http://localhost:8081/swagger-ui.html>

| Endpoint | Accès | Description |
|---|---|---|
| `/api/auth/inscription`, `/api/auth/connexion`, `GET /api/auth/me` | Public | Création de compte, connexion (jeton JWT) et profil à jour de l'utilisateur connecté |
| `GET /api/annonces`, `/api/annonces/{id}` | Public | Recherche avec filtres, tri par distance (`lat`, `lng`), pagination ; détail (annonces `ACTIVE` seulement) |
| `POST`, `PUT`, `PATCH`, `DELETE /api/annonces/...` | Propriétaire | Gestion de ses annonces et de leurs photos |
| `/api/favoris`, `GET /api/favoris/{annonceId}` | Connecté | Ajout, retrait, liste des favoris et test de présence d'une annonce en favori |
| `/api/proprietaires` | Propriétaire | Profil du propriétaire ou de l'agence |
| `/api/admin/annonces` | Administrateur | Liste de toutes les annonces et modération |
| `/api/villes`, `/api/villes/{villeId}/quartiers`, `/api/types-location` | Public | Référentiels |

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
│       └── db/migration/         scripts Flyway V1, V2
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

Projet destiné à un usage local. Avant toute mise en production, il faudrait :

- activer HTTPS (API et base de données) et définir un `JWT_SECRET` propre (aucune valeur par défaut n'est fournie dans le dépôt) ;
- les comptes de démonstration de la migration V2 sont désactivés par la migration V4 (corrective) ;
- la limitation des tentatives de connexion est active (blocage temporaire après 5 échecs) ;
- remplacer l'ajout de photos par URL par un téléversement avec contrôle du type et de la taille ;
- ajouter un pré-filtre géographique pour le tri par distance si le nombre d'annonces devient important.
