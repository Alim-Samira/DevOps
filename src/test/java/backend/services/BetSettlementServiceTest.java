package backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import backend.integration.lolesports.dto.Frame;
import backend.integration.lolesports.dto.GameEvent;
import backend.integration.lolesports.dto.ParticipantFrame;
import backend.integration.lolesports.dto.TeamFrame;
import backend.models.AutoType;
import backend.models.DiscreteChoiceBet;
import backend.models.Match;
import backend.models.NumericValueBet;
import backend.models.OrderedRankingBet;
import backend.models.User;
import backend.models.WatchParty;

class BetSettlementServiceTest {

    private BetSettlementService settlementService;
    private User creator;
    private WatchParty autoWatchParty;

    @BeforeEach
    void setUp() {
        settlementService = new BetSettlementService();
        creator = new User("creator", false);
        autoWatchParty = WatchParty.createAutoWatchParty(creator, "T1", AutoType.TEAM);
        autoWatchParty.getAutoConfig().setCurrentMatch(
                new Match("match-1", "T1", "G2", LocalDateTime.now().minusMinutes(10), "MSI", "", "BO1"));
    }

    @Test
    void findCorrectValueShouldResolveFirstBloodUsingWatchPartyTeams() {
        DiscreteChoiceBet bet = new DiscreteChoiceBet(
                "First Blood ?",
                creator,
                autoWatchParty,
                LocalDateTime.now().plusMinutes(5),
                List.of("T1", "G2"));

        Frame frame = frame(
                180_000L,
                List.of(new GameEvent("KILL", "Aatrox", "Jinx", null, "blue", 95_000L)),
                blueTeam(2, 15000),
                redTeam(1, 14800));

        Optional<Object> correctValue = settlementService.findCorrectValue(bet, frame, autoWatchParty);

        assertTrue(correctValue.isPresent());
        assertEquals("T1", correctValue.get());
    }

    @Test
    void findCorrectValueShouldResolveWinnerOnGameEnd() {
        DiscreteChoiceBet bet = new DiscreteChoiceBet(
                "Qui gagne ?",
                creator,
                autoWatchParty,
                LocalDateTime.now().plusMinutes(5),
                List.of("T1", "G2"));

        Frame frame = frame(
                2_100_000L,
                List.of(new GameEvent("GAME_END", null, null, null, "red", 2_100_000L)),
                blueTeam(10, 62000),
                redTeam(14, 68000));

        Optional<Object> correctValue = settlementService.findCorrectValue(bet, frame, autoWatchParty);

        assertTrue(correctValue.isPresent());
        assertEquals("G2", correctValue.get());
    }

    @Test
    void findCorrectValueShouldResolveNumericTotalsAndDuration() {
        NumericValueBet killsBet = new NumericValueBet(
                "Combien de kills au total ?",
                creator,
                autoWatchParty,
                LocalDateTime.now().plusMinutes(5),
                true,
                0.0,
                100.0);

        NumericValueBet durationBet = new NumericValueBet(
                "Durée du match ?",
                creator,
                autoWatchParty,
                LocalDateTime.now().plusMinutes(5),
                true,
                0.0,
                60.0);

        Frame frame = frame(
                2_100_000L,
                List.of(new GameEvent("GAME_END", null, null, null, "blue", 2_100_000L)),
                blueTeam(9, 65000),
                redTeam(8, 61200));

        assertEquals(17.0, settlementService.findCorrectValue(killsBet, frame, autoWatchParty).orElseThrow());
        assertEquals(35.0, settlementService.findCorrectValue(durationBet, frame, autoWatchParty).orElseThrow());
    }

    @Test
    void findCorrectValueShouldResolveOrderedRankingFromChampionStats() {
        OrderedRankingBet bet = new OrderedRankingBet(
                "Classement kills des champions",
                creator,
                autoWatchParty,
                LocalDateTime.now().plusMinutes(5),
                List.of("Ahri", "Jinx", "Garen", "Leona"));

        Frame frame = frame(
                2_000_000L,
                List.of(new GameEvent("GAME_END", null, null, null, "blue", 2_000_000L)),
                new TeamFrame(65000, 15, List.of(
                        new ParticipantFrame("Ahri", 10, 2, 5, 15000, 18),
                        new ParticipantFrame("Jinx", 7, 1, 9, 14800, 17))),
                new TeamFrame(59000, 9, List.of(
                        new ParticipantFrame("Garen", 3, 4, 4, 12100, 15),
                        new ParticipantFrame("Leona", 1, 6, 12, 9000, 13))));

        Optional<Object> correctValue = settlementService.findCorrectValue(bet, frame, autoWatchParty);

        assertTrue(correctValue.isPresent());
        assertEquals(List.of("Ahri", "Jinx", "Garen", "Leona"), correctValue.get());
    }

