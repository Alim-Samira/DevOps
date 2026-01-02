package backend. controllers;

import java.util.Map;

import org.springframework.web.bind.annotation. GetMapping;
import org.springframework.web. bind.annotation.PostMapping;
import org.springframework.web. bind.annotation.RequestBody;
import org. springframework.web.bind.annotation.RequestMapping;
import org. springframework.web.bind.annotation.RestController;

import backend. models.User;
import backend. services.QuizService;
import backend.services. UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas. annotations.tags.Tag;

@RestController
@RequestMapping("/api/quiz")
@Tag(name = "Quiz Mini-Game", description = "Play and manage quiz games")
public class QuizController {

    private final QuizService quizService;
    private final UserService userService;

    public QuizController(QuizService quizService, UserService userService) {
        this.quizService = quizService;
        this.userService = userService;
    }

    @Operation(summary = "Get quiz status", description = "Returns whether a quiz is active and its current state")
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return Map.of(
            "active", quizService. isActive(),
            "finished", quizService. isFinished(),
            "commandName", quizService. getCurrentQuiz().getCommandName()
        );
    }

    @Operation(summary = "Start the quiz", description = "Starts a new quiz game")
    @PostMapping("/start")
    public String startQuiz() {
        return quizService.startQuiz();
    }

    @Operation(summary = "Submit an answer", description = "Payload: { \"user\": \"alice\", \"answer\": \"Leonardo DiCaprio\" }")
    @PostMapping("/answer")
    public String submitAnswer(@RequestBody Map<String, String> payload) {
        String username = payload.get("user");
        String answer = payload.get("answer");

        if (username == null || answer == null) {
            return "Error: 'user' and 'answer' are required";
        }

        User user = userService.getUser(username);
        return quizService.submitAnswer(user, answer);
    }

    @Operation(summary = "Get quiz results", description = "Returns the current scores")
    @GetMapping("/results")
    public String getResults() {
        return quizService.getResults();
    }

    @Operation(summary = "Reset the quiz", description = "Resets the quiz to start fresh")
    @PostMapping("/reset")
    public String resetQuiz() {
        quizService.resetQuiz();
        return "Quiz has been reset! ";
    }

    @Operation(summary = "Add a custom question", description = "Payload: { \"question\": \"Your question?\", \"answer\":  \"The answer\" }")
    @PostMapping("/questions")
    public String addQuestion(@RequestBody Map<String, String> payload) {
        String question = payload.get("question");
        String answer = payload.get("answer");

        if (question == null || answer == null) {
            return "Error:  'question' and 'answer' are required";
        }

        quizService.addQuestion(question, answer);
        return "Question added successfully!";
    }

    @Operation(summary = "Create a custom quiz", description = "Payload: { \"name\": \"my-custom-quiz\" }")
    @PostMapping("/create")
    public String createCustomQuiz(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        if (name == null) {
            return "Error: 'name' is required";
        }
        quizService.createCustomQuiz(name);
        return "Custom quiz '" + name + "' created!  Add questions with POST /api/quiz/questions";
    }
}