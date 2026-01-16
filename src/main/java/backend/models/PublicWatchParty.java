package backend.models;

import java.time.LocalDateTime;

/**
 * WatchParty publique: points gagnés/utilisés sur le solde public global.
 */
public class PublicWatchParty extends WatchParty {
    public PublicWatchParty(String name, LocalDateTime date, String game) {
        super(name, date, game);
        this.setPublic(true);
    }
}
