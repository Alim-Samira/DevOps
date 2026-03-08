@Service
public class BetSettlementService {

    public boolean attemptResolve(Bet bet, Frame frame, WatchParty wp) {
        if (bet instanceof DiscreteChoiceBet dc) {
            return resolveDiscrete(dc, frame);
        }
        // idem pour NumericValueBet, OrderedRankingBet...
        return false;
    }

    private boolean resolveDiscrete(DiscreteChoiceBet bet, Frame frame) {
        String question = bet.getQuestion().toLowerCase();

        if (question.contains("first blood")) {
            for (GameEvent e : frame.events()) {
                if ("KILL".equals(e.type()) && e.timestamp() < 300_000) { // < 5min
                    bet.setWinningOption(e.team().equals("blue") ? "Équipe bleue" : "Équipe rouge");
                    return true;
                }
            }
        }
        // Ajoute ici First Dragon, First Tower, etc. selon tes besoins
        return false;
    }
}