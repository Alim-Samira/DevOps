package backend.storage;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Implémentation générique du repository JSON pour toute entité
 */
public class JsonStorageRepository<T> implements StorageRepository<T> {
    
    private static final String DATA_DIR = "data/entities";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Class<T> entityClass;
    private final String collectionName;
    
    public JsonStorageRepository(Class<T> entityClass, String collectionName) {
        this.entityClass = entityClass;
        this.collectionName = collectionName;
        createCollectionDirectory();
    }
    
    private void createCollectionDirectory() {
        try {
            String dirPath = DATA_DIR + File.separator + collectionName;
            Files.createDirectories(Paths.get(dirPath));
        } catch (IOException e) {
            System.err.println("Erreur lors de la création du répertoire: " + e.getMessage());
        }
    }
    
    private String getFilePath(String id) {
        return DATA_DIR + File.separator + collectionName + File.separator + id + ".json";
    }
    
    @Override
    public void save(T entity) {
        try {
            // Utiliser la réflexion pour obtenir l'ID (supposant une méthode getId())
            String id = getId(entity);
            String filePath = getFilePath(id);
            
            try (FileWriter writer = new FileWriter(filePath)) {
                gson.toJson(entity, writer);
            }
            System.out.println("Entité sauvegardée: " + filePath);
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }
    
    @Override
    public Optional<T> findById(String id) {
        try {
            String filePath = getFilePath(id);
            File file = new File(filePath);
            
            if (!file.exists()) {
                return Optional.empty();
            }
            
            try (FileReader reader = new FileReader(filePath)) {
                T entity = gson.fromJson(reader, entityClass);
                return Optional.ofNullable(entity);
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    @Override
    public List<T> findAll() {
        List<T> entities = new ArrayList<>();
        try {
            String dirPath = DATA_DIR + File.separator + collectionName;
            File directory = new File(dirPath);
            
            if (!directory.exists() || !directory.isDirectory()) {
                return entities;
            }
            
            File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    try (FileReader reader = new FileReader(file)) {
                        T entity = gson.fromJson(reader, entityClass);
                        if (entity != null) {
                            entities.add(entity);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement des entités: " + e.getMessage());
        }
        return entities;
    }
    
    @Override
    public boolean delete(String id) {
        try {
            return Files.deleteIfExists(Paths.get(getFilePath(id)));
        } catch (IOException e) {
            System.err.println("Erreur lors de la suppression: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean exists(String id) {
        return Files.exists(Paths.get(getFilePath(id)));
    }
    
    /**
     * Récupère l'ID d'une entité via réflexion
     */
    @SuppressWarnings("unchecked")
    private String getId(T entity) throws Exception {
        try {
            return (String) entity.getClass()
                    .getMethod("getId")
                    .invoke(entity);
        } catch (NoSuchMethodException e) {
            try {
                return String.valueOf(entity.getClass()
                        .getMethod("getId")
                        .invoke(entity));
            } catch (Exception ex) {
                throw new Exception("L'entité n'a pas de méthode getId()");
            }
        }
    }
}
