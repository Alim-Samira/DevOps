import java.util.Scanner;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.List; 
import java.util.stream.Collectors; 
import java.awt.Desktop; //  For opening browser
import java.net.URI;     // For defining the URL



public class Main {
    // A consistent date format for all timestamps
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy");

    public static void main(String[] args) {
        // --- User Initialization ---
        User admin = new User("Admin", true, true);         
        User mod = new User("Moderator", false, true);      
        User user1 = new User("Alice", false, false);       
        User user2 = new User("RB", false, false);          
        
        // Create chatrooms
        PublicChat publicChat = new PublicChat("Public Chat", admin);
        PrivateChat privateChat = new PrivateChat("Private Chat", admin);

        // Add users to chatrooms
        publicChat.addUser(mod);
        publicChat.addUser(admin);
        publicChat.addUser(user1);
        publicChat.addUser(user2);
        publicChat.addUser(new User("Bob", false, false));

        privateChat.addUser(admin);
        privateChat.addUser(user1);
        privateChat.addUser(new User("Charlie", false, false));

        // Scanner to read user input
        Scanner scanner = new Scanner(System.in);
        User sessionUser = user1; // Alice starts the session
        
        // Counter for failed login attempts across the session
        int failedAttemptsCount = 0;
        final int MAX_ATTEMPTS = 3;

        while (true) {
            // --- MAIN MENU ---
            System.out.println("\n===== Main Menu (Logged in as: " + sessionUser.getName() + ") =====");
            System.out.println("Choose a chatroom or action:");
            System.out.println("1.💬 Public Chat");
            System.out.println("2.💬 Private Chat");
            System.out.println("A.Access Admin Panel");
            System.out.println("e. Exit");
            System.out.print("Enter your choice (1, 2, A, or 'e' to exit): ");

            String choice = scanner.nextLine().trim().toLowerCase();

            if (choice.equals("e") || choice.equals("z")) { 
                System.out.println("Exiting chat application. Goodbye! 👋");
                break; 
            }
            
            if (choice.equals("a")) {
                // 1. Check if the user is already blocked
                if (failedAttemptsCount >= MAX_ATTEMPTS) {
                    System.out.println("⛔ Access Denied. Only Admins or Moderators can access the Admin Panel.");
                    continue;
                }
                
                // 2. Ask for the code
                System.out.print("Enter Admin Code: ");
                String code = scanner.nextLine().trim();
                
                if (code.equals("devops")) {
                    System.out.println("✅ Log in successful. Welcome to the Admin Panel.");
                    failedAttemptsCount = 0; // Reset counter
                    
                    // Pass chats to admin panel so we can add games to them
                    startAdminPanel(scanner, publicChat, privateChat, admin); 
                    
                } else {
                    failedAttemptsCount++;
                    System.out.println("⛔ Denied.");
                }
                continue; 
            }

            int chatChoice = -1;
            try {
                chatChoice = Integer.parseInt(choice);
            } catch (NumberFormatException e) {
                // Invalid choice
            }

            if (chatChoice == 1) {
                startChat(scanner, publicChat, sessionUser); 
            } else if (chatChoice == 2) {
                startChat(scanner, privateChat, sessionUser); 
            } else if (chatChoice != -1) {
                 System.out.println("Invalid choice, try again.");
            }
        }

        scanner.close();
    }
    
    // --------------------------------------------------------------------------
    // --- ADMIN PANEL LOGIC ---
    // --------------------------------------------------------------------------
    
    private static void startAdminPanel(Scanner scanner, PublicChat pubChat, PrivateChat privChat, User admin) {
        while (true) {
            System.out.println("\n=== 👨‍💻 ADMIN PANEL ===");
            System.out.println("1. 💬 Access Public Chat (as Admin/Mod)");
            System.out.println("2. 🚩 See Reported Messages & Likes");
            System.out.println("3. 🎮 GAME MENU (Create & Launch)"); 
            System.out.println("U. Switch back to User Mode");
            System.out.print("Enter your choice: ");

            String choice = scanner.nextLine().trim().toLowerCase();

            if (choice.equals("u")) {
                System.out.println("Exiting Admin Panel. Returning to Main Menu.");
                break; 
            }

            if (choice.equals("1")) {
                startChat(scanner, pubChat, admin); 
            } else if (choice.equals("2")) {
                viewReportsAndLikes(pubChat); 
            } else if (choice.equals("3")) {
                gameMenu(scanner, pubChat); 
            } else {
                System.out.println("Invalid choice, try again.");
            }
        }
    }

