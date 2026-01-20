package backend.storage;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * Service de stockage JSON persistant pour les données de l'application
 */
@Component
public class JsonStorage {
    
    private static final String DATA_DIR = "data";
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    public JsonStorage() {
        // Créer le répertoire de données s'il n'existe pas
        Path dataPath = Paths.get(DATA_DIR);
        try {
            Files.createDirectories(dataPath);
        } catch (IOException e) {
            System.err.println("Erreur lors de la création du répertoire data: " + e.getMessage());
        }
    }
    
    /**
     * Sauvegarde un objet en JSON
     * @param filename nom du fichier (sans extension)
     * @param data l'objet à sauvegarder
     */
    public <T> void save(String filename, T data) {
        try {
            String filepath = DATA_DIR + File.separator + filename + ".json";
            try (FileWriter writer = new FileWriter(filepath)) {
                gson.toJson(data, writer);
            }
            System.out.println("Données sauvegardées: " + filepath);
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }
    
    /**
     * Charge un objet depuis un fichier JSON
     * @param filename nom du fichier (sans extension)
     * @param clazz classe de l'objet à charger
     * @return Optional contenant l'objet ou vide si le fichier n'existe pas
     */
    public <T> Optional<T> load(String filename, Class<T> clazz) {
        try {
            String filepath = DATA_DIR + File.separator + filename + ".json";
            File file = new File(filepath);
            
            if (!file.exists()) {
                return Optional.empty();
            }
            
            try (FileReader reader = new FileReader(filepath)) {
                T data = gson.fromJson(reader, clazz);
                return Optional.ofNullable(data);
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Sauvegarde une Map de données
     * @param filename nom du fichier
     * @param dataMap map à sauvegarder
     */
    public void saveMap(String filename, Map<String, Object> dataMap) {
        save(filename, dataMap);
    }
    
    /**
     * Charge une Map depuis un fichier JSON
     * @param filename nom du fichier
     * @return Map chargée ou une Map vide
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> loadMap(String filename) {
        return load(filename, Map.class)
                .orElse(new HashMap<>());
    }
    
    /**
     * Supprime un fichier de données
     * @param filename nom du fichier
     */
    public boolean delete(String filename) {
        try {
            String filepath = DATA_DIR + File.separator + filename + ".json";
            return Files.deleteIfExists(Paths.get(filepath));
        } catch (IOException e) {
            System.err.println("Erreur lors de la suppression: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Vérifie si un fichier existe
     * @param filename nom du fichier
     */
    public boolean exists(String filename) {
        String filepath = DATA_DIR + File.separator + filename + ".json";
        return Files.exists(Paths.get(filepath));
    }
    
    /**
     * Charge un élément JSON brut pour plus de flexibilité
     * @param filename nom du fichier
     */
    public Optional<JsonElement> loadRaw(String filename) {
        try {
            String filepath = DATA_DIR + File.separator + filename + ".json";
            File file = new File(filepath);
            
            if (!file.exists()) {
                return Optional.empty();
            }
            
            try (FileReader reader = new FileReader(filepath)) {
                return Optional.of(JsonParser.parseReader(reader));
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement: " + e.getMessage());
            return Optional.empty();
        }
    }
}
