package backend. services;

import org.springframework.stereotype. Service;

import backend.models.QuizGame;
import backend. models.User;

@Service
public class QuizService {
    
    private QuizGame currentQuiz;

    public QuizService() {
        this.currentQuiz = new QuizGame(); // Default quiz with preset questions
    }

    public QuizGame getCurrentQuiz() {
        return currentQuiz;
    }

    public String startQuiz() {
        return currentQuiz.start();
    }

    public String submitAnswer(User user, String answer) {
        if (! currentQuiz.isActive()) {
            return "No quiz is currently active.  Start one first! ";
        }
        String result = currentQuiz.processInput(user, answer);
        return result != null ? result : "Wrong answer, try again!";
    }

    public String getResults() {
        return currentQuiz.getResults();
    }

    public boolean isActive() {
        return currentQuiz.isActive();
    }

    public boolean isFinished() {
        return currentQuiz. isFinished();
    }

    public void resetQuiz() {
        currentQuiz. reset();
    }

    public void addQuestion(String question, String answer) {
        currentQuiz. addQuestion(question, answer);
    }

    public QuizGame createCustomQuiz(String name) {
        this.currentQuiz = new QuizGame(name);
        return currentQuiz;
    }
}