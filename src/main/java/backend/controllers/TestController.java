package backend.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ce contrôleur vérifie que l'injection de dépendances et le serveur REST
 * fonctionnent correctement dans le contexte de votre projet DevOps.
 */
@RestController 
public class TestController {

    // Mappe cette méthode à l'URL de base : http://localhost:8080/
    @GetMapping("/")
    public String checkStatus() {
        return "Serveur REST du Projet DevOps en ligne. Prêt pour les WatchParties.";
    }
    
    // Mappe cette méthode à l'URL : http://localhost:8080/test
    @GetMapping("/test")
    public String getProjectTest() {
        return "Validation du chemin Spring Boot OK !";
    }
}