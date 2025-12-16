package backend.models;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

import backend.services.WatchPartyManager;

/**
 * Main entry point for the DevOps Chat & Watch Party System
 * Features: Chat (Public/Private), Betting System, Auto Watch Parties
 */

@SuppressWarnings("java:S106")
public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private static final String ADMIN_USER = "Admin";
    private static final String INVALID_SELECTION_MSG = "[!] Invalid selection.";
    private static User currentUser;
    private static WatchPartyManager wpManager;

    public static void main(String[] args) {
        initializeSystem();
        runMainMenu();
        cleanup();
    }

    private static void initializeSystem() {
        System.out.println("=== DevOps System Starting ===");
        
        // Initialize watch party manager with scheduler
        wpManager = new WatchPartyManager();
        wpManager.startScheduler();
        System.out.println("[OK] Watch Party System Initialized");
        
        // Set default user
        currentUser = new User("Alice", false);
        System.out.println("[OK] System Ready\n");
    }

    private static void runMainMenu() {
        while (true) {
            displayMainMenu();
            String choice = scanner.nextLine().trim().toLowerCase();

            if (choice.equals("e") || choice.equals("exit")) {
                break;
            }

            int menuChoice = parseChoice(choice);
            if (!handleMainMenuChoice(menuChoice)) {
                break;
            }
        }
    }

    private static void displayMainMenu() {
        System.out.println("\n=== MAIN MENU ===");
        System.out.println("Current User: " + currentUser.getName() + " | Points: " + currentUser.getPoints());
        System.out.println("\n1. Public Chat");
        System.out.println("2. Private Chat");
        System.out.println("3. Betting System");
        System.out.println("4. Auto Watch Parties");
        System.out.println("5. Exit");
        System.out.print("\nChoice (or 'e' to exit): ");
    }

    private static boolean handleMainMenuChoice(int choice) {
        switch (choice) {
            case 1:
                enterPublicChat();
                break;
            case 2:
                enterPrivateChat();
                break;
            case 3:
                enterBettingMenu();
                break;
            case 4:
                enterWatchPartyMenu();
                break;
            case 5:
                return false;
            default:
                System.out.println("[!] Invalid choice, try again.");
        }
        return true;
    }

    // ==================== CHAT SYSTEM ====================

    private static void enterPublicChat() {
        User admin = new User(ADMIN_USER, true);
        PublicChat chat = new PublicChat("Public Chat", admin);
        chat.addUser(admin);
        chat.addUser(currentUser);
        chat.addUser(new User("RB", false));
        runChatSession(chat);
    }

    private static void enterPrivateChat() {
        User admin = new User(ADMIN_USER, true);
        PrivateChat chat = new PrivateChat("Private Chat", admin);
        chat.addUser(admin);
        chat.addUser(currentUser);
        runChatSession(chat);
    }

    private static void runChatSession(Chat chat) {
        System.out.println("\n[+] Entering " + chat.getName());
        chat.listMessages();

        while (true) {
            System.out.print("\nMessage (type 'exit' to leave): ");
            String message = scanner.nextLine();

            if (message.equalsIgnoreCase("exit") || message.equalsIgnoreCase("e")) {
                System.out.println("[-] Leaving chat...");
                break;
            }

            chat.sendMessage(currentUser, message);
            chat.listMessages();
        }
    }

    // ==================== BETTING SYSTEM ====================

    private static void enterBettingMenu() {
        User admin = new User(ADMIN_USER, true);
        User user1 = currentUser;
        User user2 = new User("Bob", false);
        
        PublicBet currentBet = null;
        List<Choice> currentChoices = null;

        while (true) {
            displayBettingMenu();
            int choice = parseChoice(scanner.nextLine().trim());

            switch (choice) {
                case 1:
                    BetResult result = createBet();
                    currentBet = result.bet;
                    currentChoices = result.choices;
                    break;
                case 2:
                    voteOnBet(currentBet, currentChoices);
                    break;
                case 3:
                    endBetAndSetResult(currentBet, currentChoices, admin, user1, user2);
                    break;
                case 4:
                    cancelBet(currentBet, admin, user1, user2);
                    break;
                case 5:
                    showBalances(admin, user1, user2);
                    break;
                case 6:
                    return;
                default:
                    System.out.println(INVALID_SELECTION_MSG);
            }
        }
    }

    private static void displayBettingMenu() {
        System.out.println("\n=== BETTING SYSTEM ===");
        System.out.println("User: " + currentUser.getName() + " | Points: " + currentUser.getPoints());
        System.out.println("\n1. Create Bet");
        System.out.println("2. Vote on Bet");
        System.out.println("3. End Voting & Set Result");
        System.out.println("4. Cancel Bet");
        System.out.println("5. Show Balances");
        System.out.println("6. Back");
        System.out.print("\nChoice: ");
    }

    private static BetResult createBet() {
        System.out.print("Question: ");
        String question = scanner.nextLine().trim();
        
        System.out.print("Options (comma separated): ");
        String[] parts = scanner.nextLine().trim().split(",");
        
        List<Choice> choices = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                choices.add(new Choice(trimmed));
            }
        }

        if (choices.isEmpty()) {
            System.out.println("[!] No valid options entered; bet not created.");
            return new BetResult(null, null);
        }

        Collection<Choice> optionsCol = new ArrayList<>(choices);
        Time votingTime = new Time(System.currentTimeMillis() + 10 * 60 * 1000);
        PublicBet bet = new PublicBet(question, optionsCol, votingTime);
        
        System.out.println("[+] Bet created: '" + question + "' with " + choices.size() + " options.");
        return new BetResult(bet, choices);
    }

    private static void voteOnBet(PublicBet bet, List<Choice> choices) {
        if (bet == null || choices == null) {
            System.out.println("[!] No active bet. Create one first.");
            return;
        }

        System.out.println("\nOptions:");
        for (int i = 0; i < choices.size(); i++) {
            System.out.println((i + 1) + ". " + choices.get(i));
        }

        System.out.print("Choose option #: ");
        int idx = parseChoice(scanner.nextLine().trim()) - 1;
        
        if (idx < 0 || idx >= choices.size()) {
            System.out.println("[!] Invalid option.");
            return;
        }

        System.out.print("Points to bet (you have " + currentUser.getPoints() + "): ");
        int points = parseChoice(scanner.nextLine().trim());
        
        if (points <= 0 || points > currentUser.getPoints()) {
            System.out.println("[!] Invalid points amount.");
            return;
        }

        bet.vote(currentUser, choices.get(idx), points);
        System.out.println("[+] Voted " + points + " points on option #" + (idx + 1));
    }

    private static void endBetAndSetResult(PublicBet bet, List<Choice> choices, User... users) {
        if (bet == null || choices == null) {
            System.out.println("[!] No active bet.");
            return;
        }

        System.out.println("\nPick winning option:");
        for (int i = 0; i < choices.size(); i++) {
            System.out.println((i + 1) + ". " + choices.get(i));
        }

        System.out.print("Winner #: ");
        int wIdx = parseChoice(scanner.nextLine().trim()) - 1;
        
        if (wIdx < 0 || wIdx >= choices.size()) {
            System.out.println("[!] Invalid option.");
            return;
        }

        bet.setResult(choices.get(wIdx));
        System.out.println("[+] Result set. Updated balances:");
        showBalances(users);
    }

    private static void cancelBet(PublicBet bet, User... users) {
        if (bet == null) {
            System.out.println("[!] No active bet.");
            return;
        }

        bet.cancel();
        System.out.println("[+] Bet canceled. Points refunded.");
        showBalances(users);
    }

    private static void showBalances(User... users) {
        System.out.println("\n--- Balances ---");
        for (User u : users) {
            System.out.println(u.getName() + ": " + u.getPoints() + " points");
        }
    }

    // ==================== WATCH PARTY SYSTEM ====================

    private static void enterWatchPartyMenu() {
        while (true) {
            displayWatchPartyMenu();
            int choice = parseChoice(scanner.nextLine().trim());

            switch (choice) {
                case 1:
                    createAutoWatchParty(AutoType.TEAM);
                    break;
                case 2:
                    createAutoWatchParty(AutoType.TOURNAMENT);
                    break;
                case 3:
                    listAllWatchParties();
                    break;
                case 4:
                    joinWatchParty();
                    break;
                case 5:
                    leaveWatchParty();
                    break;
                case 6:
                    deleteWatchParty();
                    break;
                case 7:
                    forceSchedulerUpdate();
                    break;
                case 8:
                    return;
                default:
                    System.out.println(INVALID_SELECTION_MSG);
            }
        }
    }

    private static void displayWatchPartyMenu() {
        System.out.println("\n=== AUTO WATCH PARTIES ===");
        System.out.println("User: " + currentUser.getName());
        System.out.println("Scheduler: " + (wpManager.isSchedulerRunning() ? "RUNNING" : "STOPPED"));
        System.out.println("\n1. Create Auto WP (Team)");
        System.out.println("2. Create Auto WP (Tournament)");
        System.out.println("3. List All Watch Parties");
        System.out.println("4. Join Watch Party");
        System.out.println("5. Leave Watch Party");
        System.out.println("6. Delete Watch Party (admin)");
        System.out.println("7. Force Scheduler Update");
        System.out.println("8. Back");
        System.out.print("\nChoice: ");
    }

    private static void createAutoWatchParty(AutoType type) {
        String prompt = type == AutoType.TEAM ? 
            "Team name (e.g., T1, G2, Gen.G): " : 
            "Tournament name (e.g., Worlds 2025): ";
        
        System.out.print(prompt);
        String target = scanner.nextLine().trim();
        
        if (target.isEmpty()) {
            System.out.println("[!] Invalid name.");
            return;
        }

        WatchParty wp = WatchParty.createAutoWatchParty(currentUser, target, type);
        wpManager.addAutoWatchParty(wp);
        
        System.out.println("[+] Auto watch party created for " + type.toString().toLowerCase() + ": " + target);
        System.out.println("    Opens 30 minutes before each match.");
    }

    private static void listAllWatchParties() {
        List<WatchParty> allWP = wpManager.getAllWatchParties();
        
        if (allWP.isEmpty()) {
            System.out.println("[!] No watch parties exist.");
            return;
        }

        System.out.println("\n=== ALL WATCH PARTIES ===");
        for (int i = 0; i < allWP.size(); i++) {
            WatchParty wp = allWP.get(i);
            System.out.println("\n[" + (i + 1) + "] " + wp.name());
            
            if (wp.isAutoWatchParty()) {
                AutoConfig config = wp.getAutoConfig();
                System.out.println("    Type: Auto (" + config.getType() + ")");
                System.out.println("    Target: " + config.getTarget());
                System.out.println("    Status: " + wp.getStatus());
                
                if (config.getCurrentMatch() != null) {
                    System.out.println("    Next match: " + config.getCurrentMatch());
                }
                
                boolean canJoin = wp.getStatus() == WatchPartyStatus.OPEN;
                System.out.println("    Can join: " + (canJoin ? "YES" : "NO (" + wp.getStatus() + ")"));
            } else {
                System.out.println("    Type: Manual");
                System.out.println("    Date: " + wp.date());
                System.out.println("    Can join: YES");
            }
            
            System.out.println("    Participants: " + wp.getParticipants().size());
        }
    }

    private static void joinWatchParty() {
        List<WatchParty> allWP = wpManager.getAllWatchParties();
        
        if (allWP.isEmpty()) {
            System.out.println("[!] No watch parties to join.");
            return;
        }

        System.out.println("\nWatch Parties:");
        for (int i = 0; i < allWP.size(); i++) {
            WatchParty wp = allWP.get(i);
            String status = wp.isAutoWatchParty() ? wp.getStatus().toString() : "OPEN";
            System.out.println((i + 1) + ". " + wp.name() + " [" + status + "]");
        }

        System.out.print("Enter #: ");
        int idx = parseChoice(scanner.nextLine().trim()) - 1;
        
        if (idx < 0 || idx >= allWP.size()) {
            System.out.println(INVALID_SELECTION_MSG);
            return;
        }

        allWP.get(idx).join(currentUser);
    }

    private static void leaveWatchParty() {
        List<WatchParty> myParties = getUserWatchParties();
        
        if (myParties.isEmpty()) {
            System.out.println("[!] You are not in any watch parties.");
            return;
        }

        System.out.println("\nYour Watch Parties:");
        for (int i = 0; i < myParties.size(); i++) {
            System.out.println((i + 1) + ". " + myParties.get(i).name());
        }

        System.out.print("Enter #: ");
        int idx = parseChoice(scanner.nextLine().trim()) - 1;
        
        if (idx < 0 || idx >= myParties.size()) {
            System.out.println(INVALID_SELECTION_MSG);
            return;
        }

        myParties.get(idx).leave(currentUser);
    }

    private static void deleteWatchParty() {
        if (!currentUser.isAdmin()) {
            System.out.println("[!] Only admins can delete watch parties.");
            return;
        }

        List<WatchParty> allWP = wpManager.getAllWatchParties();
        
        if (allWP.isEmpty()) {
            System.out.println("[!] No watch parties to delete.");
            return;
        }

        System.out.println("\nWatch Parties:");
        for (int i = 0; i < allWP.size(); i++) {
            System.out.println((i + 1) + ". " + allWP.get(i).name());
        }

        System.out.print("Enter #: ");
        int idx = parseChoice(scanner.nextLine().trim()) - 1;
        
        if (idx < 0 || idx >= allWP.size()) {
            System.out.println(INVALID_SELECTION_MSG);
            return;
        }

        wpManager.removeWatchParty(allWP.get(idx).name());
        System.out.println("[+] Watch party deleted.");
    }

    private static void forceSchedulerUpdate() {
        System.out.println("[*] Forcing scheduler update (checking up to 7 days ahead)...");
        String report = wpManager.forceSchedulerUpdateReport(7);
        System.out.println("\n" + report);
    }

    private static List<WatchParty> getUserWatchParties() {
        List<WatchParty> myParties = new ArrayList<>();
        for (WatchParty wp : wpManager.getAllWatchParties()) {
            if (wp.getParticipants().contains(currentUser)) {
                myParties.add(wp);
            }
        }
        return myParties;
    }

    // ==================== UTILITY METHODS ====================

    private static int parseChoice(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static void cleanup() {
        System.out.println("\n[*] Shutting down...");
        wpManager.stopScheduler();
        scanner.close();
        System.out.println("[OK] Goodbye!");
    }

    // ==================== HELPER CLASSES ====================

    private static class BetResult {
        PublicBet bet;
        List<Choice> choices;
        
        BetResult(PublicBet bet, List<Choice> choices) {
            this.bet = bet;
            this.choices = choices;
        }
    }
}
