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

    // Scanner to read user input
    Scanner scanner = new Scanner(System.in);
    User currentUser = user1;  // Let's assume user1 starts

    // Betting system state (managed from the menu)
    Bet currentBet = null;
    List<Choice> currentChoices = null;

        while (true) {
            // Show the menu to the user
            System.out.println("\nChoose a chatroom:");
            System.out.println("1. Public Chat");
            System.out.println("2. Private Chat");
            System.out.println("3. Bets");
            System.out.println("4. Exit");
            System.out.print("Enter your choice (or 'e' to exit): ");
            
            // Read the input as a string
            String choice = scanner.nextLine().trim().toLowerCase();  // Make it case-insensitive

            // Exit if the user types 'e'
            if (choice.equals("e")) {
                System.out.println("Exiting chat...");
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
        Bet bet;
        List<Choice> choices;
        BetAndChoices(Bet bet, List<Choice> choices) { this.bet = bet; this.choices = choices; }
    }

    // Bets menu: create bet, vote, end/cancel, show balances
    private static BetAndChoices betsMenu(Scanner scanner, User currentUser, Bet currentBet, List<Choice> currentChoices,
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
                    currentBet = new Bet(question, optionsCol, votingTime);
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
                    currentBet.Vote(currentUser, choice, pts);
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
                    currentBet.SetResult(currentChoices.get(wIdx));
                    System.out.println("Result set. Updated balances:");
                    showBalances(admin, user1, user2);
                    break;
                }
                case 4: {
                    if (currentBet == null) {
                        System.out.println("No active bet.");
                        break;
                    }
                    currentBet.Cancel();
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