    // --------------------------------------------------------------------------
    // --- GAME MENU LOGIC ---
    // --------------------------------------------------------------------------
    private static void gameMenu(Scanner scanner, Chat targetChat) {
        while(true) {
            System.out.println("\n--- 🎮 GAME MANAGEMENT ---");
            System.out.println("1. ✨ Create New Quiz");
            System.out.println("2. 🚀 Launch Existing Game");
            System.out.println("B. Back");
            System.out.print("Choice: ");
            String ch = scanner.nextLine().trim().toLowerCase();

            if (ch.equals("b")) break;

            if (ch.equals("1")) {
                // CREATE QUIZ
                System.out.print("Enter a command name for the game (e.g. 'math', 'movie'): ");
                String cmd = scanner.nextLine().trim();
                if(cmd.isEmpty()) continue;

                // Create a new quiz with the custom name
                QuizGame newQuiz = new QuizGame(cmd);
                
                System.out.println("Adding questions (type 'done' as question to finish):");
                while(true) {
                    System.out.print("Question: ");
                    String q = scanner.nextLine().trim();
                    if(q.equalsIgnoreCase("done")) break;
                    
                    System.out.print("Answer: ");
                    String a = scanner.nextLine().trim();
                    
                    newQuiz.addQuestion(q, a);
                    System.out.println("➕ Question added.");
                }
                
                targetChat.registerGame(newQuiz); // Add to chat
                System.out.println("✅ Game '!" + cmd + "' created and registered in " + targetChat.getName() + "!");
            } 
            else if (ch.equals("2")) {
                // LAUNCH GAME DIRECTLY
                System.out.print("Enter game command to launch (e.g. 'quiz', 'math'): ");
                String gameCmd = scanner.nextLine().trim();
                
                // Launch the game as Admin
                String result = targetChat.launchGame(new User("Admin", true, true), gameCmd);
                System.out.println(result);
                
                if (!result.startsWith("❌") && !result.startsWith("⛔")) {
                    // If launch successful, automatically jump user into chat to see it
                    startChat(scanner, targetChat, new User("Admin", true, true));
                }
            }
        }
    }
    
    private static void viewReportsAndLikes(Chat chat) {
        System.out.println("\n--- REPORTS AND LIKES in " + chat.getName() + " ---");
        boolean dataFound = false;
        
        for (Message message : chat.getMessages()) {
            if (message.getReports() > 0 || message.getLikes() > 0) {
                dataFound = true;
                
                String messageIdSnippet = String.valueOf(message.getTimestamp().hashCode() & 0xFFFF);
                String indicator = message.getReports() > 0 ? "❗REPORTED" : "   ";
                
                System.out.println(String.format(" %s [ID: %s] %s:", indicator, messageIdSnippet, message.getSender().getName()));
                System.out.println(String.format("   Content: '%s'", message.getContent()));
                System.out.println(String.format("   STATS: 👍 %d | 🚩 %d", message.getLikes(), message.getReports()));
                System.out.println("------------------------------------");
            }
        }
        
        if (!dataFound) {
            System.out.println("No messages with reports🚩 or likes👍 found.");
        }
    }

    private static void viewBettingStatus() {
        System.out.println("\n--- BETTING STATUS (Placeholder) ---");
        System.out.println("Currently, no betting data is available.");
        System.out.println("Coming soon...");
    }
    
    // --------------------------------------------------------------------------
    // --- MAIN CHAT LOOP ---
    // --------------------------------------------------------------------------
    private static void startChat(Scanner scanner, Chat currentChat, User currentUser) {
        System.out.println("\n🎉 You have entered the " + currentChat.getName() + "!");
        currentChat.listMessages(); 

        while (true) {
            // Prompt changes if game is active
            String prompt = (currentChat.getActiveGame() != null) 
                ? "\n[JEU ACTIF] Tapez votre réponse, ou '!" + currentChat.getActiveGame().getCommandName() + " exit': "
                : "\n[" + currentUser.getName() + "] Message, !watch, ou 'exit': ";
            
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("e")) {
                // If game is active, stop it before leaving
                if (currentChat.getActiveGame() != null) {
                    MiniGame activeGame = currentChat.getActiveGame();
                    String exitCommand = "!" + activeGame.getCommandName() + " exit";
                    String gameResponse = currentChat.processGameInput(currentUser, exitCommand);
                    if (gameResponse != null) {
                        sendBotResponse(currentChat, gameResponse);
                        currentChat.listMessages();
                    }
                }
                System.out.println("Exiting the chatroom...");
                break;
            }
            
            // --- VIDEO WATCH COMMAND --- 
            // To adapt to watch party Live video later
            if (input.equalsIgnoreCase("!watch")) {
                System.out.println("🍿 Attempting to launch video in default browser...");
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    try {
                        // Using the Sintel video URL a video that doesn't restrict because of copy write
                        Desktop.getDesktop().browse(new URI("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"));
                        System.out.println("✅ Browser opened! Enjoy the show.");
                    } catch (Exception e) {
                        System.out.println("❌ Error opening browser: " + e.getMessage());
                    }
                } else {
                    System.out.println("⚠️ Video launch not supported directly on this terminal.Coming soon but 👇🏻👇🏻👇🏻");
                    System.out.println("\n🔗 Please open this link manually: https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4");
                }
                currentChat.listMessages();
                continue;
            }
            
