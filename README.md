# /alll - Esports Watch Party & Betting Platform

[![Java CI with Gradle](https://github.com/Alim-Samira/DevOps/actions/workflows/gradle.yml/badge.svg)](https://github.com/Alim-Samira/DevOps/actions/workflows/gradle.yml)
[![Coverage Badge](https://raw.githubusercontent.com/Alim-Samira/DevOps/main/.github/badges/jacoco.svg)](https://github.com/Alim-Samira/DevOps/actions/workflows/coverage-badge.yml)
[![Branches Badge](https://raw.githubusercontent.com/Alim-Samira/DevOps/main/.github/badges/branches.svg)](https://github.com/Alim-Samira/DevOps/actions/workflows/coverage-badge.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Alim-Samira_DevOps&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Alim-Samira_DevOps)
[![Version](https://img.shields.io/github/v/release/Alim-Samira/DevOps?label=release)](https://github.com/Alim-Samira/DevOps/releases)

API REST Spring Boot pour organiser des watch parties esport, gérer des paris, des classements, des récompenses, des intégrations calendrier, un suivi League of Legends professionnel et une interface de test web.

## Vue d'ensemble

Le projet couvre aujourd'hui l'ensemble du cycle d'une watch party :

- création manuelle de watch parties publiques et privées
- création automatique de watch parties basées sur une équipe ou un tournoi
- gestion des participants, du chat et des notifications
- système de paris avancé avec plusieurs types de bets
- tickets spéciaux et récompenses
- classements globaux et par watch party
- intégration calendrier
- lien avec Leaguepedia et l'API LoL Esports Live Stats
- persistance en base PostgreSQL
- front de test servi directement par Spring Boot

## Fonctionnalités

### Watch parties

- création de watch parties publiques et privées
- création automatique de watch parties à partir d'une équipe ou d'un tournoi
- ouverture et mise à jour automatiques via scheduler
- suivi des états de match et du statut des watch parties
- gestion des participants et du créateur

### Paris

Le backend supporte trois types de paris :

- `DiscreteChoiceBet`
- `NumericValueBet`
- `OrderedRankingBet`

Fonctionnalités associées :

- phase de vote, fermeture, résolution et annulation
- résolution manuelle
- auto-résolution quand les données live le permettent
- gestion des gagnants, du pot et des tickets

### Tickets

- tickets liés aux paris gagnants
- tickets spécialisés selon le type de pari
- ticket `IN_OR_OUT`
- modification d'un pari déjà clos côté vote quand le métier l'autorise

### Classements et récompenses

- classements publics en points
- classements publics en victoires
- classements par watch party
- récompenses par paliers
- récompenses mensuelles

### Chat et mini-jeu

- chat par watch party
- quiz intégré
- endpoints REST dédiés pour le quiz et son état

### Notifications

- notifications utilisateur
- notifications liées aux watch parties en présentiel selon la disponibilité calendrier

## Intégrations externes

### Leaguepedia

`LeaguepediaClient` interroge Leaguepedia pour récupérer les matchs à venir et alimenter les watch parties automatiques.

Cas d'usage :

- suivre une équipe
- suivre un tournoi
- récupérer le prochain match
- alimenter le scheduler automatique

### LoL Esports Live Stats

`LolEsportsClient` et `LiveMatchMonitorService` permettent de relier un match détecté à la source live Riot.

Le système peut :

- retrouver un identifiant d'événement live
- récupérer le `gameId`
- poller régulièrement les frames
- exploiter les événements et statistiques live
- auto-résoudre certains paris à partir du flux de partie

### Calendrier

La fonctionnalité calendrier permet :

- connexion d'un calendrier `ICAL` par URL publique
- connexion d'un calendrier `GOOGLE` via token OAuth et `calendarId`
- lecture des événements sur une plage temporelle
- vérification de disponibilité
- ajout d'une watch party à Google Calendar
- notifications de disponibilité pour des watch parties en présentiel

## Persistance

Le projet utilise PostgreSQL avec Spring Data JPA pour persister :

- les watch parties
- les utilisateurs
- les données liées au cycle de vie métier

Cela permet de conserver l'état de l'application entre deux redémarrages.

## Front de test

Une interface web légère en `vanilla JS` est servie directement par Spring Boot.

- URL : `http://localhost:8080/`
- fichiers : [index.html](/c:/Users/User/Documents/Code/Java/DevOps/src/main/resources/static/index.html), [app.js](/c:/Users/User/Documents/Code/Java/DevOps/src/main/resources/static/app.js), [styles.css](/c:/Users/User/Documents/Code/Java/DevOps/src/main/resources/static/styles.css)

Ce front permet notamment de :

- créer des watch parties
- rejoindre ou quitter une watch party
- créer, voter et résoudre des paris
- consulter le chat
- consulter les rankings
- connecter des calendriers
- voir les notifications

Il sert avant tout d'interface de démonstration et de validation backend.

## Stack technique

- Java 17
- Spring Boot
- Spring Web
- Spring Data JPA
- PostgreSQL
- Gson
- Springdoc OpenAPI / Swagger UI
- JaCoCo
- GitHub Actions
- SonarCloud

## Structure du projet

```text
.
├── .github/workflows/
├── docs/
├── src/
│   ├── main/
│   │   ├── java/backend/
│   │   │   ├── controllers/
│   │   │   ├── integration/lolesports/
│   │   │   ├── models/
│   │   │   ├── repositories/
│   │   │   └── services/
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/
│   └── test/java/backend/
├── build.gradle.kts
└── README.md
```

## Démarrage rapide

### Pré-requis

- JDK 17+
- une base PostgreSQL accessible

### Configuration

Le fichier principal est [application.properties](/c:/Users/User/Documents/Code/Java/DevOps/src/main/resources/application.properties).

Exemple de paramètres utiles :

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/devops
spring.datasource.username=postgres
spring.datasource.password=postgres

lolesports.auth-token=
lolesports.gw-base-url=https://esports-api.lolesports.com/persisted/gw
lolesports.live-base-url=https://feed.lolesports.com/livestats/v1
```

Pour l'API LoL Esports, il est recommandé de passer le token via variable d'environnement :

```bash
LOLESPORTS_AUTH_TOKEN=...
```

### Lancement

```bash
./gradlew bootRun
```

L'application est alors disponible sur :

- API : `http://localhost:8080`
- Swagger UI : `http://localhost:8080/swagger-ui.html`
- front de test : `http://localhost:8080/`

## Tests et couverture

### Commandes

```bash
./gradlew test
./gradlew test jacocoTestReport jacocoTestCoverageVerification
```

Rapports générés :

- tests : `build/reports/tests/test/index.html`
- couverture : `build/reports/jacoco/test/html/index.html`


## Endpoints utiles

### Watch parties

```http
POST /api/watchparties
POST /api/watchparties/public
POST /api/watchparties/private
POST /api/watchparties/{name}/join
POST /api/watchparties/{name}/leave
GET  /api/watchparties
GET  /api/watchparties/{name}/chat
POST /api/watchparties/{name}/chat
POST /api/watchparties/{name}/calendar
```

### Paris

```http
POST /api/watchparties/{name}/bets/discrete
POST /api/watchparties/{name}/bets/numeric
POST /api/watchparties/{name}/bets/ranking
POST /api/watchparties/{name}/bets/vote
POST /api/watchparties/{name}/bets/end-voting
POST /api/watchparties/{name}/bets/resolve
POST /api/watchparties/{name}/bets/use-ticket
```

### Calendrier

```http
GET    /api/calendars/providers
POST   /api/users/{user}/calendars
GET    /api/users/{user}/calendars
DELETE /api/users/{user}/calendars/{connectionId}
GET    /api/users/{user}/calendars/{connectionId}/events
GET    /api/users/{user}/availability
```

### Classements, récompenses et utilisateurs

```http
GET  /api/rankings/public/points
GET  /api/rankings/public/wins
GET  /api/watchparties/{name}/rankings/points
GET  /api/rewards/thresholds
POST /api/rewards/evaluate-thresholds
POST /api/rewards/monthly-top3
GET  /api/users
GET  /api/users/{username}
GET  /api/users/{username}/notifications
```

### Quiz

```http
GET  /api/quiz/status
POST /api/quiz/start
POST /api/quiz/answer
POST /api/quiz/reset
POST /api/quiz/questions
```

## CI/CD

Le dépôt utilise GitHub Actions pour :

- builder le projet
- exécuter les tests
- produire les rapports JaCoCo
- vérifier les seuils de couverture
- mettre à jour les badges de coverage

Workflows :

- [gradle.yml](/c:/Users/User/Documents/Code/Java/DevOps/.github/workflows/gradle.yml)
- [coverage-badge.yml](/c:/Users/User/Documents/Code/Java/DevOps/.github/workflows/coverage-badge.yml)

## Liens utiles

- dépôt GitHub : https://github.com/Alim-Samira/DevOps
- SonarCloud : https://sonarcloud.io/summary/new_code?id=Alim-Samira_DevOps
- Swagger UI : http://localhost:8080/swagger-ui.html
- documentation complémentaire : [docs/doc.md](/c:/Users/User/Documents/Code/Java/DevOps/docs/doc.md)

---

Projet maintenu par l'équipe `/alll`.
