import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // Create users
        User admin = new User("Admin", true);
        User user1 = new User("Alice", false);
        User user2 = new User("RB", false);

        // Create chatrooms
        PublicChat publicChat = new PublicChat("Public Chat", admin);  // Pass both name and admin
        PrivateChat privateChat = new PrivateChat("Private Chat", admin);  // Pass both name and admin

        // Add users to chatrooms
        publicChat.addUser(admin);
        publicChat.addUser(user1);
        publicChat.addUser(user2);

        privateChat.addUser(admin);
        privateChat.addUser(user1);

        // Scanner to read user input
        Scanner scanner = new Scanner(System.in);
        User currentUser = user1;  // Let's assume user1 starts

        while (true) {
            // Show the menu to the user
            System.out.println("\nChoose a chatroom:");
            System.out.println("1. Public Chat");
            System.out.println("2. Private Chat");
            System.out.println("3. Exit");
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
                startChat(publicChat, currentUser);
            } else if (chatChoice == 2) {
                // Choose the private chat
                startChat(privateChat, currentUser);
            } else {
                System.out.println("Invalid choice, try again.");
            }
        }

        scanner.close();
    }

    // Method to handle the chat interactions
    private static void startChat(Chat currentChat, User currentUser) {
        Scanner scanner = new Scanner(System.in);
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
            Thread.sleep(1000);  // Wait 1 second before sending the message
            chat.sendMessage(new User("RB", true), response);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
