import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.awt.Desktop; //  For opening browser
import java.net.URI;     // For defining the URL  

public class Main {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy");
    
    // Global lists
    private static List<Chat> chatRooms = new ArrayList<>();
    private static List<User> allUsers = new ArrayList<>();

    public static void main(String[] args) {
        // --- User Initialization ---
        User admin = new User("Admin", true, true);         
        User mod = new User("Moderator", false, true);      
        User user1 = new User("Alice", false, false);       
        User user2 = new User("RB", false, false); 

        // Add to "Database"
        allUsers.add(admin);
        allUsers.add(mod);
        allUsers.add(user1);
        allUsers.add(user2);

        // --- Default Chats ---
        PublicChat publicChat = new PublicChat("Public Chat", admin);
        publicChat.addUser(mod);
        publicChat.addUser(admin);
        publicChat.addUser(user1);
        publicChat.addUser(user2); 
        publicChat.addUser(new User("Bob", false, false));
        chatRooms.add(publicChat);
        
        PrivateChat privateChat = new PrivateChat("Private Chat", admin, 0); // Default cost 0
        privateChat.addMember(admin); 
        privateChat.addMember(user1); 
        chatRooms.add(privateChat);

        // Scanner setup
        Scanner scanner = new Scanner(System.in);
        User sessionUser = user1; // Alice starts the session
        
        int failedAttemptsCount = 0;
        final int MAX_ATTEMPTS = 3;

        while (true) {
            // --- MAIN MENU ---
            System.out.println("\n===== Main Menu (Logged in as: " + sessionUser.getName() + ") =====");
            System.out.println("Available Chatrooms:");
            
            for (int i = 0; i < chatRooms.size(); i++) {
                Chat chat = chatRooms.get(i);
                String type = (chat instanceof PrivateChat) ? "🔒" : "💬";
                System.out.println("  " + (i + 1) + ". " + type + " " + chat.getName());
            }
            
            System.out.println("----------------");
            System.out.println("C. Create Private Chat");
            System.out.println("A. Access Admin Panel");
            System.out.println("E. Exit");
            System.out.print("Enter your choice: ");

            String choice = scanner.nextLine().trim().toLowerCase();

            if (choice.equals("e")) { 
                System.out.println("Goodbye! 👋");
                break; 
            }

            // --- ADMIN PANEL LOGIN ---
            if (choice.equals("a")) {
                if (failedAttemptsCount >= MAX_ATTEMPTS) {
                    System.out.println("⛔ Access Denied. Too many failed attempts.");
                    continue;
                }
                
                System.out.print("Enter Admin Code: ");
                String code = scanner.nextLine().trim();
                
                if (code.equals("devops")) {
                    System.out.println("✅ Log in successful.");
                    failedAttemptsCount = 0;
                    startAdminPanel(scanner, publicChat, admin); 
                } else {
                    failedAttemptsCount++;
                    System.out.println("⛔ Denied.");
                }
                continue; 
            }

            // --- CREATE CHAT ---
            if (choice.equals("c")) {
                createPrivateChat(scanner, sessionUser);
                continue;
            }

            // --- JOIN CHAT ---
            try {
                int index = Integer.parseInt(choice) - 1;
                if (index >= 0 && index < chatRooms.size()) {
                    Chat selectedChat = chatRooms.get(index);
                    
                    if (selectedChat instanceof PrivateChat) {
                        PrivateChat pc = (PrivateChat) selectedChat;
                        if (pc.isAllowed(sessionUser)) {
                            startChat(scanner, selectedChat, sessionUser);
                        } else {
                        
                            if(pc.addMember(sessionUser)) {
                                startChat(scanner, selectedChat, sessionUser);
                            }
                        }
                    } else {
                        startChat(scanner, selectedChat, sessionUser);
                    }
                } else {
                    System.out.println("❌ Invalid number.");
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Invalid input.");
            }
        }
        scanner.close();
    }

    // CHAT CREATION LOGIC ---
    private static void createPrivateChat(Scanner scanner, User creator) {
        System.out.println("\n--- Create Your Room ---");
        System.out.print("Room Name: ");
        String name = scanner.nextLine().trim();
        if(name.isEmpty()) return;

        // No longer asking for points cost
        int cost = 0; 

        PrivateChat newChat = new PrivateChat(name, creator, cost);
        chatRooms.add(newChat);
        System.out.println("✅ Room created! You are the Admin.");
    }

    // ADMIN PANEL LOGIC ---
    private static void startAdminPanel(Scanner scanner, PublicChat pubChat, User admin) {
        while (true) {
            System.out.println("\n=== ADMIN PANEL ===");
            System.out.println("1. 💬 Access Public Chat (as Admin)");
            System.out.println("2. 🚩 See Reported Messages & Likes");
            System.out.println("3. 🎮 GAME MENU (Create & Launch)"); 
            System.out.println("U. Switch back to User Mode");
            System.out.print("Enter your choice: ");

            String choice = scanner.nextLine().trim().toLowerCase();

            if (choice.equals("u")) break; 

            if (choice.equals("1")) {
                startChat(scanner, pubChat, admin); 
            } else if (choice.equals("2")) {
                viewReportsAndLikes(pubChat); 
            } else if (choice.equals("3")) {
                gameMenu(scanner, pubChat); 
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
                System.out.println(String.format(" %s [ID: %s] %s: '%s'", indicator, messageIdSnippet, message.getSender().getName(), message.getContent()));
                System.out.println(String.format("   STATS: 👍 %d | 🚩 %d", message.getLikes(), message.getReports()));
            }
        }
        if (!dataFound) System.out.println("No messages with reports/likes found.");
    }

    // --- GAME MENU LOGIC ---
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
                System.out.print("Game command name (e.g. 'math'): ");
                String cmd = scanner.nextLine().trim();
                if(cmd.isEmpty()) continue;
                QuizGame newQuiz = new QuizGame(cmd);
                
                System.out.println("Adding questions (type 'done' to finish):");
                while(true) {
                    System.out.print("Question: ");
                    String q = scanner.nextLine().trim();
                    if(q.equalsIgnoreCase("done")) break;
                    System.out.print("Answer: ");
                    String a = scanner.nextLine().trim();
                    newQuiz.addQuestion(q, a);
                    System.out.println("➕ Added.");
                }
                targetChat.registerGame(newQuiz);
                System.out.println("✅ Game registered!");
            } 
            else if (ch.equals("2")) {
                List<MiniGame> available = targetChat.getAvailableGames();
                
                if (available.isEmpty()) {
                    System.out.println("❌ No games registered in this chat yet.");
                    continue;
                }

                System.out.println("\nSelect a game to launch:");
                for (int i = 0; i < available.size(); i++) {
                    System.out.println("   " + (i + 1) + ". " + available.get(i).getCommandName().toUpperCase());
                }
                
                System.out.print("Enter number: ");
                try {
                    int gameIdx = Integer.parseInt(scanner.nextLine().trim()) - 1;
                    if (gameIdx >= 0 && gameIdx < available.size()) {
                        String gameCmd = available.get(gameIdx).getCommandName();
                        String result = targetChat.launchGame(new User("Admin", true, true), gameCmd);
                        System.out.println(result);
                        if (!result.startsWith("❌") && !result.startsWith("⛔")) {
                            startChat(scanner, targetChat, new User("Admin", true, true));
                        }
                    } else {
                        System.out.println("❌ Invalid selection.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("❌ Invalid number.");
                }
            }
        }
    }

