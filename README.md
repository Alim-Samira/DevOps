<div align="center">

# /alll – Esports Watch Party & Betting Platform

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen)
![Gradle](https://img.shields.io/badge/Gradle-9.x-02303A)
![Version](https://img.shields.io/github/v/release/Alim-Samira/DevOps?label=version)

[![Java CI with Gradle](https://github.com/Alim-Samira/DevOps/actions/workflows/gradle.yml/badge.svg)](https://github.com/Alim-Samira/DevOps/actions/workflows/gradle.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Alim-Samira_DevOps&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Alim-Samira_DevOps)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=Alim-Samira_DevOps&metric=coverage)](https://sonarcloud.io/summary/new_code?id=Alim-Samira_DevOps)

**_/alll_** — un clin d'œil aux initiales des développeurs et à la commande `/all` emblématique du jeu vidéo.

</div>

---

## 🚀 Aperçu

**API REST Spring Boot** pour gérer des watch parties esports avec système de paris avancé, classements, récompenses et intégration temps réel via Leaguepedia. Idéale pour organiser des sessions de visionnage communautaire avec engagement compétitif.

### 🎯 Les 5 fonctionnalités principales

#### 1️⃣ **Watch Parties**
Créez des sessions de visionnage pour vos matchs esports favoris. Gérez les participants, suivez les états de match (WAITING → OPEN → CLOSED), et rejoignez des événements publics ou orgnisez des événements privés avec systèmes de points séparés.

#### 2️⃣ **Création automatique via Leaguepedia API**
Intégration complète avec l'API Leaguepedia (Cargo) pour créer automatiquement des watch parties basées sur :
- **Équipes** : suivez vos équipes préférées (T1, Gen.G, G2, etc.)
- **Tournois** : suivez des compétitions entières (Worlds, LCK, LEC, etc.)
- **Scheduler intelligent** : vérification toutes les 5 min, ouverture 30 min avant le match, fermeture automatique après

#### 3️⃣ **Système de paris avancé**
Trois types de paris pour maximiser l'engagement :
- **DiscreteChoiceBet** : choix parmi 2-4 options (Qui va gagner ?)
- **NumericValueBet** : prédiction de valeur numérique (Durée du match ?)
- **OrderedRankingBet** : classement ordonné (Top 5 joueurs ?)

Fonctionnalités : modification en cours de vote, tickets spéciaux, résolution intelligente (algorithme de proximité, distance de Kendall).

#### 4️⃣ **Chats intégrés**
Communication en temps réel avec :
- **Chats** : messages horodatés par watch party
- **Mini-jeux** : Quiz interactif avec scores et classements
- **Commandes admins** : `!quiz start`, `!quiz exit` pour lancer les jeux

#### 5️⃣ **Système de récompenses**
Triple système de récompenses pour fidéliser les joueurs :
- **Paliers de progression** : récompenses automatiques à 1000, 5000, 10000+ points
- **Tickets spéciaux** : gagnés dans les paris, permettent d'influencer les paris en cours (ajuster mise, changer valeur)
- **Récompenses mensuelles** : top 3 du classement global reçoit des récompenses exclusives

---

### 📊 Suivi qualité
- **SonarCloud** : Quality Gate
- **CI/CD** : GitHub Actions (build, tests, déploiement)
- **Tests** : 46+ tests unitaires et d'intégration

## 🎯 Nouveautés v1.0-α

### 🎲 Système de paris avancé
- **3 types de paris** :
  - `DiscreteChoiceBet` : choix parmi 2-4 options (ex: quelle équipe gagne ?)
  - `NumericValueBet` : prédiction d'une valeur numérique (ex: durée du match)
  - `OrderedRankingBet` : classement ordonné d'items (ex: top 5 joueurs)
- **Machine à états** : VOTING → PENDING → RESOLVED/CANCELED
- **Modification de paris** : ajuster son choix/valeur pendant la phase de vote
- **Résolution intelligente** : algorithme de proximité (NumericValue) et distance de Kendall (Ranking)

### 🎫 Système de tickets
- **4 types de tickets** : DISCRETE_CHOICE, NUMERIC_VALUE, ORDERED_RANKING, IN_OR_OUT
- **Obtention** : gagnés dans certains paris (10% de chance bonus IN_OR_OUT)
- **Utilisation** : modifier sa mise ou valeur sur un pari en état PENDING
- **Consommation unique** : chaque ticket utilisé est retiré de l'inventaire

### 🏆 Système de récompenses
- **Paliers de progression** : récompenses automatiques à 1000, 5000, 10000+ points
- **Récompenses mensuelles** : top 3 du classement global (points + victoires)

### 📊 Classements améliorés
- **4 classements** :
  - Global (points publics)
  - Global (victoires publiques)
  - Par watch party (points privés)
  - Par watch party (victoires privées)
- **Cache intelligent** : rafraîchissement uniquement si changement détecté
- **Endpoints dédiés** : `/api/ranking/global/{points|wins}`, `/api/ranking/watchparty/{name}/{points|wins}`

### 🧪 Tests renforcés
- **46+ tests** : couverture des paris, classements, récompenses, tickets
- **Tests d'intégration** : MockMvc pour les contrôleurs REST
- **Tests métier** : logique de distribution, algorithmes de résolution

### 🔧 Qualité du code
- **Architecture propre** : séparation claire controllers/services/models
- **Documentation** : diagramme de classes PlantUML, workflows GitHub Actions

---

## 🧭 Structure du projet

```
.
├─ build.gradle.kts
├─ settings.gradle.kts
├─ src/
│  ├─ main/java/backend/
│  │  ├─ DevOpsApplication.java
│  │  ├─ controllers/          # REST endpoints
│  │  │  ├─ BetController.java
│  │  │  ├─ QuizController.java
│  │  │  ├─ RankingController.java
│  │  │  ├─ RewardController.java
│  │  │  ├─ UserController.java
│  │  │  └─ WatchPartyController.java
│  │  ├─ models/               # Domain objects
│  │  │  ├─ Bet.java (abstract)
│  │  │  ├─ DiscreteChoiceBet.java
│  │  │  ├─ NumericValueBet.java
│  │  │  ├─ OrderedRankingBet.java
│  │  │  ├─ User.java
│  │  │  ├─ WatchParty.java
│  │  │  ├─ TicketType.java
│  │  │  └─ ...
│  │  └─ services/             # Business logic
│  │     ├─ BetService.java
│  │     ├─ WatchPartyManager.java
│  │     ├─ RankingService.java
│  │     ├─ RewardService.java
│  │     ├─ AutoWatchPartyScheduler.java
│  │     ├─ LeaguepediaClient.java
│  │     └─ ...
│  └─ test/java/backend/
│     ├─ MainTest.java
│     └─ ControllerIntegrationTest.java
├─ docs/
│  ├─ class-diagram.png
│  ├─ class-diagram.puml
│  └─ doc.md
└─ .github/workflows/
   ├─ gradle.yml
   └─ doc.yml
```

## ✅ Prérequis

- **JDK 17+** (JAVA_HOME configuré)
- **Gradle** : wrapper inclus (pas d'installation requise)
- **OS** : Windows, macOS, Linux

Vérifier votre Java :

```bash
java -version
# Sortie attendue : openjdk version "17.x.x"
```

## 🔧 Installation & Lancement

### 1. Cloner le projet

```bash
git clone https://github.com/Alim-Samira/DevOps.git
cd DevOps
```

### 2. Build

```bash
./gradlew build
```

### 3. Tests

```bash
./gradlew test
```

Rapport HTML : `build/reports/tests/test/index.html`  
**46+ tests**

### 4. Lancer l'API

```bash
./gradlew bootRun
```

L'API démarre sur **http://localhost:8080**

---

## 🌐 Endpoints API

### 📋 Documentation interactive
- **Swagger UI** : http://localhost:8080/swagger-ui/index.html
- **OpenAPI JSON** : http://localhost:8080/v3/api-docs

### 🎲 Paris (`/api/watchparties/{name}/bets`)

```http
# Créer un pari à choix discret
POST /api/watchparties/{name}/bets/discrete-choice
{
  "admin": "alice",
  "question": "Qui va gagner ?",
  "choices": ["T1", "Gen.G"],
  "votingMinutes": 10,
  "offersTicket": true
}

# Voter sur un pari
POST /api/watchparties/{name}/bets/vote
{
  "user": "bob",
  "value": "T1",
  "points": 50
}

# Terminer le vote
POST /api/watchparties/{name}/bets/end-voting
{ "admin": "alice" }

# Résoudre le pari
POST /api/watchparties/{name}/bets/resolve
{
  "admin": "alice",
  "correctValue": "T1"
}

# Utiliser un ticket
POST /api/watchparties/{name}/bets/use-ticket
{
  "user": "bob",
  "ticketType": "IN_OR_OUT",
  "newPoints": 100
}
```

### 🏆 Classements (`/api/ranking`)

```http
# Classement global par points
GET /api/ranking/global/points?forceRefresh=false

# Classement global par victoires
GET /api/ranking/global/wins

# Classement d'une watch party
GET /api/ranking/watchparty/{name}/points
```

### 🎁 Récompenses (`/api/rewards`)

```http
# Paliers disponibles
GET /api/rewards/thresholds

# Forcer évaluation des paliers
POST /api/rewards/evaluate-thresholds

# Calculer top 3 mensuel
POST /api/rewards/monthly-top3
```

### 🎮 Watch Parties (`/api/watchparties`)

```http
# Créer une watch party manuelle
POST /api/watchparties/manual
{
  "user": "alice",
  "name": "T1 vs Gen.G",
  "date": "2026-02-01T19:00:00",
  "game": "League of Legends",
  "isPublic": true
}

# Créer une auto watch party
POST /api/watchparties/auto
{
  "user": "alice",
  "name": "T1",
  "type": "TEAM"
}

# Rejoindre
POST /api/watchparties/{name}/join
{ "user": "bob" }

# Quitter
POST /api/watchparties/{name}/leave
{ "user": "bob" }
```

### 💬 Chat & Quiz

**Chat**: See WatchParty endpoints (`GET/POST /api/watchparties/{name}/chat`)

**Quiz** (`/api/quiz`):

```http
# Répondre au quiz
POST /api/quiz/answer
{
  "username": "bob",
  "answer": "Paris"
}
```

---

## 🎮 Cas d'usage complet

### Scénario : Match T1 vs Gen.G

1. **Alice (admin) crée une watch party**
   ```bash
   POST /api/watchparties/auto {"user":"alice","name":"T1","type":"TEAM"}
   ```

2. **Le scheduler ouvre automatiquement la watch party 30 min avant le match**

3. **Alice crée un pari**
   ```bash
   POST /api/watchparties/T1/bets/discrete-choice
   {"admin":"alice","question":"Qui va gagner ?","choices":["T1","Gen.G"],"offersTicket":true}
   ```

4. **Bob et Charlie votent**
   ```bash
   POST /api/watchparties/T1/bets/vote {"user":"bob","value":"T1","points":100}
   POST /api/watchparties/T1/bets/vote {"user":"charlie","value":"Gen.G","points":50}
   ```

5. **Alice ferme le vote**
   ```bash
   POST /api/watchparties/T1/bets/end-voting {"admin":"alice"}
   ```

6. **Le match commence, Bob utilise un ticket IN_OR_OUT pour ajuster sa mise**
   ```bash
   POST /api/watchparties/T1/bets/use-ticket
   {"user":"bob","ticketType":"IN_OR_OUT","newPoints":150}
   ```

7. **T1 gagne ! Alice résout le pari**
   ```bash
   POST /api/watchparties/T1/bets/resolve {"admin":"alice","correctValue":"T1"}
   ```

8. **Bob gagne 150 points + un ticket DISCRETE_CHOICE (+ 10% chance de IN_OR_OUT bonus)**

9. **Bob franchit le palier 1000 points → récompense automatique**

10. **Fin du mois : Bob est dans le top 3 → récompense mensuelle**

---

## 🎲 Détails des types de paris

### DiscreteChoiceBet (Choix discret)
- **Question** : "Quelle équipe va gagner ?"
- **Options** : 2-4 choix (ex: T1, Gen.G, DRX)
- **Vote** : choisir une option + miser des points
- **Résolution** : redistribution équitable du pot aux gagnants
- **Exemple payload** :
  ```json
  {
    "admin": "alice",
    "question": "Quelle équipe va gagner ?",
    "choices": ["T1", "Gen.G"],
    "votingMinutes": 10,
    "offersTicket": true
  }
  ```

### NumericValueBet (Valeur numérique)
- **Question** : "Durée du match en minutes ?"
- **Contraintes** : entier ou flottant, min/max optionnels
- **Vote** : soumettre une valeur numérique + miser
- **Résolution** : 
  - Match exact → 100% du pot équitablement
  - Sinon → algorithme de proximité (LinkedHashMap pour priorité au premier voté)
- **Exemple payload** :
  ```json
  {
    "admin": "alice",
    "question": "Durée du match en minutes ?",
    "isInteger": true,
    "minValue": 20,
    "maxValue": 60,
    "votingMinutes": 10
  }
  ```

### OrderedRankingBet (Classement ordonné)
- **Question** : "Classez les 5 meilleurs joueurs"
- **Items** : liste d'éléments à ordonner
- **Vote** : soumettre un classement complet + miser
- **Résolution** :
  - Match parfait → 100% du pot
  - Sinon → distance de Kendall (tau) pour récompenser proximité
- **Exemple payload** :
  ```json
  {
    "admin": "alice",
    "question": "Top 5 joueurs du match",
    "items": ["Faker", "Chovy", "Showmaker", "Zeka", "Doran"],
    "votingMinutes": 15
  }
  ```

---

## 🎫 Système de tickets

### Types de tickets
- **DISCRETE_CHOICE** : modifier son choix sur un pari à choix discret (état PENDING)
- **NUMERIC_VALUE** : modifier sa valeur sur un pari numérique (état PENDING)
- **ORDERED_RANKING** : modifier son classement (état PENDING)
- **IN_OR_OUT** : ajuster sa mise (augmenter/diminuer/se retirer) sur n'importe quel pari (état PENDING)

### Obtention
- Gagner un pari qui offre des tickets (`offersTicket: true`)
- 10% de chance de recevoir un ticket IN_OR_OUT bonus

### Utilisation
```http
POST /api/watchparties/{name}/bets/use-ticket
{
  "user": "bob",
  "ticketType": "IN_OR_OUT",
  "newPoints": 200  # ou newValue selon le type de pari
}
```

### Restrictions
- Ticket consommé après utilisation
- Pari doit être en état PENDING
- Ticket doit correspondre au type de pari (sauf IN_OR_OUT qui fonctionne partout)
- Impossible d'utiliser un ticket sur un pari qui offre lui-même un ticket

---

## 🏆 Système de récompenses

### Paliers de progression
Points franchis → récompense automatique :
- **1 000 points** → Récompense Bronze
- **5 000 points** → Récompense Argent
- **10 000 points** → Récompense Or
- **25 000 points** → Récompense Platine
- **50 000 points** → Récompense Diamant

Endpoints :
```http
GET /api/rewards/thresholds         # Voir les paliers
POST /api/rewards/evaluate-thresholds  # Forcer l'évaluation
```

### Récompenses mensuelles
**Top 3 du classement global** (points + victoires) en fin de mois :
- 🥇 1ère place → Récompense Champion
- 🥈 2ème place → Récompense Vice-Champion
- 🥉 3ème place → Récompense Challenger

Endpoint :
```http
POST /api/rewards/monthly-top3
```

### Scheduler automatique
- `RewardScheduler` s'exécute quotidiennement
- Évalue les paliers automatiquement
- Distribution mensuelle le 1er du mois

---

## 📊 Classements

### Types de classements
1. **Global (points publics)** : tous les points gagnés dans les watch parties publiques
2. **Global (victoires publiques)** : nombre de paris gagnés dans les WP publiques
3. **Par watch party (points privés)** : points dans une WP privée spécifique
4. **Par watch party (victoires privées)** : victoires dans une WP privée

### Endpoints
```http
GET /api/ranking/global/points?forceRefresh=false
GET /api/ranking/global/wins
GET /api/ranking/watchparty/{name}/points
GET /api/ranking/watchparty/{name}/wins
```

### Cache intelligent
- Rafraîchissement uniquement si changements détectés
- `forceRefresh=true` pour forcer la mise à jour
- Utilisé par `RewardService` pour calculer le top 3

---

## 🧪 Tests

### Lancer les tests
```bash
./gradlew test
```

### Couverture
- **46+ tests** unitaires et d'intégration
- Rapport HTML : `build/reports/tests/test/index.html`
- Couverture SonarCloud : voir badge en haut du README

### Catégories testées
- ✅ Paris (création, vote, résolution, annulation)
- ✅ Tickets (attribution, utilisation, restrictions)
- ✅ Classements (cache, rafraîchissement, top 3)
- ✅ Récompenses (paliers, mensuel, scheduler)
- ✅ Watch Parties (auto, manuel, statuts)
- ✅ Intégration REST (MockMvc, controllers)

---

## 🔧 Configuration

### application.properties (optionnel)
```properties
# Port du serveur (défaut: 8080)
server.port=8080

# Logs
logging.level.backend=INFO
logging.level.org.springframework=WARN
```

### Scheduler
- **Auto Watch Party** : vérification toutes les 5 minutes
- **Récompenses** : évaluation quotidienne
- Modifiable dans `AutoWatchPartyScheduler` et `RewardScheduler`

---

## 📦 Dépendances principales

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web:3.4.1")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter")
}
```

- **Spring Boot 3.4.1** : framework REST
- **Gson 2.10.1** : parsing JSON Leaguepedia
- **SpringDoc OpenAPI 2.3.0** : Swagger UI
- **JUnit 5** : tests unitaires/intégration

---

## 📝 Notes de version

### v1.0-α (Janvier 2026)
- 🎲 **Système de paris avancé** : 3 types (DiscreteChoice, NumericValue, OrderedRanking)
- 🎫 **Système de tickets** : 4 types, obtention via paris gagnants, utilisation sur paris PENDING
- 🏆 **Récompenses** : paliers de progression, top 3 mensuel, scheduler automatique
- 📊 **Classements** : global + par watch party, cache intelligent
- 🎮 **Points séparés** : publics (WP publiques) vs privés (WP privées)
- 🔧 **Architecture** : relation unidirectionnelle WatchParty → Bet

### v0.3.0 (Décembre 2025)
- 🌀 Migration complète en API REST Spring Boot
- 🏷️ Badges SonarCloud (quality gate, coverage)
- 🧱 Réorganisation en packages controllers/models/services
- ✨ Swagger UI pour documentation interactive

### v0.2.0 (Novembre 2025)
- ✨ Auto Watch Parties avec Leaguepedia
- ✨ Mini-jeux (Quiz) dans les chats
- ✨ Gestion d'états de match
- 🔧 27+ tests unitaires

### v0.1.0 (Octobre 2025)
- 🎬 Version initiale console
- Chat public/privé
- Système de paris simple
- Watch parties manuelles

---

## 🤝 Contribution

Le projet est maintenu par l'équipe **/alll** 

### Workflow Git
```bash
# Branche dev pour le développement
git checkout dev
git pull origin dev

# Créer une branche feature
git checkout -b feature/ma-fonctionnalite

# Après commit, merge dans dev
git checkout dev
git merge feature/ma-fonctionnalite

# Merge dev → main pour les releases
git checkout main
git merge dev
```

### Standards de code
- ✅ Code Java conforme aux conventions Spring Boot
- ✅ Tests pour toute nouvelle fonctionnalité
- ✅ Corrections SonarCloud obligatoires
- ✅ Documentation inline

---

## 📄 Licence

Ce projet est fourni à des fins pédagogiques.

---

## 🔗 Liens utiles

- **GitHub** : https://github.com/Alim-Samira/DevOps
- **SonarCloud** : https://sonarcloud.io/summary/new_code?id=Alim-Samira_DevOps
- **CI/CD** : https://github.com/Alim-Samira/DevOps/actions
- **Documentation** : [docs/doc.md](docs/doc.md)
- **Diagramme de classes** : [docs/class-diagram.puml](docs/class-diagram.puml)

---

<div align="center">

**Développé par l'équipe /alll**

</div>