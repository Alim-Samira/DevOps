import java.time.LocalDateTime;

public class WatchParty {

    private String name;
    private LocalDateTime date;
    private String game;
    private boolean planned;

    public WatchParty(String name, LocalDateTime date, String game) {
        this.name = name;
        this.date = date;
        this.game = game;
        this.planned=false;
    }

     public String name() {
        return name;
    }

    public LocalDateTime date() {
        return date;
    }

    public String game() {
        return game;
    }

    public boolean isPlanned(){
        return planned;
    }

    public void toPlan() {
        this.planned= true;
        System.out.println(" WatchParty planifiée : " + name +
                           " | Jeu : " + game +
                           " | Date : " + date);    
    }

    
    public void displayInfos() {
        System.out.println( "Nom : " + name);
        System.out.println("date : " + date);
        System.out.println("jeu : " + game);
        System.out.println("Planifiee : " + (planned ? "oui" : "non"));
    }
}






  
   

    

        
    

