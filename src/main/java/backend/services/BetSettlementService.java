package backend.services;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.ToIntFunction;

import org.springframework.stereotype.Service;

import backend.integration.lolesports.dto.Frame;
import backend.integration.lolesports.dto.GameEvent;
import backend.integration.lolesports.dto.ParticipantFrame;
import backend.integration.lolesports.dto.TeamFrame;
import backend.models.Bet;
import backend.models.DiscreteChoiceBet;
import backend.models.NumericValueBet;
import backend.models.OrderedRankingBet;
import backend.models.WatchParty;

@Service
public class BetSettlementService {

    public Optional<Object> findCorrectValue(Bet bet, Frame frame, WatchParty watchParty) {
        if (bet instanceof DiscreteChoiceBet discreteChoiceBet) {
            return findDiscreteChoiceValue(discreteChoiceBet, frame, watchParty).map(value -> (Object) value);
        }
        if (bet instanceof NumericValueBet numericValueBet) {
            return findNumericValue(numericValueBet, frame).map(value -> (Object) value);
        }
        if (bet instanceof OrderedRankingBet orderedRankingBet) {
            return findRankingValue(orderedRankingBet, frame).map(value -> (Object) value);
        }
        return Optional.empty();
    }

    private Optional<String> findDiscreteChoiceValue(DiscreteChoiceBet bet, Frame frame, WatchParty watchParty) {
        String normalizedQuestion = normalize(bet.getQuestion());

        if (containsAny(normalizedQuestion, "first blood", "premier kill")) {
            return findTeamForFirstEvent(frame, "KILL")
                    .flatMap(team -> matchChoiceToTeam(bet.getChoices(), team, watchParty));
        }
        if (containsAny(normalizedQuestion, "first dragon", "premier dragon")) {
            return findTeamForFirstEvent(frame, "DRAGON_KILL")
                    .flatMap(team -> matchChoiceToTeam(bet.getChoices(), team, watchParty));
        }
        if (containsAny(normalizedQuestion, "first tower", "premiere tour", "premier tower")) {
            return findTeamForFirstEvent(frame, "TOWER_DESTROYED")
                    .flatMap(team -> matchChoiceToTeam(bet.getChoices(), team, watchParty));
        }
        if (containsAny(normalizedQuestion, "winner", "gagnant", "qui gagne", "qui va gagner", "victoire")
                && isGameFinished(frame)) {
            return findWinningTeam(frame)
                    .flatMap(team -> matchChoiceToTeam(bet.getChoices(), team, watchParty));
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
        if (containsAny(normalizedQuestion, "duration", "duree", "durée", "temps de jeu")
                && isGameFinished(frame)) {
            return Optional.of(extractDurationMinutes(frame));
        }

        return Optional.empty();
    }

    private Optional<List<String>> findRankingValue(OrderedRankingBet bet, Frame frame) {
        if (!isGameFinished(frame)) {
            return Optional.empty();
        }

        String normalizedQuestion = normalize(bet.getQuestion());
        Comparator<ParticipantFrame> comparator = buildRankingComparator(normalizedQuestion);
        if (comparator == null) {
            return Optional.empty();
        }

        List<ParticipantFrame> participants = flattenParticipants(frame);
        if (participants.isEmpty()) {
            return Optional.empty();
        }

        Map<String, String> originalItemsByNormalized = new HashMap<>();
        for (String item : bet.getItems()) {
            originalItemsByNormalized.put(normalize(item), item);
        }

        List<ParticipantFrame> filteredParticipants = participants.stream()
                .filter(participant -> originalItemsByNormalized.containsKey(normalize(participant.championName())))
                .toList();
        if (filteredParticipants.size() != bet.getItems().size()) {
            return Optional.empty();
        }

        List<ParticipantFrame> sortedParticipants = new ArrayList<>(filteredParticipants);
        sortedParticipants.sort(comparator.thenComparing(participant -> normalize(participant.championName())));

        List<String> ranking = sortedParticipants.stream()
                .map(participant -> originalItemsByNormalized.get(normalize(participant.championName())))
                .toList();
        return Optional.of(ranking);
    }

    private Optional<String> findTeamForFirstEvent(Frame frame, String eventType) {
        return frame.events().stream()
                .filter(event -> isEventType(event, eventType))
                .min(Comparator.comparingLong(GameEvent::timestamp))
                .map(GameEvent::team);
    }

    private Optional<String> matchChoiceToTeam(List<String> choices, String team, WatchParty watchParty) {
        String normalizedTeam = normalize(team);
        String watchPartyTeam1 = extractWatchPartyTeam(watchParty, true);
        String watchPartyTeam2 = extractWatchPartyTeam(watchParty, false);

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
            if (isBlueTeam(normalizedTeam) && !watchPartyTeam1.isBlank() && normalizedChoice.equals(watchPartyTeam1)) {
                return Optional.of(choice);
            }
            if (isRedTeam(normalizedTeam) && !watchPartyTeam2.isBlank() && normalizedChoice.equals(watchPartyTeam2)) {
                return Optional.of(choice);
            }
        }
        return Optional.empty();
    }

