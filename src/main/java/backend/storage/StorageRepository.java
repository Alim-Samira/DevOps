package backend.storage;

import java.util.List;
import java.util.Optional;

/**
 * Interface générique pour gérer le stockage des entités
 */
public interface StorageRepository<T> {
    
    /**
     * Sauvegarde une entité
     */
    void save(T entity);
    
    /**
     * Charge une entité par son ID
     */
    Optional<T> findById(String id);
    
    /**
     * Charge toutes les entités
     */
    List<T> findAll();
    
    /**
     * Supprime une entité
     */
    boolean delete(String id);
    
    /**
     * Vérifie l'existence d'une entité
     */
    boolean exists(String id);
}