    // --- MAIN CHAT LOOP (modified becuase of errors)---
    private static void startChat(Scanner scanner, Chat currentChat, User currentUser) {
        System.out.println("\n🎉 Joined " + currentChat.getName());
        currentChat.listMessages();
        
        boolean isOwner = currentChat.getAdmin().equals(currentUser);
        if (isOwner) {
            System.out.println("👑 You are the owner. Cmds: '!add <name>', '!delete'");
        }

        while (true) {
            String prompt = (currentChat.getActiveGame() != null) 
                ? "\n[GAME ACTIVE] Your answer (or '!" + currentChat.getActiveGame().getCommandName() + " exit'): "
                : "\n[" + currentUser.getName() + "]: ";
            
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("e")) {
                if (currentChat.getActiveGame() != null) {
                     MiniGame ag = currentChat.getActiveGame();
                     currentChat.processGameInput(currentUser, "!" + ag.getCommandName() + " exit");
                }
                break;
            }

            // OWNER COMMANDS (delete chat, add member)
            if (isOwner && input.equalsIgnoreCase("!delete")) {
                System.out.print("⚠️ Delete this chat? (y/n): ");
                if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
                    chatRooms.remove(currentChat);
                    System.out.println("🗑️ Chat deleted.");
                    return;
                }
                continue;
            }
            if (isOwner && input.toLowerCase().startsWith("!add ")) {
                String targetName = input.substring(5).trim();
                User targetUser = findUserByName(targetName);
                if (targetUser != null && currentChat instanceof PrivateChat) {
                    ((PrivateChat) currentChat).addMember(targetUser);
                } else {
                    System.out.println("❌ User not found or Chat not Private.");
                }
                continue;
            }

