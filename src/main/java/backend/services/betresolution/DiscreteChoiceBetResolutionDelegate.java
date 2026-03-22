package backend.services.betresolution;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import backend.integration.lolesports.dto.Frame;
import backend.integration.lolesports.dto.ParticipantFrame;
import backend.models.Bet;
import backend.models.DiscreteChoiceBet;

public class DiscreteChoiceBetResolutionDelegate implements BetResolutionDelegate {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)");

    @Override
    public boolean supports(Bet bet) {
        return bet instanceof DiscreteChoiceBet;
    }

    @Override
    public void recordObservation(Bet bet, BetResolutionContext context) {
        DiscreteChoiceBet discreteChoiceBet = (DiscreteChoiceBet) bet;
        findMilestoneWinner(discreteChoiceBet, context)
                .ifPresent(choice -> context.milestoneRaceTracker().recordWinner(bet, choice));
    }

    @Override
    public Optional<Object> findCorrectValue(Bet bet, BetResolutionContext context) {
        recordObservation(bet, context);
        Optional<String> milestoneWinner = context.milestoneRaceTracker().getWinner(bet);
        if (milestoneWinner.isPresent()) {
            return milestoneWinner.map(value -> (Object) value);
        }

        DiscreteChoiceBet discreteChoiceBet = (DiscreteChoiceBet) bet;
        Frame frame = context.currentFrame();
        if (frame == null) {
            return Optional.empty();
        }

        String normalizedQuestion = BetResolutionSupport.normalize(discreteChoiceBet.getQuestion());

        if (BetResolutionSupport.containsAny(normalizedQuestion, "first blood", "premier kill")) {
            return BetResolutionSupport.findTeamForFirstEvent(frame, "KILL")
                    .flatMap(team -> BetResolutionSupport.matchChoiceToTeam(
                            discreteChoiceBet.getChoices(), team, context.watchParty()))
                    .map(value -> (Object) value);
        }
        if (BetResolutionSupport.containsAny(normalizedQuestion, "first dragon", "premier dragon")) {
            return BetResolutionSupport.findTeamForFirstEvent(frame, "DRAGON_KILL")
                    .flatMap(team -> BetResolutionSupport.matchChoiceToTeam(
                            discreteChoiceBet.getChoices(), team, context.watchParty()))
                    .map(value -> (Object) value);
        }
        if (BetResolutionSupport.containsAny(normalizedQuestion, "first tower", "premiere tour", "premier tower")) {
            return BetResolutionSupport.findTeamForFirstEvent(frame, "TOWER_DESTROYED")
                    .flatMap(team -> BetResolutionSupport.matchChoiceToTeam(
                            discreteChoiceBet.getChoices(), team, context.watchParty()))
                    .map(value -> (Object) value);
        }
        if (BetResolutionSupport.containsAny(normalizedQuestion, "winner", "gagnant", "qui gagne",
                "qui va gagner", "victoire")
                && BetResolutionSupport.isGameFinished(frame)) {
            return BetResolutionSupport.findWinningTeam(frame)
                    .flatMap(team -> BetResolutionSupport.matchChoiceToTeam(
                            discreteChoiceBet.getChoices(), team, context.watchParty()))
                    .map(value -> (Object) value);
        }

        return Optional.empty();
    }

    private Optional<String> findMilestoneWinner(DiscreteChoiceBet bet, BetResolutionContext context) {
        MilestoneRaceDefinition definition = parseMilestoneDefinition(
                BetResolutionSupport.normalize(bet.getQuestion())).orElse(null);
        if (definition == null || context.currentFrame() == null) {
            return Optional.empty();
        }

        List<MilestoneCandidate> candidates = new ArrayList<>();
        for (String choice : bet.getChoices()) {
            Optional<ParticipantFrame> currentParticipant = BetResolutionSupport.findParticipantForChoice(
                    context.currentFrame(), choice);
            if (currentParticipant.isEmpty()) {
                continue;
            }

            ParticipantFrame current = currentParticipant.get();
            int currentValue = definition.metric().extract(current);
            if (currentValue < definition.threshold()) {
                continue;
            }

            int previousValue = 0;
            if (context.previousFrame() != null) {
                previousValue = BetResolutionSupport.findParticipantForChoice(context.previousFrame(), choice)
                        .map(definition.metric()::extract)
                        .orElse(0);
                if (previousValue >= definition.threshold()) {
                    continue;
                }
            }

            candidates.add(new MilestoneCandidate(
                    choice,
                    currentValue,
                    Math.max(0, currentValue - previousValue),
                    current.gold(),
                    current.level()));
        }

        return candidates.stream()
                .sorted(MilestoneCandidate.COMPARATOR)
                .map(MilestoneCandidate::choice)
                .findFirst();
    }

    private Optional<MilestoneRaceDefinition> parseMilestoneDefinition(String normalizedQuestion) {
        if (!BetResolutionSupport.containsAny(normalizedQuestion,
                "first to",
                "reach first",
                "reaches first",
                "atteindra en premier",
                "atteindra le plus rapidement",
                "atteindra d abord",
                "atteindra d abord le palier",
                "atteindra premier",
                "plus rapidement le palier")) {
            return Optional.empty();
        }

        int threshold = extractThreshold(normalizedQuestion).orElse(-1);
        if (threshold < 0) {
            return Optional.empty();
        }

        for (ParticipantMetric metric : ParticipantMetric.values()) {
            if (metric.matches(normalizedQuestion)) {
                return Optional.of(new MilestoneRaceDefinition(metric, threshold));
            }
        }
        return Optional.empty();
    }

    private Optional<Integer> extractThreshold(String normalizedQuestion) {
        Matcher matcher = NUMBER_PATTERN.matcher(normalizedQuestion);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String raw = matcher.group(1).replace(',', '.');
        return Optional.of((int) Math.round(Double.parseDouble(raw)));
    }

    private record MilestoneRaceDefinition(ParticipantMetric metric, int threshold) {
    }

    private record MilestoneCandidate(String choice, int currentValue, int delta, int gold, int level) {
        private static final Comparator<MilestoneCandidate> COMPARATOR = Comparator
                .comparingInt(MilestoneCandidate::currentValue).reversed()
                .thenComparing(Comparator.comparingInt(MilestoneCandidate::delta).reversed())
                .thenComparing(Comparator.comparingInt(MilestoneCandidate::gold).reversed())
                .thenComparing(Comparator.comparingInt(MilestoneCandidate::level).reversed())
                .thenComparing(candidate -> BetResolutionSupport.normalize(candidate.choice()));
    }

    private enum ParticipantMetric {
        KILLS(ParticipantFrame::kills, "kills", "kill"),
        ASSISTS(ParticipantFrame::assists, "assists", "assist"),
        GOLD(ParticipantFrame::gold, "gold"),
        LEVEL(ParticipantFrame::level, "level", "niveau"),
        DEATHS(ParticipantFrame::deaths, "deaths", "death", "morts", "mort");

        private final ToIntFunction<ParticipantFrame> extractor;
        private final String[] keywords;

        ParticipantMetric(ToIntFunction<ParticipantFrame> extractor, String... keywords) {
            this.extractor = extractor;
            this.keywords = keywords;
        }

        int extract(ParticipantFrame participantFrame) {
            return extractor.applyAsInt(participantFrame);
        }

        boolean matches(String normalizedQuestion) {
            return BetResolutionSupport.containsAny(normalizedQuestion, keywords);
        }
    }
}
