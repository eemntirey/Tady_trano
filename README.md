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

- **JDK de l'API** : Java 21 est la version cible (`java.version` dans le `pom.xml` racine).
- **JDK minimal du client** : Java 25 ou ultérieur, car JavaFX 27 exige JDK 25 minimum.
- **JDK testé en CI** : Eclipse Temurin 25, sur le runner Ubuntu de GitHub Actions. Le projet indique JDK 27 comme environnement de développement local ; vérifie la version active sur ta machine avec `java -version`.
- **JavaFX** : version 27 (`javafx.version` dans `tady-trano-client/pom.xml`). La carte utilise `WebView` et le pont Java/JavaScript `jdk.jsobject` ; toute mise à jour JavaFX/JDK doit être revalidée sur cette partie.
- **Systèmes vérifiés** : Ubuntu Linux pour la compilation et les tests automatisés CI ; Windows 11 est l'environnement de développement déclaré, mais le workflow CI ne valide pas le rendu graphique de JavaFX/WebView sous Windows. macOS n'est pas validé par le workflow actuel.
- **Maven** 3.9 ou plus récent.
- **MySQL ou MariaDB** : uniquement pour le mode avec base persistante (le mode rapide utilise H2 en mémoire).
- **Connexion internet** pour la carte (Leaflet et les tuiles OpenStreetMap sont chargés en ligne).

Pour vérifier le JDK actif et les dépendances JavaFX résolues (JavaFX Web, Controls, etc.) :

```bash
java -version && mvn -pl tady-trano-client -q dependency:tree -Dincludes=org.openjfx
```

Dans Windows PowerShell 5.1, exécute ces deux commandes séparément si l'opérateur `&&` n'est pas pris en charge.

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

**1. Définir les variables d'environnement.** `DB_PASSWORD` est obligatoire : l'API refuse de démarrer sans elle.

| Variable | Rôle | Défaut |
|---|---|---|
| `DB_PASSWORD` | Mot de passe de la base | **obligatoire, aucun fallback** |
| `DB_USER` | Utilisateur de la base | `root` |
| `DB_URL` | URL JDBC | `jdbc:mysql://localhost:3306/tady_trano?createDatabaseIfNotExist=true&useSSL=true&...` |
| `JWT_SECRET` | Secret de signature des jetons | **obligatoire, aucun fallback** |
| `JWT_EXPIRATION_MS` | Durée de vie des jetons | `86400000` (24 h) |
| `SERVER_PORT` | Port de l'API | `8081` |

Exemple sous Windows (PowerShell) :

```powershell
$env:DB_PASSWORD = "votre_mot_de_passe"
mvn -pl tady-trano-api spring-boot:run
```

Exemple sous Linux ou macOS :

```bash
export DB_PASSWORD="votre_mot_de_passe"
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
- ajouter et tester une limitation des tentatives de connexion avant toute exposition à des utilisateurs non fiables ;
- remplacer l'ajout de photos par URL par un téléversement avec contrôle du type et de la taille ;
- ajouter un pré-filtre géographique pour le tri par distance si le nombre d'annonces devient important.
