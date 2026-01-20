<div align="center">

# DevOps Java Project – Chat, Bets, and Auto Watch Parties

![Java](https://img.shields.io/badge/Java-17-orange)
![Gradle](https://img.shields.io/badge/Gradle-9.x-02303A)
![JUnit](https://img.shields.io/badge/JUnit-5-brightgreen)
![Version](https://img.shields.io/badge/version-0.3.0-blue)

[![Java CI with Gradle](https://github.com/Alim-Samira/DevOps/actions/workflows/gradle.yml/badge.svg)](https://github.com/Alim-Samira/DevOps/actions/workflows/gradle.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Alim-Samira_DevOps&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Alim-Samira_DevOps)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=Alim-Samira_DevOps&metric=coverage)](https://sonarcloud.io/summary/new_code?id=Alim-Samira_DevOps)

</div>

## 🚀 Aperçu

Application Spring Boot 3 / Java 17 exposant une API REST pour gérer:
- **Chats** (public/privé) avec messages horodatés et mini-jeux
- **Paris** (Bet/Choice) avec points, votes, annulation et répartition des gains
- **Auto Watch Parties** via Leaguepedia API (fetch des prochains matchs + scheduler)
- **États de match** (PRE_MATCH → IN_PROGRESS → PAUSED → FINISHED)

**v0.3.0** : passage de l'application console à une API REST (frontend manquant pour l'instant). Aucune nouvelle fonctionnalité métier, mais exposition complète des capacités existantes via contrôleurs REST et ajout du suivi qualité SonarCloud.

## 🎯 Nouveautés v0.3.0 (migration REST)

- ✅ Passage complet en API REST Spring Boot (contrôleurs pour chat, paris, quiz, ranking, watch parties, utilisateurs)
- ✅ Réorganisation du code en packages `backend.controllers`, `backend.models`, `backend.services`
- ✅ Intégration SonarCloud (badges qualité et couverture)
- ⚠️ Frontend non fourni : tester via Swagger/Postman/cURL

Fonctionnalités métiers conservées (v0.2.x) et désormais exposées en REST :
- Auto Watch Parties basées sur Leaguepedia (scheduler, états, filtrage des matchs passés)
- Gestion d'états de match (admin) + lancement de mini-jeux conditionné
- Mini-jeux : interface `MiniGame`, `QuizGame` (scores + classement)
- Système de paris : `Bet` / `PublicBet` avec `vote()`, `setResult()`, `cancel()` et répartition des points

## 🧭 Structure du projet

```
.
├─ build.gradle.kts
├─ settings.gradle.kts
├─ gradlew / gradlew.bat
├─ src
│  ├─ main/java
│  │  ├─ backend
│  │  │  ├─ DevOpsApplication.java       # Entrée Spring Boot (API REST)
│  │  │  ├─ controllers/                 # Contrôleurs REST (chat, bet, quiz, ranking, user, watchparty)
│  │  │  ├─ models/                      # Domain (User, Chat, Bet, WatchParty, Match, etc.)
│  │  │  └─ services/                    # Services métier + scheduler + client Leaguepedia
│  └─ test/java
│     └─ backend/                     # Tests unitaires + intégration (MockMvc)
└─ ...
```

## ✅ Prérequis

- **JDK 17** installé (JAVA_HOME pointant vers Java 17)
- Pas besoin d'installer Gradle: le wrapper est inclus
- OS: Windows, macOS ou Linux

Vérifier votre Java:

```powershell
java -version
```

## 🔧 Installation

1. Cloner le dépôt

```powershell
git clone https://github.com/Alim-Samira/DevOps.git
cd DevOps
```

2. Build le projet

```powershell
.\gradlew build
```

## 🧪 Lancer les tests

Les tests JUnit 5 couvrent:
- Chat (public/privé, messages)
- Betting (vote, résultats, annulation, points)
- Watch Parties (création, join/leave, planification)
- Auto Watch Parties (création, états, scheduler)
- Tests d'intégration

```powershell
.\gradlew test
```

Rapport HTML des tests:
- `build/reports/tests/test/index.html`

Résultat attendu: **37+ tests passés** ✅

## ▶️ Lancement de l'application
API REST (Tomcat embarqué, port 8080) :

```powershell
.\gradlew bootRun
# puis consommer l'API sur http://localhost:8080
```

Exemples de ressources (selon les contrôleurs) :
- `GET /ranking` – récupérer le classement
- `POST /quiz/answer` – répondre à une question de quiz
- `POST /bet/public` – créer un pari public
- `POST /watchparty/auto` – créer une auto watch party (team/tournament)
- `DELETE /watchparty/{name}` – supprimer une watch party

## 🎮 Utilisation

### 1. Auto Watch Parties

```
=== AUTO WATCH PARTIES ===
User: Alice
Scheduler: RUNNING

1. Create Auto WP (Team)       # Ex: T1, G2, Gen.G
2. Create Auto WP (Tournament)  # Ex: Worlds 2025, LCK Spring
3. List All Watch Parties
4. Join Watch Party
5. Leave Watch Party
6. Delete Watch Party (admin)
7. Force Scheduler Update
8. Back
```

**Exemple:**
1. Sélectionner option 1 (Team)
2. Entrer "T1"
3. Le système crée une auto watch party qui s'ouvre 30 min avant chaque match de T1
4. Le scheduler vérifie toutes les 5 minutes et met à jour les statuts

### 2. Système de paris

```
=== BETTING SYSTEM ===
User: Alice | Points: 200

1. Create Bet
2. Vote on Bet
3. End Voting & Set Result
4. Cancel Bet
5. Show Balances
6. Back
```

**Exemple:**
1. Créer un pari: "Qui va gagner?" avec options "T1, Gen.G, G2"
2. Voter avec des points (ex: 50 points sur T1)
3. Définir le résultat (ex: T1 gagne)
4. Les points sont redistribués aux gagnants

### 3. Mini-jeux (admins seulement)

Dans un chat, les admins peuvent lancer:
```
!quiz start     # Démarre le quiz
[répondre aux questions]
!quiz exit      # Affiche les résultats
```

## 🧩 Fonctionnalités clés

### Chat System
- Messages horodatés avec ID unique
- Likes et reports sur les messages
- Réponses (reply) aux messages
- Mini-jeux intégrables

### Betting System
- Interface `Bet` standardisée
- Vote avec déduction de points
- Calcul automatique des gains (redistribution proportionnelle)
- Annulation avec remboursement
- Support des paris publics et privés

### Auto Watch Parties
- Création automatique basée sur équipe ou tournoi
- Intégration Leaguepedia API (récupération des matchs à venir)
- Scheduler automatique (vérification toutes les 5 min)
- Ouverture 30 min avant le match
- Fermeture automatique après le match
- Gestion des participants (join/leave)
- Filtrage des matchs passés

### Match Management (Admin)
- États: SCHEDULED → LIVE → FINISHED
- Changement manuel d'état
- Auto-mise à jour via API
- Conditions pour lancer les mini-jeux



## 📦 Dépendances
Stack principale (voir `build.gradle.kts`) :
- Spring Boot 3 / Spring Web
- Spring Boot Starter Test (JUnit 5)
- Gson (parsing JSON Leaguepedia)
- Gradle 9 (wrapper inclus)

## 🧪 Tests

- ~37 tests (unitaires + intégration MockMvc)
- Rapport HTML : `build/reports/tests/test/index.html`
- Lancer: `./gradlew test`

## 📝 Notes de version

### v0.3.0 (Janvier 2026)
- 🌀 Migration complète en API REST Spring Boot
- 🏷️ Ajout badges SonarCloud (quality gate, coverage)
- 🧱 Réorganisation en packages controllers/models/services
- ⚠️ Pas de nouvelles features métier

### v0.2.0 (Décembre 2025)
- ✨ Auto Watch Parties avec Leaguepedia integration
- ✨ Mini-jeux (Quiz) dans les chats
- ✨ Gestion d'états de match (admin)
- ✨ Système de classement amélioré
- 🔧 Refactorisation complète de Main.java
- 🔧 27+ tests unitaires et d'intégration

### v0.1.1 (Précédent)
- Chat public/privé de base
- Système de paris simple
- Watch parties manuelles

## 📜 Licence

Ce dépôt est fourni à des fins pédagogiques.

---