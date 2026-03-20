package backend.services;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;

import backend.integration.lolesports.dto.Frame;
import backend.integration.lolesports.dto.GameEvent;
import backend.integration.lolesports.dto.TeamFrame;
import backend.models.Bet;
import backend.models.DiscreteChoiceBet;
import backend.models.NumericValueBet;

@Service
public class BetSettlementService {

    public Optional<Object> findCorrectValue(Bet bet, Frame frame) {
        if (bet instanceof DiscreteChoiceBet discreteChoiceBet) {
            return findDiscreteChoiceValue(discreteChoiceBet, frame).map(value -> (Object) value);
        }
        if (bet instanceof NumericValueBet numericValueBet) {
            return findNumericValue(numericValueBet, frame).map(value -> (Object) value);
        }
        return Optional.empty();
    }

    private Optional<String> findDiscreteChoiceValue(DiscreteChoiceBet bet, Frame frame) {
        String normalizedQuestion = normalize(bet.getQuestion());

        if (containsAny(normalizedQuestion, "first blood", "premier kill")) {
            return findTeamForFirstEvent(frame, "KILL")
                    .flatMap(team -> matchChoiceToTeam(bet.getChoices(), team));
        }
        if (containsAny(normalizedQuestion, "first dragon", "premier dragon")) {
            return findTeamForFirstEvent(frame, "DRAGON_KILL")
                    .flatMap(team -> matchChoiceToTeam(bet.getChoices(), team));
        }
        if (containsAny(normalizedQuestion, "first tower", "premiere tour", "premier tower")) {
            return findTeamForFirstEvent(frame, "TOWER_DESTROYED")
                    .flatMap(team -> matchChoiceToTeam(bet.getChoices(), team));
        }

        return Optional.empty();
    }

    private Optional<Double> findNumericValue(NumericValueBet bet, Frame frame) {
        String normalizedQuestion = normalize(bet.getQuestion());

        if (containsAny(normalizedQuestion, "total kills", "nombre de kills", "combien de kills")
                && isGameFinished(frame)) {
            int totalKills = safeKills(frame.blueTeam()) + safeKills(frame.redTeam());
            return Optional.of((double) totalKills);
        }

        if (containsAny(normalizedQuestion, "gold diff", "ecart de gold", "difference de gold")
                && isGameFinished(frame)) {
            int goldDiff = Math.abs(safeGold(frame.blueTeam()) - safeGold(frame.redTeam()));
            return Optional.of((double) goldDiff);
        }

        return Optional.empty();
    }

    private Optional<String> findTeamForFirstEvent(Frame frame, String eventType) {
        return frame.events().stream()
                .filter(event -> isEventType(event, eventType))
                .min(Comparator.comparingLong(GameEvent::timestamp))
                .map(GameEvent::team);
    }

    private Optional<String> matchChoiceToTeam(List<String> choices, String team) {
        String normalizedTeam = normalize(team);
        for (String choice : choices) {
            String normalizedChoice = normalize(choice);
            if (normalizedChoice.equals(normalizedTeam)) {
                return Optional.of(choice);
            }
            if (isBlueTeam(normalizedTeam) && isBlueChoice(normalizedChoice)) {
                return Optional.of(choice);
            }
            if (isRedTeam(normalizedTeam) && isRedChoice(normalizedChoice)) {
                return Optional.of(choice);
            }
        }
        return Optional.empty();
    }

    private boolean isGameFinished(Frame frame) {
        return frame.events().stream().anyMatch(event -> isEventType(event, "GAME_END"));
    }

    private boolean isEventType(GameEvent event, String expectedType) {
        return normalize(event.type()).equals(normalize(expectedType));
    }

    private boolean isBlueTeam(String normalizedValue) {
        return containsAny(normalizedValue, "blue", "bleu", "100");
    }

    private boolean isRedTeam(String normalizedValue) {
        return containsAny(normalizedValue, "red", "rouge", "200");
    }

    private boolean isBlueChoice(String normalizedChoice) {
        return containsAny(normalizedChoice, "blue", "bleu", "equipe bleue", "side blue");
    }

    private boolean isRedChoice(String normalizedChoice) {
        return containsAny(normalizedChoice, "red", "rouge", "equipe rouge", "side red");
    }

    private int safeKills(TeamFrame teamFrame) {
        return teamFrame == null ? 0 : teamFrame.totalKills();
    }

    private int safeGold(TeamFrame teamFrame) {
        return teamFrame == null ? 0 : teamFrame.totalGold();
    }

    private boolean containsAny(String source, String... values) {
        for (String value : values) {
            if (source.contains(normalize(value))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9]+", " ").trim();
    }
}
