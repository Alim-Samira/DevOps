package backend.services.betresolution;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import backend.integration.lolesports.dto.Frame;
import backend.integration.lolesports.dto.ParticipantFrame;
import backend.models.Bet;
import backend.models.OrderedRankingBet;

public class OrderedRankingBetResolutionDelegate implements BetResolutionDelegate {

    @Override
    public boolean supports(Bet bet) {
        return bet instanceof OrderedRankingBet;
    }

    @Override
    public Optional<Object> findCorrectValue(Bet bet, BetResolutionContext context) {
        OrderedRankingBet orderedRankingBet = (OrderedRankingBet) bet;
        Frame frame = context.currentFrame();
        if (frame == null || !BetResolutionSupport.isGameFinished(frame)) {
            return Optional.empty();
        }

        String normalizedQuestion = BetResolutionSupport.normalize(orderedRankingBet.getQuestion());
        Comparator<ParticipantFrame> comparator = BetResolutionSupport.buildRankingComparator(normalizedQuestion);
        if (comparator == null) {
            return Optional.empty();
        }

        List<ParticipantFrame> participants = BetResolutionSupport.flattenParticipants(frame);
        if (participants.isEmpty()) {
            return Optional.empty();
        }

        Map<ParticipantFrame, String> originalItemsByParticipant = new LinkedHashMap<>();
        for (String item : orderedRankingBet.getItems()) {
            Optional<ParticipantFrame> participant = BetResolutionSupport.findParticipantForChoice(participants, item);
            if (participant.isEmpty() || originalItemsByParticipant.containsKey(participant.get())) {
                return Optional.empty();
            }
            originalItemsByParticipant.put(participant.get(), item);
        }

        List<ParticipantFrame> sortedParticipants = new ArrayList<>(originalItemsByParticipant.keySet());
        sortedParticipants.sort(comparator.thenComparing(BetResolutionSupport::participantIdentifier));

        List<String> ranking = sortedParticipants.stream()
                .map(originalItemsByParticipant::get)
                .toList();
        return Optional.of((Object) ranking);
    }
}
