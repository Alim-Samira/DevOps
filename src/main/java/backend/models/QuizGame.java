package backend.models;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizGame implements MiniGame {
    private String commandName; // Changed from final constant to variable for better flexibility 
    private final Map<User, Integer> scores;
    private final List<QuizQuestion> questions;
    private int currentQuestionIndex;
    private boolean isActive;

    private static class QuizQuestion {
        String question;
        String answer;
        List<String> choices;

        public QuizQuestion(String question, String answer, String... choices) {
            this.question = question;
            this.answer = answer.toLowerCase();
            this.choices = Arrays.asList(choices);
        }
    }

    // Default constructor (The original quiz) 
    public QuizGame() {
        this("quiz"); // Default command name
        // Possibility to add default questions here
        this.questions.add(new QuizQuestion("Quel est l'acteur principal dans le film 'Inception' ?", "Leonardo DiCaprio"));
        this.questions.add(new QuizQuestion("En quelle année le confinement a t'il commencé ?", "2020"));
        this.questions.add(new QuizQuestion("Quelle est la capitale de l'Australie ?", "Canberra"));
    }

    //Constructor for Custom Games
    public QuizGame(String customCommandName) {
        this.commandName = customCommandName;
        this.scores = new HashMap<>();
        this.questions = new ArrayList<>();
        this.currentQuestionIndex = -1;
        this.isActive = false;
    }

    // Method to add questions 
    public void addQuestion(String question, String answer) {
        this.questions.add(new QuizQuestion(question, answer));
    }

    @Override
    public String getCommandName() {
        return commandName;
    }

    @Override
    public String start() {
        this.reset();
        if (questions.isEmpty()) {
            return "[WARNING] Ce quiz n'a pas de questions ! Ajoutez-en via le menu Admin.";
        }
        this.isActive = true;
        this.currentQuestionIndex = 0;
        return "[START] **MINI-JEU '" + commandName.toUpperCase() + "' DEMARRE !** [START]\n" +
               "Le premier à donner la bonne réponse marque un point !\n" +
               getQuestionMessage();
    }

    private String getQuestionMessage() {
        if (currentQuestionIndex < 0 || currentQuestionIndex >= questions.size()) {
            return "Fin du quiz. Tapez 'exit' pour voir les résultats.";
        }
        QuizQuestion q = questions.get(currentQuestionIndex);
        return String.format("[Q] Question %d/%d : %s\n(Tapez la reponse directement)", 
                             currentQuestionIndex + 1, questions.size(), q.question);
    }

    @Override
    public String processInput(User user, String input) {
        if (!isActive || isFinished()) return null;

        String lowerInput = input.trim().toLowerCase();
        QuizQuestion currentQuestion = questions.get(currentQuestionIndex); 

        if (lowerInput.contains(currentQuestion.answer)) {
            scores.put(user, scores.getOrDefault(user, 0) + 1);
            String response = String.format("✅ **BRAVO %s !** La réponse était bien '%s'. (Score: %d)\n", 
                                            user.getName(), currentQuestion.answer, scores.get(user));
            
            currentQuestionIndex++;
            
            if (isFinished()) {
                isActive = false;
                return response + getResults();
            } else {
                return response + "\n" + getQuestionMessage();
            }
        }
        return null; 
    }

    @Override
    public boolean isFinished() {
        return currentQuestionIndex >= questions.size();
    }

    @Override
    public String getResults() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n[RESULTS] --- RESULTATS: " + commandName.toUpperCase() + " ---\n");
        scores.entrySet().stream()
            .sorted(Map.Entry.<User, Integer>comparingByValue().reversed())
            .forEach(entry -> sb.append(String.format("[*] %s : %d pts\n", entry.getKey().getName(), entry.getValue())));
        sb.append("--------------------------\n");
        return sb.toString();
    }

    @Override
    public void reset() {
        this.scores.clear();
        this.currentQuestionIndex = -1;
        this.isActive = false;
    }
    
    public boolean isActive() { return isActive; }
}