    private Optional<String> findWinningTeam(Frame frame) {
        Optional<String> eventWinner = frame.events().stream()
                .filter(event -> isEventType(event, "GAME_END") && !normalize(event.team()).isBlank())
                .max(Comparator.comparingLong(GameEvent::timestamp))
                .map(GameEvent::team);
        if (eventWinner.isPresent()) {
            return eventWinner;
        }

        int blueGold = safeGold(frame.blueTeam());
        int redGold = safeGold(frame.redTeam());
        if (blueGold != redGold) {
            return Optional.of(blueGold > redGold ? "blue" : "red");
        }

        int blueKills = safeKills(frame.blueTeam());
        int redKills = safeKills(frame.redTeam());
        if (blueKills != redKills) {
            return Optional.of(blueKills > redKills ? "blue" : "red");
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

    private double extractDurationMinutes(Frame frame) {
        long durationMillis = frame.timestamp();
        for (GameEvent event : frame.events()) {
            durationMillis = Math.max(durationMillis, event.timestamp());
        }
        return durationMillis / 60000.0;
    }

    private List<ParticipantFrame> flattenParticipants(Frame frame) {
        List<ParticipantFrame> participants = new ArrayList<>();
        if (frame.blueTeam() != null) {
            participants.addAll(frame.blueTeam().participants());
        }
        if (frame.redTeam() != null) {
            participants.addAll(frame.redTeam().participants());
        }
        return participants.stream()
                .filter(participant -> participant != null && participant.championName() != null)
                .toList();
    }

    private Comparator<ParticipantFrame> buildRankingComparator(String normalizedQuestion) {
        if (containsAny(normalizedQuestion, "kills", "kill")) {
            return descendingComparator(ParticipantFrame::kills);
        }
        if (containsAny(normalizedQuestion, "assists", "assist")) {
            return descendingComparator(ParticipantFrame::assists);
        }
        if (containsAny(normalizedQuestion, "gold")) {
            return descendingComparator(ParticipantFrame::gold);
        }
        if (containsAny(normalizedQuestion, "level", "niveau")) {
            return descendingComparator(ParticipantFrame::level);
        }
        if (containsAny(normalizedQuestion, "deaths", "morts", "mort")) {
            if (containsAny(normalizedQuestion, "least", "fewest", "moins")) {
                return Comparator.comparingInt(ParticipantFrame::deaths);
            }
            return descendingComparator(ParticipantFrame::deaths);
        }
        return null;
    }

    private Comparator<ParticipantFrame> descendingComparator(ToIntFunction<ParticipantFrame> extractor) {
        return Comparator.comparingInt(extractor).reversed();
    }

    private String extractWatchPartyTeam(WatchParty watchParty, boolean firstTeam) {
        if (watchParty == null
                || watchParty.getAutoConfig() == null
                || watchParty.getAutoConfig().getCurrentMatch() == null) {
            return "";
        }
        String team = firstTeam
                ? watchParty.getAutoConfig().getCurrentMatch().getTeam1()
                : watchParty.getAutoConfig().getCurrentMatch().getTeam2();
        return normalize(team);
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
