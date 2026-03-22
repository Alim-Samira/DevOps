package backend.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import backend.integration.lolesports.dto.Frame;
import backend.models.Bet;
import backend.models.WatchParty;
import backend.services.betresolution.BetResolutionContext;
import backend.services.betresolution.BetResolutionDelegate;
import backend.services.betresolution.DiscreteChoiceBetResolutionDelegate;
import backend.services.betresolution.MilestoneRaceTracker;
import backend.services.betresolution.NumericValueBetResolutionDelegate;
import backend.services.betresolution.OrderedRankingBetResolutionDelegate;

@Service
public class BetSettlementService {

    private final List<BetResolutionDelegate> delegates;
    private final MilestoneRaceTracker milestoneRaceTracker;

    public BetSettlementService() {
        this.delegates = List.of(
                new DiscreteChoiceBetResolutionDelegate(),
                new NumericValueBetResolutionDelegate(),
                new OrderedRankingBetResolutionDelegate());
        this.milestoneRaceTracker = new MilestoneRaceTracker();
    }

    public void observe(Bet bet, Frame previousFrame, Frame currentFrame, WatchParty watchParty) {
        findDelegate(bet).ifPresent(delegate -> delegate.recordObservation(
                bet,
                new BetResolutionContext(watchParty, previousFrame, currentFrame, milestoneRaceTracker)));
    }

    public Optional<Object> findCorrectValue(Bet bet, Frame previousFrame, Frame currentFrame, WatchParty watchParty) {
        return findDelegate(bet)
                .flatMap(delegate -> delegate.findCorrectValue(
                        bet,
                        new BetResolutionContext(watchParty, previousFrame, currentFrame, milestoneRaceTracker)));
    }

    public Optional<Object> findCorrectValue(Bet bet, Frame frame, WatchParty watchParty) {
        return findCorrectValue(bet, null, frame, watchParty);
    }

    public void clear(Bet bet) {
        milestoneRaceTracker.clear(bet);
    }

    private Optional<BetResolutionDelegate> findDelegate(Bet bet) {
        return delegates.stream()
                .filter(delegate -> delegate.supports(bet))
                .findFirst();
    }
}
