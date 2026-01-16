package backend.models;

/**
 * Chat unifié pour les watchparties (remplace PublicChat/PrivateChat au sein des WatchParty).
 */
public class PartyChat extends Chat {
    public PartyChat(String name, User admin) {
        super(name, admin);
    }
}
