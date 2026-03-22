package backend.services.betresolution;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.ToIntFunction;

import backend.integration.lolesports.dto.Frame;
import backend.integration.lolesports.dto.GameEvent;
import backend.integration.lolesports.dto.ParticipantFrame;
import backend.integration.lolesports.dto.TeamFrame;
import backend.models.WatchParty;

public final class BetResolutionSupport {

    private BetResolutionSupport() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9]+", " ").trim();
    }

    public static boolean containsAny(String source, String... values) {
        for (String value : values) {
            if (source.contains(normalize(value))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isGameFinished(Frame frame) {
        return frame != null && frame.events().stream().anyMatch(event -> isEventType(event, "GAME_END"));
    }

    public static boolean isEventType(GameEvent event, String expectedType) {
        return normalize(event.type()).equals(normalize(expectedType));
    }

    public static int safeKills(TeamFrame teamFrame) {
        return teamFrame == null ? 0 : teamFrame.totalKills();
    }

    public static int safeGold(TeamFrame teamFrame) {
        return teamFrame == null ? 0 : teamFrame.totalGold();
    }

    public static double extractDurationMinutes(Frame frame) {
        long durationMillis = frame.timestamp();
        for (GameEvent event : frame.events()) {
            durationMillis = Math.max(durationMillis, event.timestamp());
        }
        return durationMillis / 60000.0;
    }

    public static List<ParticipantFrame> flattenParticipants(Frame frame) {
        List<ParticipantFrame> participants = new ArrayList<>();
        if (frame == null) {
            return List.of();
        }
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

    public static Optional<String> findTeamForFirstEvent(Frame frame, String eventType) {
        return frame.events().stream()
                .filter(event -> isEventType(event, eventType))
                .min(Comparator.comparingLong(GameEvent::timestamp))
                .map(GameEvent::team);
    }

    public static Optional<String> findWinningTeam(Frame frame) {
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

    public static Optional<String> matchChoiceToTeam(List<String> choices, String team, WatchParty watchParty) {
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

    public static Comparator<ParticipantFrame> buildRankingComparator(String normalizedQuestion) {
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

    public static Optional<ParticipantFrame> findParticipantForChoice(Frame frame, String choice) {
        return findParticipantForChoice(flattenParticipants(frame), choice);
    }

    public static Optional<ParticipantFrame> findParticipantForChoice(List<ParticipantFrame> participants, String choice) {
        String normalizedChoice = normalize(choice);
        return participants.stream()
                .filter(participant -> participantMatchesChoice(participant, normalizedChoice))
                .findFirst();
    }

    public static boolean participantMatchesChoice(ParticipantFrame participant, String choice) {
        String normalizedChoice = normalize(choice);
        return normalize(participant.championName()).equals(normalizedChoice)
                || normalize(participant.summonerName()).equals(normalizedChoice)
                || normalize(participant.displayName()).equals(normalizedChoice);
    }

    public static String participantIdentifier(ParticipantFrame participant) {
        return normalize(participant.displayName());
    }

    private static Comparator<ParticipantFrame> descendingComparator(ToIntFunction<ParticipantFrame> extractor) {
        return Comparator.comparingInt(extractor).reversed();
    }

    private static String extractWatchPartyTeam(WatchParty watchParty, boolean firstTeam) {
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

    private static boolean isBlueTeam(String normalizedValue) {
        return containsAny(normalizedValue, "blue", "bleu", "100");
    }

    private static boolean isRedTeam(String normalizedValue) {
        return containsAny(normalizedValue, "red", "rouge", "200");
    }

    private static boolean isBlueChoice(String normalizedChoice) {
        return containsAny(normalizedChoice, "blue", "bleu", "equipe bleue", "side blue");
    }

    private static boolean isRedChoice(String normalizedChoice) {
        return containsAny(normalizedChoice, "red", "rouge", "equipe rouge", "side red");
    }
}
