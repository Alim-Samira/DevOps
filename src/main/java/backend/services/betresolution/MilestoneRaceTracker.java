package backend.services.betresolution;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import backend.models.Bet;

public class MilestoneRaceTracker {

    private final Map<Bet, String> winnersByBet = new ConcurrentHashMap<>();

    public Optional<String> getWinner(Bet bet) {
        return Optional.ofNullable(winnersByBet.get(bet));
    }

    public void recordWinner(Bet bet, String choice) {
        if (bet == null || choice == null || choice.isBlank()) {
            return;
        }
        winnersByBet.putIfAbsent(bet, choice);
    }

    public void clear(Bet bet) {
        if (bet != null) {
            winnersByBet.remove(bet);
        }
    }
}