    @Test
    void findCorrectValueShouldResolveFirstDragonFirstTowerAndGoldDiff() {
        DiscreteChoiceBet dragonBet = new DiscreteChoiceBet(
                "First Dragon ?",
                creator,
                autoWatchParty,
                LocalDateTime.now().plusMinutes(5),
                List.of("T1", "G2"));
        DiscreteChoiceBet towerBet = new DiscreteChoiceBet(
                "Premiere tour ?",
                creator,
                autoWatchParty,
                LocalDateTime.now().plusMinutes(5),
                List.of("Blue side", "Red side"));
        NumericValueBet goldDiffBet = new NumericValueBet(
                "Ecart de gold final ?",
                creator,
                autoWatchParty,
                LocalDateTime.now().plusMinutes(5),
                true,
                0.0,
                20000.0);

        Frame frame = frame(
                2_220_000L,
                List.of(
                        new GameEvent("DRAGON_KILL", null, null, null, "blue", 300_000L),
                        new GameEvent("TOWER_DESTROYED", null, null, null, "red", 450_000L),
                        new GameEvent("GAME_END", null, null, null, "blue", 2_220_000L)),
                blueTeam(14, 68000),
                redTeam(11, 63100));

        assertEquals("T1", settlementService.findCorrectValue(dragonBet, frame, autoWatchParty).orElseThrow());
        assertEquals("Red side", settlementService.findCorrectValue(towerBet, frame, autoWatchParty).orElseThrow());
        assertEquals(4900.0, settlementService.findCorrectValue(goldDiffBet, frame, autoWatchParty).orElseThrow());
    }

    @Test
    void findCorrectValueShouldFallbackToGoldLeadAndSupportLeastDeathsRanking() {
        DiscreteChoiceBet winnerBet = new DiscreteChoiceBet(
                "Winner ?",
                creator,
                autoWatchParty,
                LocalDateTime.now().plusMinutes(5),
                List.of("Blue side", "Red side"));
        OrderedRankingBet leastDeathsBet = new OrderedRankingBet(
                "Classement des champions avec le moins de morts",
                creator,
                autoWatchParty,
                LocalDateTime.now().plusMinutes(5),
                List.of("Ahri", "Jinx", "Garen", "Leona"));

        Frame frame = frame(
                1_950_000L,
                List.of(new GameEvent("GAME_END", null, null, null, null, 1_950_000L)),
                new TeamFrame(70000, 12, List.of(
                        new ParticipantFrame("Ahri", 9, 1, 7, 16000, 18),
                        new ParticipantFrame("Jinx", 6, 3, 8, 15000, 17))),
                new TeamFrame(64000, 12, List.of(
                        new ParticipantFrame("Garen", 5, 4, 3, 13000, 16),
                        new ParticipantFrame("Leona", 1, 6, 14, 9800, 14))));

        assertEquals("Blue side", settlementService.findCorrectValue(winnerBet, frame, autoWatchParty).orElseThrow());
        assertEquals(
                List.of("Ahri", "Jinx", "Garen", "Leona"),
                settlementService.findCorrectValue(leastDeathsBet, frame, autoWatchParty).orElseThrow());
    }

    private Frame frame(long timestamp, List<GameEvent> events, TeamFrame blueTeam, TeamFrame redTeam) {
        return new Frame(timestamp, blueTeam, redTeam, events);
    }

    private TeamFrame blueTeam(int kills, int gold) {
        return new TeamFrame(gold, kills, List.of(
                new ParticipantFrame("Ahri", 8, 2, 5, 14000, 16),
                new ParticipantFrame("Jinx", 4, 1, 7, 13000, 15)));
    }

    private TeamFrame redTeam(int kills, int gold) {
        return new TeamFrame(gold, kills, List.of(
                new ParticipantFrame("Garen", 3, 5, 4, 12000, 14),
                new ParticipantFrame("Leona", 1, 6, 8, 9000, 13)));
    }
}
