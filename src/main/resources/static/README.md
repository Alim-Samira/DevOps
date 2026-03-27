Frontend demo (vanilla JS)

- Emplacement: `src/main/resources/static/` (UI accessible à la racine)
- Accès: http://localhost:8080/
- But: prototype UI pour tester les endpoints REST (WP CRUD, création pari, vote, résolution, rankings, lookup user)

Usage:
1. Démarrer l'application Spring Boot: `./gradlew bootRun`
2. Ouvrir: http://localhost:8080/

Aucune dépendance ni build nécessaire.

Calendrier Google :
- mode `Connexion Google` : le bouton `Connecter` lance OAuth Google
- mode `Invitation Google` : le bouton `Connecter` enregistre seulement l'email de l'invité
- aucun token Google n'est saisi manuellement dans l'interface
- un utilisateur connecté via OAuth peut servir d'organisateur et envoyer des invitations Google Calendar
- un utilisateur configuré en `Invitation Google` peut recevoir l'invitation sans connecter son agenda
