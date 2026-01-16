package backend.models;

import java.time.LocalDateTime;

/**
 * WatchParty privée: points séparés par watchparty (isolés).
 */
public class PrivateWatchParty extends WatchParty {
    public PrivateWatchParty(String name, LocalDateTime date, String game) {
        super(name, date, game);
        this.setPublic(false);
    }
}
