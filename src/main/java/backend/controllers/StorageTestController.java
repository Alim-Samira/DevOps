package backend.controllers;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import backend.storage.JsonStorage;
import backend.storage.PersistenceManager;

/**
 * Contrôleur pour tester le système de stockage JSON
 */
@RestController
@RequestMapping("/api/storage")
public class StorageTestController {
    
    @Autowired
    private JsonStorage jsonStorage;
    
    @Autowired
    private PersistenceManager persistenceManager;
    
    /**
     * Teste la sauvegarde simple d'un objet JSON
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, String>> saveData(@RequestBody Map<String, Object> data) {
        try {
            String filename = (String) data.getOrDefault("filename", "test_data");
            Object content = data.getOrDefault("content", "");
            
            jsonStorage.save(filename, content);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Données sauvegardées: " + filename + ".json");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Teste le chargement d'un objet JSON
     */
    @GetMapping("/load/{filename}")
    public ResponseEntity<?> loadData(@PathVariable String filename) {
        try {
            Optional<?> data = jsonStorage.loadRaw(filename);
            
            if (data.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("filename", filename);
                response.put("data", data.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> response = new HashMap<>();
                response.put("status", "not_found");
                response.put("message", "Fichier non trouvé: " + filename + ".json");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Teste la vérification d'existence d'un fichier
     */
    @GetMapping("/exists/{filename}")
    public ResponseEntity<Map<String, Object>> checkExists(@PathVariable String filename) {
        Map<String, Object> response = new HashMap<>();
        boolean exists = jsonStorage.exists(filename);
        response.put("filename", filename);
        response.put("exists", exists);
        response.put("path", "data/" + filename + ".json");
        return ResponseEntity.ok(response);
    }
    
    /**
     * Teste la suppression d'un fichier
     */
    @DeleteMapping("/delete/{filename}")
    public ResponseEntity<Map<String, String>> deleteData(@PathVariable String filename) {
        try {
            boolean deleted = jsonStorage.delete(filename);
            
            Map<String, String> response = new HashMap<>();
            if (deleted) {
                response.put("status", "success");
                response.put("message", "Fichier supprimé: " + filename + ".json");
            } else {
                response.put("status", "not_found");
                response.put("message", "Fichier non trouvé: " + filename + ".json");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Endpoint de santé pour le système de stockage
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("storage", "JSON");
        response.put("location", "data/");
        return ResponseEntity.ok(response);
    }
}
