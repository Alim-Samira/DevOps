<div align="center">

# DevOps Java Project – Chat, Bets, and Watch Parties

![Java](https://img.shields.io/badge/Java-17-orange)
![Gradle](https://img.shields.io/badge/Gradle-9.x-02303A)
![JUnit](https://img.shields.io/badge/JUnit-5-brightgreen)

[![Java CI with Gradle](https://github.com/Alim-Samira/DevOps/actions/workflows/gradle.yml/badge.svg)](https://github.com/Alim-Samira/DevOps/actions/workflows/gradle.yml)

</div>

## 🚀 Aperçu

Projet Java (structure Gradle standard) fournissant:
- Un mini système de chat (public/privé) avec messages horodatés
- Un module de pari (Bet/Choice) avec points utilisateurs
- La gestion de Watch Parties (création, planification, affichage)

L’application propose:
- une exécution interactive en console (fichier JAR fourni dans la release v0.1),
- et une suite de tests JUnit 5 pour valider les fonctionnalités.

## 🧭 Structure du projet

```
.
├─ build.gradle.kts
├─ settings.gradle.kts
├─ gradlew / gradlew.bat
├─ src
│  ├─ main
│  │  └─ java
│  │     ├─ Bet.java
│  │     ├─ Chat.java
│  │     ├─ Choice.java
│  │     ├─ Main.java
│  │     ├─ Message.java
│  │     ├─ PrivateChat.java
│  │     ├─ PublicChat.java
│  │     ├─ User.java
│  │     ├─ WatchParty.java
│  │     └─ WatchPartyManager.java
│  └─ test
│     └─ java
│        └─ MainTest.java
└─ ...
```

## ✅ Prérequis

- JDK 17 installé (JAVA_HOME pointant vers Java 17)
- Pas besoin d’installer Gradle: le wrapper est inclus
- OS: Windows, macOS ou Linux (exemples ci-dessous pour Windows PowerShell)

Vérifier rapidement votre Java:

```powershell
java -version
```

## 🔧 Installation

1) Cloner le dépôt

```powershell
git clone <URL_DU_DEPOT>
cd DevOps
```

2) (Optionnel) Nettoyer et reconstruire

```powershell
./gradlew.bat clean build
```

## 🧪 Lancer les tests

Les tests JUnit 5 couvrent les principales fonctionnalités (chat, bets, watch parties):

```powershell
./gradlew.bat test
```

Rapport HTML des tests:
- build/reports/tests/test/index.html

## ▶️ Lancement de l’application (JAR release v0.1)

La release actuelle v0.1 fournit un JAR exécutable prêt à l’emploi. Prérequis: Java 17.

• Release v0.1: https://github.com/Alim-Samira/DevOps/releases/tag/v0.1

1) Téléchargez le JAR de la release v0.1 depuis l’onglet "Releases" de ce dépôt.
2) Depuis PowerShell (Windows):

```powershell
java -jar .\DevOps-v0.1.jar
```

Sur Linux/macOS:

```bash
java -jar ./DevOps-v0.1.jar
```

Notes:
- Remplacez DevOps-v0.1.jar par le nom exact du fichier si besoin (voir les Assets de la release v0.1).
- L’application est interactive (menu console: Chat public/privé, Bets, etc.).

## 🧩 Fonctionnalités clés

- Chat (classe abstraite `Chat`) et implémentations `PublicChat` et `PrivateChat`
	- Envoi de messages avec horodatage
	- Journalisation des messages
- Module Bet/Choice
	- Vote avec points utilisateurs
	- Calcul des récompenses et restitution des points
- Watch Parties
	- Création et planification (future uniquement)
	- Affichage de la liste et des infos

## 🛠️ Développement (VS Code recommandé)

- Extensions utiles: Pack « Extension Pack for Java », Test Runner for Java
- Lancement rapide des tests: onglet Testing ou terminal Gradle

## 📦 Dépendances principales

- Java 17 (toolchain Gradle)
- JUnit 5 (API + Engine + Launcher)

Extrait `build.gradle.kts` pertinent:

```kotlin
plugins {
		java
}

repositories { mavenCentral() }

dependencies {
		testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
		testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
		testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.9.3")
}

tasks.withType<Test> { useJUnitPlatform() }
```

## 🧹 Tâches Gradle utiles

```powershell
# Nettoyer les outputs
./gradlew.bat clean

# Compiler uniquement
./gradlew.bat classes

# Lancer tous les tests
./gradlew.bat test
```

## 📜 Licence

Ce dépôt est fourni à des fins pédagogiques.

---