package backend.models;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("PUBLIC") // what it will write in the 'type' column of the table
public class PublicChat extends Chat {

    // necessity for JPA
    public PublicChat() {
        super();
    }

    public PublicChat(String name, User admin) {
        super(name, admin); 
    }
}