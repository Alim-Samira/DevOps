package backend.services.betresolution;

import java.util.Optional;

import backend.models.Bet;

public interface BetResolutionDelegate {

    boolean supports(Bet bet);

    default void recordObservation(Bet bet, BetResolutionContext context) {
        // Default no-op.
    }

    Optional<Object> findCorrectValue(Bet bet, BetResolutionContext context);
}
