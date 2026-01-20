<div align="center">

# DevOps Java Project – Chat, Bets, and Auto Watch Parties

![Java](https://img.shields.io/badge/Java-17-orange)
![Gradle](https://img.shields.io/badge/Gradle-9.x-02303A)
![JUnit](https://img.shields.io/badge/JUnit-5-brightgreen)
![Version](https://img.shields.io/badge/version-0.2.0-blue)

[![Java CI with Gradle](https://github.com/Alim-Samira/DevOps/actions/workflows/gradle.yml/badge.svg)](https://github.com/Alim-Samira/DevOps/actions/workflows/gradle.yml)

</div>

## 🚀 Aperçu

Projet Java (structure Gradle standard) fournissant un système complet de gestion d'événements esports avec:
- **Système de chat** (public/privé) avec messages horodatés et mini-jeux intégrés
- **Module de paris** (Bet/Choice) avec système de points et répartition des gains
- **Auto Watch Parties** - Création automatique de watch parties basée sur l'API Leaguepedia
- **Gestion d'états de match** - Suivi en temps réel des matchs
- **Mini-jeux** - Quiz intégrés dans les chats pour l'engagement des utilisateurs

L'application propose:
- une exécution interactive en console avec menu intuitif
- une suite de tests JUnit 5 complète (27+ tests)

## 🎯 Nouveautés v0.2.0

### ✨ Fonctionnalités ajoutées

**Auto Watch Parties**
- Création automatique de watch parties pour une équipe ou un tournoi
- Intégration avec Leaguepedia API (mock) pour récupérer les prochains matchs
- Scheduler qui ouvre/ferme automatiquement les watch parties (30 min avant le match)
- États: WAITING → OPEN → CLOSED
- Filtrage des matchs passés pour éviter les doublons

**Gestion d'états de match (Admin)**
- Enum `MatchState`: PRE_MATCH, IN_PROGRESS, PAUSED, FINISHED
- Admins peuvent changer l'état des matchs manuellement
- Lancement de mini-jeux conditionnel à l'état du match

**Mini-jeux dans les chats**
- Interface `MiniGame` pour créer des jeux personnalisés
- `QuizGame`: quiz interactif avec scores et classement
- Lancement réservé aux admins/modérateurs
- Commandes: `!quiz start`, `!quiz exit`

**Système de classement**
- Interface `Bet` pour standardiser les paris
- `PublicBet` implémente Bet avec méthodes `vote()`, `setResult()`, `cancel()`
- Gestion des points améliorée

### 🔧 Améliorations techniques

- Refactorisation complète de `Main.java` (meilleure organisation par sections)
- Extension des tests: 27 tests couvrant tous les scénarios
- Meilleure séparation des responsabilités (Chat, Betting, Watch Parties)
- Encodage Windows-1252 compatible (marqueurs ASCII au lieu d'emojis)

## 🧭 Structure du projet

```
.
├─ build.gradle.kts
├─ settings.gradle.kts
├─ gradlew / gradlew.bat
├─ src
│  ├─ main/java
│  │  ├─ Main.java                    # Point d'entrée avec menu interactif
│  │  ├─ User.java                    # Gestion utilisateurs et points
│  │  ├─ Chat.java                    # Classe abstraite pour chats
│  │  ├─ PublicChat.java              # Chat public
│  │  ├─ PrivateChat.java             # Chat privé
│  │  ├─ Message.java                 # Messages avec likes/reports
│  │  ├─ Bet.java                     # Interface pour les paris
│  │  ├─ PublicBet.java               # Implémentation publique des paris
│  │  ├─ Choice.java                  # Options de paris
│  │  ├─ WatchParty.java              # Watch party (manuelle ou auto)
│  │  ├─ WatchPartyManager.java       # Gestion et planification
│  │  ├─ AutoConfig.java              # Config pour auto watch parties
│  │  ├─ AutoType.java                # TEAM ou TOURNAMENT
│  │  ├─ WatchPartyStatus.java        # WAITING, OPEN, CLOSED
│  │  ├─ Match.java                   # Modèle de match esports
│  │  ├─ MatchState.java              # PRE_MATCH, IN_PROGRESS, PAUSED, FINISHED
│  │  ├─ LeaguepediaClient.java       # Client API (mock)
│  │  ├─ AutoWatchPartyScheduler.java # Scheduler automatique
│  │  ├─ MiniGame.java                # Interface mini-jeux
│  │  └─ QuizGame.java                # Quiz interactif
│  └─ test/java
│     └─ MainTest.java                # 27+ tests unitaires
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

Résultat attendu: **27 tests passed** ✅

## ▶️ Lancement de l'application

### Option 1: Via Gradle (recommandé)

```powershell
.\gradlew run
```

### Option 2: Via JAR

```powershell
.\gradlew jar
java -jar build/libs/DevOps-0.2.0.jar
```

### Menu principal

```
=== MAIN MENU ===
Current User: Alice | Points: 200

1. Public Chat
2. Private Chat
3. Betting System
4. Auto Watch Parties
5. Exit

Choice (or 'e' to exit):
```

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

## 🛠️ Développement

### VS Code (recommandé)

Extensions utiles:
- Extension Pack for Java
- Test Runner for Java
- Gradle for Java

### Tâches Gradle

```powershell
# Nettoyer
.\gradlew clean

# Compiler
.\gradlew classes

# Tester
.\gradlew test

# Build complet
.\gradlew build

# Exécuter
.\gradlew run

# Créer JAR
.\gradlew jar
```

## 📦 Dépendances

```kotlin
plugins {
    java
    application
}

group = "com.devops"
version = "0.2.0"

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.9.3")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

## 🧪 Tests

### Couverture des tests

- **Chat Tests** (3)
  - Public chat messaging
  - Private chat user management
  - Message storage

- **Betting Tests** (4)
  - Vote and point deduction
  - Result distribution
  - Bet cancellation
  - Points consistency

- **Watch Party Tests** (3)
  - Manual party creation and planning
  - Join/leave functionality
  - Party removal

- **Auto Watch Party Tests** (8)
  - Auto creation (team/tournament)
  - State transitions
  - Join restrictions
  - Manager tracking
  - Scheduler lifecycle
  - Multiple parties coexistence

- **Integration Tests** (9)
  - User points consistency
  - Multiple concurrent parties
  - Full workflow scenarios

**Total: 27 tests** ✅

## 📝 Notes de version

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

**Développé avec passion pour la communauté esports**