            // --- 1. HANDLE GAME INPUT (Priority) ---
            if (currentChat.getActiveGame() != null) {
                String gameResponse = currentChat.processGameInput(currentUser, input);
                if (gameResponse != null) {
                    sendBotResponse(currentChat, gameResponse);
                    currentChat.listMessages();
                    continue; // Skip normal flow
                }
                continue; 
            }

            // --- 2. HANDLE COMMANDS ---
            String[] parts = input.split("\\s+", 2);
            String actionOrMessage = parts[0].toLowerCase();
            String parameter = parts.length > 1 ? parts[1].trim() : "";

            if (actionOrMessage.equals("!jeu") || actionOrMessage.equals("jeu")|| actionOrMessage.equals("play")) {
                actionOrMessage = "play";
                parameter = "quiz"; 
            }
            
            if (actionOrMessage.equals("play")) {
                String gameCommand = parameter.split("\\s+")[0];
                String launchResponse = currentChat.launchGame(currentUser, gameCommand);
                sendBotResponse(currentChat, launchResponse);
                currentChat.listMessages();
                continue;
            }
            
            if (actionOrMessage.equals("l") || actionOrMessage.equals("z") || actionOrMessage.equals("r") || actionOrMessage.equals("d")) {
                String messageId = parameter;
                Message targetMessage = findMessageByShortId(currentChat, messageId);

                if (targetMessage == null && !actionOrMessage.equals("d")) {
                    System.out.println("Message ID '" + messageId + "' not found. Try again.");
                    continue;
                }

                if (actionOrMessage.equals("l")) {
                    targetMessage.like();
                    System.out.println("👍 You liked message ID: " + messageId);
                } else if (actionOrMessage.equals("z")) {
                    targetMessage.report();
                    System.out.println("🚩 You reported message ID: " + messageId);
                } else if (actionOrMessage.equals("r")) {
                    System.out.print("Enter your reply: ");
                    String replyContent = scanner.nextLine();
                    String timestamp = DATE_FORMAT.format(new Date());
                    Message replyMessage = new Message(currentUser, replyContent, timestamp);
                    replyMessage.setReplyTo(targetMessage); 
                    currentChat.sendMessage(currentUser, replyMessage.getContent()); 
                    currentChat.getMessages().get(currentChat.getMessages().size() - 1).setReplyTo(targetMessage);
                    System.out.println("💬 Replied.");
                } else if (actionOrMessage.equals("d")) {
                    if (currentUser.isAdmin() || currentUser.isModerator()) {
                        if (targetMessage != null) {
                            currentChat.removeMessage(targetMessage); 
                            System.out.println("✅ Message deleted.");
                        } else {
                             System.out.println("❌ ID invalid.");
                        }
                    } else {
                        System.out.println("⛔ Permission Denied.");
                    }
                }
                currentChat.listMessages(); 
            } else {
                // --- 3. STANDARD MESSAGE ---
                String fullMessageContent = input;
                currentChat.sendMessage(currentUser, fullMessageContent);

                if (fullMessageContent.trim().length() > 0) {
                    if (isGreeting(fullMessageContent)) sendBotResponse(currentChat, "Hi! I'm RB the bot.");
                    else if (isHowAreYou(fullMessageContent)) sendBotResponse(currentChat, "I'm doing great!");
                    else if (isWhatsUp(fullMessageContent)) sendBotResponse(currentChat, "The sky!");
                    else if (isThanks(fullMessageContent)) sendBotResponse(currentChat, "Happy to help!");
                }
                currentChat.listMessages(); 
            }
        }
    }

    private static Message findMessageByShortId(Chat chat, String shortId) {
        if (shortId.isEmpty()) return null;
        for (Message message : chat.getMessages()) {
            String messageIdSnippet = String.valueOf(message.getTimestamp().hashCode() & 0xFFFF);
            if (messageIdSnippet.equals(shortId)) {
                return message;
            }
        }
        return null;
    }

    private static void sendBotResponse(Chat chat, String response) {
        try {
            User botUser = new User("RB", false, false); 
            Thread.sleep(100);  
            chat.sendMessage(botUser, response); 
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static boolean isGreeting(String message) {
        return message.toLowerCase().matches(".*\\b(hi|hey|hello|morning|evening)\\b.*");
    }

    private static boolean isHowAreYou(String message) {
        return message.toLowerCase().contains("how are you") || message.toLowerCase().contains("how's it going");
    }

    private static boolean isWhatsUp(String message) {
        return message.toLowerCase().contains("what's up");
    }

    private static boolean isThanks(String message) {
        return message.toLowerCase().matches(".*\\b(thanks|thank you|thx)\\b.*");
    }

}
