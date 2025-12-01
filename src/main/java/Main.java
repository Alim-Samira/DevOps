import java.sql.Time;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // Create users
        User admin = new User("Admin", true);
        User user1 = new User("Alice", false);
        User user2 = new User("RB", false);

        // Create chatrooms
        PublicChat publicChat = new PublicChat("Public Chat", admin);
        PrivateChat privateChat = new PrivateChat("Private Chat", admin);

        // Add users to chatrooms
        publicChat.addUser(admin);
        publicChat.addUser(user1);
        publicChat.addUser(user2);

        privateChat.addUser(admin);
        privateChat.addUser(user1);
        
        // Watch Party Manager with auto scheduler
        WatchPartyManager wpManager = new WatchPartyManager();
        wpManager.startScheduler();
        System.out.println("=== Watch Party System Initialized ===");

        // Scanner to read user input
        Scanner scanner = new Scanner(System.in);
        User currentUser = user1;  // Let's assume user1 starts

        // Betting system state (managed from the menu)
        PublicBet currentBet = null;
        List<Choice> currentChoices = null;

        while (true) {
            // Show the menu to the user
            System.out.println("\nChoose a chatroom:");
            System.out.println("1. Public Chat");
            System.out.println("2. Private Chat");
            System.out.println("3. Bets");
            System.out.println("4. Auto Watch Parties");
            System.out.println("5. Exit");
            System.out.print("Enter your choice (or 'e' to exit): ");
            
            // Read the input as a string
            String choice = scanner.nextLine().trim().toLowerCase();  // Make it case-insensitive

            // Exit if the user types 'e'
            if (choice.equals("e")) {
                System.out.println("Exiting chat...");
                wpManager.stopScheduler();
                break;  // Exit the program
            }

            // Parse the choice as an integer if it's a number
            int chatChoice = -1;
            try {
                chatChoice = Integer.parseInt(choice);
            } catch (NumberFormatException e) {
                // If the input isn't a number, it'll just stay -1, meaning invalid choice
            }

            // Handle the chatroom choice
            if (chatChoice == 1) {
                // Choose the public chat
                startChat(scanner, publicChat, currentUser);
            } else if (chatChoice == 2) {
                // Choose the private chat
                startChat(scanner, privateChat, currentUser);
            } else if (chatChoice == 3) {
                // Open bets menu
                BetAndChoices result = betsMenu(scanner, currentUser, currentBet, currentChoices, admin, user1, user2);
                currentBet = result.bet;
                currentChoices = result.choices;
            } else if (chatChoice == 4) {
                // Open auto watch parties menu
                autoWatchPartiesMenu(scanner, currentUser, wpManager, admin);
            } else if (chatChoice == 5) {
                System.out.println("Exiting chat...");
                wpManager.stopScheduler();
                break;  // Exit the program
            } else {
                System.out.println("Invalid choice, try again.");
            }
        }

        scanner.close();
    }

    // Method to handle the chat interactions
    private static void startChat(Scanner scanner, Chat currentChat, User currentUser) {
        System.out.println("\nYou have entered the " + currentChat.getName() + "!");
        currentChat.listMessages();  // List previous messages

        while (true) {
            System.out.print("\nEnter your message (type 'exit', 'n', or 'e' to leave chat): ");
            String messageContent = scanner.nextLine();

            // Check if user wants to exit chat
            if (messageContent.equalsIgnoreCase("exit") || messageContent.equalsIgnoreCase("n") || messageContent.equalsIgnoreCase("e")) {
                System.out.println("Exiting the chatroom...");
                break;  // Exit the current chatroom
            }

            // Send user message with timestamp
            currentChat.sendMessage(currentUser, messageContent);

            // Process the message only once and reply if it's a valid greeting, question, or thank you
            if (messageContent.trim().length() > 0) {
                // Bot replies after a delay
                if (isGreeting(messageContent)) {
                    sendBotResponse(currentChat, "Hi! I'm RB the robot. How can I help you today?");
                } else if (isHowAreYou(messageContent)) {
                    sendBotResponse(currentChat, "I'm doing great, and you?");
                } else if (isWhatsUp(messageContent)) {
                    sendBotResponse(currentChat, "The sky! Wait... I'm joking. I'm doing great! What about you?");
                } else if (isThanks(messageContent)) {
                    sendBotResponse(currentChat, "Happy to help!");
                } else if (isResponse(messageContent)) {
                    sendBotResponse(currentChat, "That's nice to hear!");
                }
            }

            // List messages after both user and bot message are sent
            currentChat.listMessages();  // Display messages after both user and bot reply
        }
    }

    // Helper holder for returning both bet and its options
    private static class BetAndChoices {
        PublicBet bet;
        List<Choice> choices;
        BetAndChoices(PublicBet bet, List<Choice> choices) { this.bet = bet; this.choices = choices; }
    }

    // Bets menu: create bet, vote, end/cancel, show balances
    private static BetAndChoices betsMenu(Scanner scanner, User currentUser, PublicBet currentBet, List<Choice> currentChoices,
                                          User admin, User user1, User user2) {
        while (true) {
            System.out.println("\n=== Bets Menu ===");
            System.out.println("Current user: " + currentUser.getName() + " | Points: " + currentUser.getPoints());
            System.out.println("1. Create bet");
            System.out.println("2. Vote on current bet");
            System.out.println("3. End voting and set result");
            System.out.println("4. Cancel current bet");
            System.out.println("5. Show all balances");
            System.out.println("6. Back");
            System.out.print("Choose: ");

            String line = scanner.nextLine().trim();
            int sel;
            try { sel = Integer.parseInt(line); } catch (NumberFormatException ex) { sel = -1; }

            switch (sel) {
                case 1: {
                    // Create a new bet
                    System.out.print("Enter question: ");
                    String question = scanner.nextLine().trim();
                    System.out.print("Enter options (comma separated): ");
                    String opts = scanner.nextLine().trim();
                    String[] parts = opts.split(",");
                    List<Choice> choices = new ArrayList<>();
                    for (String p : parts) {
                        String t = p.trim();
                        if (!t.isEmpty()) choices.add(new Choice(t));
                    }
                    if (choices.isEmpty()) {
                        System.out.println("No valid options entered; bet not created.");
                        break;
                    }
                    Collection<Choice> optionsCol = new ArrayList<>(choices);
                    Time votingTime = new Time(System.currentTimeMillis() + 10 * 60 * 1000); // 10 minutes window
                    currentBet = new PublicBet(question, optionsCol, votingTime);
                    currentChoices = choices;
                    System.out.println("Bet created: " + question + " with " + choices.size() + " options.");
                    break;
                }
                case 2: {
                    if (currentBet == null || currentChoices == null) {
                        System.out.println("No active bet. Create one first.");
                        break;
                    }
                    // List options
                    System.out.println("Options:");
                    for (int i = 0; i < currentChoices.size(); i++) {
                        System.out.println((i + 1) + ". " + currentChoices.get(i));
                    }
                    System.out.print("Choose option number: ");
                    String sIdx = scanner.nextLine().trim();
                    int idx;
                    try { idx = Integer.parseInt(sIdx) - 1; } catch (NumberFormatException ex) { idx = -1; }
                    if (idx < 0 || idx >= currentChoices.size()) {
                        System.out.println("Invalid option.");
                        break;
                    }
                    Choice choice = currentChoices.get(idx);
                    System.out.print("Enter points to bet (you have " + currentUser.getPoints() + "): ");
                    String sPts = scanner.nextLine().trim();
                    int pts;
                    try { pts = Integer.parseInt(sPts); } catch (NumberFormatException ex) { pts = -1; }
                    if (pts <= 0 || pts > currentUser.getPoints()) {
                        System.out.println("Invalid points amount.");
                        break;
                    }
                    currentBet.vote(currentUser, choice, pts);
                    System.out.println("Voted " + pts + " points on option #" + (idx + 1));
                    break;
                }
                case 3: {
                    if (currentBet == null || currentChoices == null) {
                        System.out.println("No active bet.");
                        break;
                    }
                    System.out.println("Pick winning option:");
                    for (int i = 0; i < currentChoices.size(); i++) {
                        System.out.println((i + 1) + ". " + currentChoices.get(i));
                    }
                    System.out.print("Winner #: ");
                    String sW = scanner.nextLine().trim();
                    int wIdx;
                    try { wIdx = Integer.parseInt(sW) - 1; } catch (NumberFormatException ex) { wIdx = -1; }
                    if (wIdx < 0 || wIdx >= currentChoices.size()) {
                        System.out.println("Invalid option.");
                        break;
                    }
                    currentBet.setResult(currentChoices.get(wIdx));
                    System.out.println("Result set. Updated balances:");
                    showBalances(admin, user1, user2);
                    break;
                }
                case 4: {
                    if (currentBet == null) {
                        System.out.println("No active bet.");
                        break;
                    }
                    currentBet.cancel();
                    System.out.println("Bet canceled. Points refunded where applicable.");
                    showBalances(admin, user1, user2);
                    break;
                }
                case 5: {
                    showBalances(admin, user1, user2);
                    break;
                }
                case 6:
                    return new BetAndChoices(currentBet, currentChoices);
                default:
                    System.out.println("Invalid selection.");
            }
        }
    }

    private static void showBalances(User... users) {
        System.out.println("-- Balances --");
        for (User u : users) {
            System.out.println(u.getName() + ": " + u.getPoints());
        }
    }
    
    // Auto Watch Parties Menu
    private static void autoWatchPartiesMenu(Scanner scanner, User currentUser, WatchPartyManager manager, User admin) {
        while (true) {
            System.out.println("\n=== Auto Watch Parties ===");
            System.out.println("Current user: " + currentUser.getName());
            System.out.println("Scheduler status: " + (manager.isSchedulerRunning() ? "RUNNING" : "STOPPED"));
            System.out.println("\n1. Create Auto Watch Party (Team)");
            System.out.println("2. Create Auto Watch Party (Tournament)");
            System.out.println("3. List All Watch Parties");
            System.out.println("4. Join Watch Party");
            System.out.println("5. Leave Watch Party");
            System.out.println("6. Delete Auto Watch Party (admin)");
            System.out.println("7. Force Scheduler Update (debug)");
            System.out.println("8. Back");
            System.out.print("Choose: ");
            
            String line = scanner.nextLine().trim();
            int sel;
            try { sel = Integer.parseInt(line); } catch (NumberFormatException ex) { sel = -1; }
            
            switch (sel) {
                case 1: {
                    System.out.print("Enter team name (e.g., T1, G2, Gen.G): ");
                    String team = scanner.nextLine().trim();
                    if (team.isEmpty()) {
                        System.out.println("Invalid team name.");
                        break;
                    }
                    WatchParty wp = WatchParty.createAutoWatchParty(currentUser, team, AutoType.TEAM);
                    manager.addAutoWatchParty(wp);
                    System.out.println("[+] Auto watch party created for team: " + team);
                    System.out.println("    It will open 30 minutes before each " + team + " match.");
                    break;
                }
                case 2: {
                    System.out.print("Enter tournament name (e.g., Worlds 2025, LCK Spring 2025): ");
                    String tournament = scanner.nextLine().trim();
                    if (tournament.isEmpty()) {
                        System.out.println("Invalid tournament name.");
                        break;
                    }
                    WatchParty wp = WatchParty.createAutoWatchParty(currentUser, tournament, AutoType.TOURNAMENT);
                    manager.addAutoWatchParty(wp);
                    System.out.println("[+] Auto watch party created for tournament: " + tournament);
                    System.out.println("    It will open 30 minutes before each match.");
                    break;
                }
                case 3: {
                    List<WatchParty> allWP = manager.getAllWatchParties();
                    if (allWP.isEmpty()) {
                        System.out.println("No watch parties exist.");
                    } else {
                        System.out.println("\n=== All Watch Parties ===");
                        for (int i = 0; i < allWP.size(); i++) {
                            WatchParty wp = allWP.get(i);
                            System.out.println("\n[" + (i + 1) + "] " + wp.name());
                            if (wp.isAutoWatchParty()) {
                                System.out.println("    Type: Auto (" + wp.getAutoConfig().getType() + ")");
                                System.out.println("    Target: " + wp.getAutoConfig().getTarget());
                                System.out.println("    Status: " + wp.getStatus());
                                if (wp.getAutoConfig().getCurrentMatch() != null) {
                                    System.out.println("    Next match: " + wp.getAutoConfig().getCurrentMatch());
                                }
                            } else {
                                System.out.println("    Type: Manual");
                                System.out.println("    Date: " + wp.date());
                            }
                            System.out.println("    Participants: " + wp.getParticipants().size());
                            System.out.println("    Can join: " + 
                                (wp.isAutoWatchParty() ? (wp.getStatus() == WatchPartyStatus.OPEN ? "YES" : "NO (status: " + wp.getStatus() + ")") : "YES"));
                        }
                    }
                    break;
                }
                case 4: {
                    List<WatchParty> allWP = manager.getAllWatchParties();
                    if (allWP.isEmpty()) {
                        System.out.println("No watch parties to join.");
                        break;
                    }
                    System.out.println("Watch parties:");
                    for (int i = 0; i < allWP.size(); i++) {
                        System.out.println((i + 1) + ". " + allWP.get(i).name() + 
                            " [" + (allWP.get(i).isAutoWatchParty() ? allWP.get(i).getStatus() : "OPEN") + "]");
                    }
                    System.out.print("Enter number to join: ");
                    String idxStr = scanner.nextLine().trim();
                    int idx;
                    try { idx = Integer.parseInt(idxStr) - 1; } catch (NumberFormatException ex) { idx = -1; }
                    if (idx < 0 || idx >= allWP.size()) {
                        System.out.println("Invalid selection.");
                        break;
                    }
                    allWP.get(idx).join(currentUser);
                    break;
                }
                case 5: {
                    List<WatchParty> allWP = manager.getAllWatchParties();
                    if (allWP.isEmpty()) {
                        System.out.println("No watch parties to leave.");
                        break;
                    }
                    // Filter to only parties user is in
                    List<WatchParty> myParties = new ArrayList<>();
                    for (WatchParty wp : allWP) {
                        if (wp.getParticipants().contains(currentUser)) {
                            myParties.add(wp);
                        }
                    }
                    if (myParties.isEmpty()) {
                        System.out.println("You are not in any watch parties.");
                        break;
                    }
                    System.out.println("Your watch parties:");
                    for (int i = 0; i < myParties.size(); i++) {
                        System.out.println((i + 1) + ". " + myParties.get(i).name());
                    }
                    System.out.print("Enter number to leave: ");
                    String idxStr = scanner.nextLine().trim();
                    int idx;
                    try { idx = Integer.parseInt(idxStr) - 1; } catch (NumberFormatException ex) { idx = -1; }
                    if (idx < 0 || idx >= myParties.size()) {
                        System.out.println("Invalid selection.");
                        break;
                    }
                    myParties.get(idx).leave(currentUser);
                    break;
                }
                case 6: {
                    if (!currentUser.isAdmin()) {
                        System.out.println("[!] Only admins can delete watch parties.");
                        break;
                    }
                    List<WatchParty> allWP = manager.getAllWatchParties();
                    if (allWP.isEmpty()) {
                        System.out.println("No watch parties to delete.");
                        break;
                    }
                    System.out.println("Watch parties:");
                    for (int i = 0; i < allWP.size(); i++) {
                        System.out.println((i + 1) + ". " + allWP.get(i).name());
                    }
                    System.out.print("Enter number to delete: ");
                    String idxStr = scanner.nextLine().trim();
                    int idx;
                    try { idx = Integer.parseInt(idxStr) - 1; } catch (NumberFormatException ex) { idx = -1; }
                    if (idx < 0 || idx >= allWP.size()) {
                        System.out.println("Invalid selection.");
                        break;
                    }
                    manager.removeWatchParty(allWP.get(idx).name());
                    break;
                }
                case 7: {
                    System.out.println("Forcing scheduler update...");
                    manager.forceSchedulerUpdate();
                    System.out.println("Update complete. Check watch party statuses.");
                    break;
                }
                case 8:
                    return;
                default:
                    System.out.println("Invalid selection.");
            }
        }
    }

    // Method to check if the message is a greeting (hi, hey, hello, etc.)
    private static boolean isGreeting(String message) {
        String[] greetings = {"hi", "hey", "hello", "morning", "evening"};
        for (String greeting : greetings) {
            if (message.toLowerCase().contains(greeting)) {
                return true;
            }
        }
        return false;
    }

    // Method to check if the message is a "thank you"
    private static boolean isResponse(String message) {
        String[] response = {"good", "great", "I'm fine", "not bad", "doing well", "okay", "ok"};
        for (String res : response) {
            if (message.toLowerCase().contains(res)) {
                return true;
            }
        }
        return false;
    }

    // Method to check if the message asks "how are you?"
    private static boolean isHowAreYou(String message) {
        String[] howAreYouQuestions = {"how are you", "how's it going", "how's life", "how do you do"};
        for (String question : howAreYouQuestions) {
            if (message.toLowerCase().contains(question)) {
                return true;
            }
        }
        return false;
    }

    // Method to check if the message is "what's up?"
    private static boolean isWhatsUp(String message) {
        return message.toLowerCase().contains("what's up") || message.toLowerCase().contains("what is up");
    }

    // Method to check if the message is a "thank you"
    private static boolean isThanks(String message) {
        String[] thanks = {"thanks", "thank you", "thx"};
        for (String thank : thanks) {
            if (message.toLowerCase().contains(thank)) {
                return true;
            }
        }
        return false;
    }

    // Method to simulate bot response with delays and send it to the chat
    private static void sendBotResponse(Chat chat, String response) {
        try {
            // Simulate typing delay for fun
            Thread.sleep(500);  // Wait half a second before sending the message
            chat.sendMessage(new User("RB", true), response);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
