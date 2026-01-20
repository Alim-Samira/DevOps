package backend.models;
/**
 * Status of a watch party
 */
public enum WatchPartyStatus {
    WAITING,  // Created but no match imminent (auto watch parties)
    OPEN,     // Active and accepting participants
    CLOSED    // Match ended, waiting for next match (auto watch parties)
}