            // WATCH COMMAND
            if (input.equalsIgnoreCase("!watch")) {
                launchVideo();
                currentChat.listMessages();
                continue;
            }
            
            // GAME INPUT 
            if (currentChat.getActiveGame() != null) {
                String gameResponse = currentChat.processGameInput(currentUser, input);
                if (gameResponse != null) {
                    sendBotResponse(currentChat, gameResponse);
                    currentChat.listMessages();
                }
                continue;
            }

            // MESSAGE ACTIONS (z for report , l for like, r for reply, d for delete)
            String[] parts = input.split("\\s+", 2);
            String cmd = parts[0].toLowerCase();
            String param = parts.length > 1 ? parts[1].trim() : "";

            if (cmd.equals("play") && !param.isEmpty()) {
                String res = currentChat.launchGame(currentUser, param.split(" ")[0]);
                sendBotResponse(currentChat, res);
                currentChat.listMessages();
                continue;
            }

            if (cmd.equals("l") || cmd.equals("z") || cmd.equals("r") || cmd.equals("d")) {
                Message target = findMessageByShortId(currentChat, param);
                if (target == null) {
                    System.out.println("❌ Msg ID not found.");
                    continue;
                }
                if (cmd.equals("l")) {
                    target.like();
                    System.out.println("👍 Liked.");
                } else if (cmd.equals("z")) {
                    target.report();
                    System.out.println("🚩 Reported.");
                } else if (cmd.equals("r")) {
                    System.out.print("Reply: ");
                    String reply = scanner.nextLine();
                    Message rMsg = new Message(currentUser, reply, DATE_FORMAT.format(new Date()));
                    rMsg.setReplyTo(target);
                    currentChat.sendMessage(currentUser, rMsg.getContent());
                    currentChat.getMessages().get(currentChat.getMessages().size()-1).setReplyTo(target);
                    System.out.println("💬 Replied.");
                } else if (cmd.equals("d")) {
                    currentChat.deleteMessage(currentUser, param); 
                }
                currentChat.listMessages();
            } else {
                // STANDARD MESSAGE
                if (!input.isEmpty()) {
                    currentChat.sendMessage(currentUser, input);
                    if (isGreeting(input)) sendBotResponse(currentChat, "Hi! I'm RB the bot.");
                    else if (isHowAreYou(input)) sendBotResponse(currentChat, "I'm doing great!");
                    else if (isWhatsUp(input)) sendBotResponse(currentChat, "The sky! lol just kidding, I'm good how are you?");
                    else if (isThanks(input)) sendBotResponse(currentChat, "Happy to help!");
                    
                    currentChat.listMessages();
                }
            }
        }
    }

    // --- HELPERS ---

    private static User findUserByName(String name) {
        for (User u : allUsers) {
            if (u.getName().equalsIgnoreCase(name)) return u;
        }
        return null;
    }

    private static Message findMessageByShortId(Chat chat, String shortId) {
        if (shortId.isEmpty()) return null;
        for (Message message : chat.getMessages()) {
            String snippet = String.valueOf(message.getTimestamp().hashCode() & 0xFFFF);
            if (snippet.equals(shortId)) return message;
        }
        return null;
    }

    private static void sendBotResponse(Chat chat, String response) {
        try {
            User bot = findUserByName("RB");
            if (bot == null) bot = new User("RB", false, false);
            Thread.sleep(100);  
            chat.sendMessage(bot, response); 
        } catch (InterruptedException e) { e.printStackTrace(); }
    }

    private static void launchVideo() {
        System.out.println("🍿 Launching video...");
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"));
                System.out.println("✅ Browser opened!");
            } catch (Exception e) { System.out.println("❌ Error: " + e.getMessage()); }
        } else {
            System.out.println("❌ Browser not supported.");
            System.out.println("🔗 Open manually: https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4");
        }
    }

    private static boolean isGreeting(String msg) { return msg.toLowerCase().matches(".*\\b(hi|hey|hello|morning|evening)\\b.*"); }
    private static boolean isHowAreYou(String msg) { return msg.toLowerCase().contains("how are you") || msg.toLowerCase().contains("how's it going"); }
    private static boolean isWhatsUp(String msg) { return msg.toLowerCase().contains("what's up"); }
    private static boolean isThanks(String msg) { return msg.toLowerCase().matches(".*\\b(thanks|thank you|thx)\\b.*"); }
}