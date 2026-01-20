package backend.storage;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * Manager centralisé pour la persistance des données en JSON
 */
@Service
public class PersistenceManager {
    
    private final JsonStorage jsonStorage;
    private final Map<String, JsonStorageRepository<?>> repositories = new HashMap<>();
    
    public PersistenceManager(JsonStorage jsonStorage) {
        this.jsonStorage = jsonStorage;
    }
    
    /**
     * Crée ou récupère un repository pour une classe d'entité
     */
    @SuppressWarnings("unchecked")
    public <T> JsonStorageRepository<T> getRepository(Class<T> entityClass, String collectionName) {
        String key = entityClass.getSimpleName() + "_" + collectionName;
        
        return (JsonStorageRepository<T>) repositories.computeIfAbsent(key, k -> 
            new JsonStorageRepository<>(entityClass, collectionName)
        );
    }
    
    /**
     * Accès direct au JsonStorage pour les cas plus simples
     */
    public JsonStorage getJsonStorage() {
        return jsonStorage;
    }
}
