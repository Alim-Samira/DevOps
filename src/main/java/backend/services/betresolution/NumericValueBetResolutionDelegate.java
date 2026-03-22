package backend.services.betresolution;

import java.util.Optional;

import backend.integration.lolesports.dto.Frame;
import backend.models.Bet;
import backend.models.NumericValueBet;

public class NumericValueBetResolutionDelegate implements BetResolutionDelegate {

    @Override
    public boolean supports(Bet bet) {
        return bet instanceof NumericValueBet;
    }

    @Override
    public Optional<Object> findCorrectValue(Bet bet, BetResolutionContext context) {
        NumericValueBet numericValueBet = (NumericValueBet) bet;
        Frame frame = context.currentFrame();
        if (frame == null) {
            return Optional.empty();
        }

        String normalizedQuestion = BetResolutionSupport.normalize(numericValueBet.getQuestion());

        if (BetResolutionSupport.containsAny(normalizedQuestion, "total kills", "nombre de kills", "combien de kills")
                && BetResolutionSupport.isGameFinished(frame)) {
            int totalKills = BetResolutionSupport.safeKills(frame.blueTeam())
                    + BetResolutionSupport.safeKills(frame.redTeam());
            return Optional.of((Object) (double) totalKills);
        }

        if (BetResolutionSupport.containsAny(normalizedQuestion, "gold diff", "ecart de gold", "difference de gold")
                && BetResolutionSupport.isGameFinished(frame)) {
            int goldDiff = Math.abs(BetResolutionSupport.safeGold(frame.blueTeam())
                    - BetResolutionSupport.safeGold(frame.redTeam()));
            return Optional.of((Object) (double) goldDiff);
        }

        if (BetResolutionSupport.containsAny(normalizedQuestion, "duration", "duree", "durÃ©e", "temps de jeu")
                && BetResolutionSupport.isGameFinished(frame)) {
            return Optional.of((Object) BetResolutionSupport.extractDurationMinutes(frame));
        }

        return Optional.empty();
    }
}
