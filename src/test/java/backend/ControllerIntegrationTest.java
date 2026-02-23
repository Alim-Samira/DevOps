package backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

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
    @DisplayName("GET /api/rankings/public/points should return ranking")
    void testGetPublicPointsRanking() throws Exception {
        mockMvc.perform(get("/api/rankings/public/points"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("GET /api/rankings/public/wins should return ranking")
    void testGetPublicWinsRanking() throws Exception {
        mockMvc.perform(get("/api/rankings/public/wins"))
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
                .content("{\"user\": \"alice\", \"name\": \"TestParty\", \"type\": \"TEAM\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/watchparties/{name}/join should add participant and initialize 200 WP points")
    void testJoinWatchPartyInitializesPoints() throws Exception {
        // create a public manual watchparty
        mockMvc.perform(post("/api/watchparties/public")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"JoinablePublicWP\", \"game\": \"LoL\"}"))
                .andExpect(status().isOk());

        // user bob joins
        mockMvc.perform(post("/api/watchparties/JoinablePublicWP/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"user\": \"bob\"}"))
                .andExpect(status().isOk());

        // bob should have 200 points for the WP
        mockMvc.perform(get("/api/users/bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsByWatchParty.JoinablePublicWP").value(200));
    }

    @Test
    @DisplayName("Global ranking = sum of points in public WPs where user participates")
    void testGlobalRankingSummedFromPublicWPs() throws Exception {
        // Create two public WPs and have 'charlie' join both
        mockMvc.perform(post("/api/watchparties/public")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"PublicA\", \"game\": \"LoL\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/watchparties/public")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"PublicB\", \"game\": \"LoL\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/watchparties/PublicA/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"user\": \"charlie\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/watchparties/PublicB/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"user\": \"charlie\"}"))
                .andExpect(status().isOk());

        // Each join gives 200 WP points -> global ranking for charlie should be 400
        mockMvc.perform(get("/api/rankings/public/points?refresh=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.charlie").value(400));

        // and the user's own globalPoints should reflect the same
        mockMvc.perform(get("/api/users/charlie"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.globalPoints").value(400));
    }

    @Test
    @DisplayName("Manual private watchparty with creator initializes creator's WP points")
    void testManualPrivateWatchPartyInitializesCreatorPoints() throws Exception {
        mockMvc.perform(post("/api/watchparties/private")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"ManualPrivateWP\", \"game\": \"LoL\", \"user\": \"creatorBob\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/creatorBob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsByWatchParty.ManualPrivateWP").value(200));
    }

    @Test
    @DisplayName("Manual public watchparty with creator initializes creator's WP points (public WP)")
    void testManualPublicWatchPartyInitializesCreatorPublicPoints() throws Exception {
        mockMvc.perform(post("/api/watchparties/public")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"ManualPublicWP\", \"game\": \"LoL\", \"user\": \"creatorCarol\"}"))
                .andExpect(status().isOk());

        // creatorCarol's WP-specific points for ManualPublicWP must be initialized to 200
        mockMvc.perform(get("/api/users/creatorCarol"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsByWatchParty.ManualPublicWP").value(200))
                .andExpect(jsonPath("$.globalPoints").value(200));
    }

    @Test
    @DisplayName("WatchParty admin vs global admin behavior")
    void testCreatorIsWpAdminButNotGlobalAdmin() throws Exception {
        // 1) Auto WP created by 'alice' (creator != null)
        mockMvc.perform(post("/api/watchparties")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"user\": \"alice\", \"name\": \"CreatorWP\", \"type\": \"TEAM\"}"))
                .andExpect(status().isOk());

        // Creator 'alice' can create a bet on her WP (WP-scoped admin)
        mockMvc.perform(post("/api/watchparties/Auto WP: Team CreatorWP/bets/discrete")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"admin\": \"alice\", \"question\": \"Is creator admin?\", \"choices\": [\"Yes\", \"No\"], \"votingMinutes\": 5}"))
                .andExpect(status().isOk());

        // Global admin 'admin' must NOT be allowed to create a bet when WP has a creator
        mockMvc.perform(post("/api/watchparties/Auto WP: Team CreatorWP/bets/discrete")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"admin\": \"admin\", \"question\": \"Global admin attempt\", \"choices\": [\"A\", \"B\"], \"votingMinutes\": 5}"))
                .andExpect(status().isOk())
                .andExpect(content().string("❌ Seuls les admins peuvent créer des paris"));

        // Ensure 'alice' was NOT promoted to global admin
        mockMvc.perform(get("/api/users/alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admin").value(false));

        // 2) Create a manual (public) watchparty -> creator == null
        mockMvc.perform(post("/api/watchparties/public")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"ManualWP\", \"game\": \"League of Legends\"}"))
                .andExpect(status().isOk());

        // On manual WP a global admin may create bets
        mockMvc.perform(post("/api/watchparties/ManualWP/bets/discrete")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"admin\": \"admin\", \"question\": \"Admin on manual WP\", \"choices\": [\"A\", \"B\"], \"votingMinutes\": 5}"))
                .andExpect(status().isOk());

        // Non-admin (alice) cannot create bets on manual WP
        mockMvc.perform(post("/api/watchparties/ManualWP/bets/discrete")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"admin\": \"alice\", \"question\": \"Alice on manual WP\", \"choices\": [\"A\", \"B\"], \"votingMinutes\": 5}"))
                .andExpect(status().isOk())
                .andExpect(content().string("❌ Seuls les admins peuvent créer des paris"));
    }

    // ==================== BET CONTROLLER TESTS ====================

    @Test
    @DisplayName("GET /api/bets/all should return all bets")
    void testGetAllBets() throws Exception {
        mockMvc.perform(get("/api/bets/all"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("POST /api/watchparties/{name}/bets/discrete should create a discrete choice bet")
    void testCreateDiscreteBet() throws Exception {
        // First create a watch party
        mockMvc.perform(post("/api/watchparties")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"user\": \"AdminAPI\", \"name\": \"TestWP\", \"type\": \"TEAM\"}"))
                .andExpect(status().isOk());

        // Then create a bet for that watch party
        mockMvc.perform(post("/api/watchparties/Auto WP: Team TestWP/bets/discrete")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"admin\": \"AdminAPI\", \"question\": \"Who wins?\", \"choices\": [\"Team A\", \"Team B\"], \"votingMinutes\": 10}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Only WatchParty admin can end voting on a bet (close voting phase)")
    void testOnlyAdminCanEndVoting() throws Exception {
        // Create a public WP (no creator, so global admin can manage)
        mockMvc.perform(post("/api/watchparties/public")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"PublicBetWP\", \"game\": \"LoL\"}"))
                .andExpect(status().isOk());

        // Admin creates a discrete bet
        mockMvc.perform(post("/api/watchparties/PublicBetWP/bets/discrete")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"admin\": \"admin\", \"question\": \"Winner?\", \"choices\": [\"A\", \"B\"], \"votingMinutes\": 10}"))
                .andExpect(status().isOk());

        // Non-admin tries to close voting -> fails
        mockMvc.perform(post("/api/watchparties/PublicBetWP/bets/end-voting")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"admin\": \"bob\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("❌ Seuls le créateur de la watchparty ou les admins globaux peuvent fermer le vote"));

        // Admin closes voting -> succeeds
        mockMvc.perform(post("/api/watchparties/PublicBetWP/bets/end-voting")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"admin\": \"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("✅ Phase de vote terminée, en attente du résultat"));

        // Verify bet is now in PENDING state
        mockMvc.perform(get("/api/watchparties/PublicBetWP/bets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PENDING"));
    }

    @Test
    @DisplayName("WP creator can close voting on their own WP (WP-scoped admin)")
    void testWpCreatorCanEndVotingOnOwnWp() throws Exception {
        // Create a manual public WP with 'dave' as creator
        mockMvc.perform(post("/api/watchparties/public")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"CreatorBetWP\", \"game\": \"LoL\", \"user\": \"dave\"}"))
                .andExpect(status().isOk());

        // Creator 'dave' creates a bet on their WP
        mockMvc.perform(post("/api/watchparties/CreatorBetWP/bets/discrete")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"admin\": \"dave\", \"question\": \"Winner?\", \"choices\": [\"X\", \"Y\"], \"votingMinutes\": 10}"))
                .andExpect(status().isOk());

        // Creator 'dave' closes voting -> succeeds (is WP admin)
        mockMvc.perform(post("/api/watchparties/CreatorBetWP/bets/end-voting")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"admin\": \"dave\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("✅ Phase de vote terminée, en attente du résultat"));

        // Verify bet is now in PENDING state
        mockMvc.perform(get("/api/watchparties/CreatorBetWP/bets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PENDING"));
    }

    // ==================== CALENDAR CONTROLLER TESTS ====================

    @Test
    @DisplayName("GET /api/calendars/providers should return supported providers")
    void testGetCalendarProviders() throws Exception {
        mockMvc.perform(get("/api/calendars/providers"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].provider").exists());
    }

    @Test
    @DisplayName("POST /api/users/{user}/calendars should connect ICAL calendar")
    void testConnectIcsCalendar() throws Exception {
        mockMvc.perform(post("/api/users/alice/calendars")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"provider\":\"ICAL\",\"label\":\"Mon agenda\",\"sourceUrl\":\"https://example.com/my.ics\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.connection.type").value("ICAL"));

        mockMvc.perform(get("/api/users/alice/calendars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("ICAL"));
    }

    @Test
    @DisplayName("POST /api/users/{user}/calendars should reject invalid payload")
    void testConnectCalendarInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/users/bob/calendars")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"provider\":\"ICAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("DELETE /api/users/{user}/calendars/{connectionId} should remove connection")
    void testDeleteCalendarConnection() throws Exception {
        String response = mockMvc.perform(post("/api/users/carol/calendars")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"provider\":\"ICAL\",\"sourceUrl\":\"https://example.com/carol.ics\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String connectionId = response.split("\\\"id\\\":\\\"")[1].split("\\\"")[0];

        mockMvc.perform(delete("/api/users/carol/calendars/" + connectionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

}