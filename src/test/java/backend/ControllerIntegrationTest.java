package backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = backend.DevOpsApplication.class)
class ControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    // ==================== RANKING CONTROLLER TESTS ====================

    @Test
    @DisplayName("GET /api/ranking should return ranking")
    void testGetRanking() throws Exception {
        mockMvc.perform(get("/api/ranking"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("GET /api/ranking/detailed should return detailed ranking")
    void testGetDetailedRanking() throws Exception {
        mockMvc.perform(get("/api/ranking/detailed"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    // ==================== QUIZ CONTROLLER TESTS ====================

    @Test
    @DisplayName("GET /api/quiz/status should return quiz status")
    void testGetQuizStatus() throws Exception {
        mockMvc.perform(get("/api/quiz/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").exists())
                .andExpect(jsonPath("$.finished").exists());
    }

    @Test
    @DisplayName("POST /api/quiz/start should start the quiz")
    void testStartQuiz() throws Exception {
        mockMvc.perform(post("/api/quiz/start"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/quiz/answer should accept answers")
    void testSubmitAnswer() throws Exception {
        // Start quiz first
        mockMvc.perform(post("/api/quiz/start"));

        mockMvc. perform(post("/api/quiz/answer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"user\": \"testuser\", \"answer\": \"test\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/quiz/reset should reset quiz")
    void testResetQuiz() throws Exception {
        mockMvc.perform(post("/api/quiz/reset"))
                .andExpect(status().isOk())
                .andExpect(content().string("Quiz has been reset! "));
    }

    @Test
    @DisplayName("POST /api/quiz/questions should add questions")
    void testAddQuestion() throws Exception {
        mockMvc. perform(post("/api/quiz/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\": \"Test question?\", \"answer\":  \"test answer\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Question added successfully!"));
    }

    // ==================== USER CONTROLLER TESTS ====================

    @Test
    @DisplayName("GET /api/users should return all users")
    void testGetAllUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("GET /api/users/{username} should return specific user")
    void testGetUser() throws Exception {
        mockMvc.perform(get("/api/users/alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("alice"));
    }

    // ==================== CHAT CONTROLLER TESTS ====================

    @Test
    @DisplayName("GET /api/chat should return chat history")
    void testGetChatHistory() throws Exception {
        mockMvc.perform(get("/api/chat"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType. APPLICATION_JSON));
    }

    @Test
    @DisplayName("POST /api/chat should send a message")
    void testSendMessage() throws Exception {
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"user\": \"testuser\", \"text\": \"Hello!\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Message sent"));
    }

    // ==================== WATCHPARTY CONTROLLER TESTS ====================

    @Test
    @DisplayName("GET /api/watchparties should return all watch parties")
    void testGetAllWatchParties() throws Exception {
        mockMvc.perform(get("/api/watchparties"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("POST /api/watchparties should create a watch party")
    void testCreateWatchParty() throws Exception {
        mockMvc.perform(post("/api/watchparties")
                .contentType(MediaType. APPLICATION_JSON)
                .content("{\"name\": \"TestParty\", \"type\": \"TEAM\"}"))
                .andExpect(status().isOk());
    }

    // ==================== BET CONTROLLER TESTS ====================

    @Test
    @DisplayName("GET /api/bets should return all bets")
    void testGetAllBets() throws Exception {
        mockMvc.perform(get("/api/bets"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("POST /api/bets/discrete should create a discrete choice bet")
    void testCreateDiscreteBet() throws Exception {
        // First create a watch party
        mockMvc.perform(post("/api/watchparties")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Test WP\", \"type\": \"TEAM\"}"))
                .andExpect(status().isOk());

        // Then create a bet for that watch party
        mockMvc.perform(post("/api/bets/discrete")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"watchParty\": \"Auto WP: Team Test WP\", \"admin\": \"AdminAPI\", \"question\": \"Who wins?\", \"choices\": [\"Team A\", \"Team B\"], \"votingMinutes\": 10}"))
                .andExpect(status().isOk());
    }
}