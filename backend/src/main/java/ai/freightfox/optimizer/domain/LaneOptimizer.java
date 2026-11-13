package ai.freightfox.optimizer.domain;

import ai.freightfox.optimizer.api.ApiModels;
import java.math.BigDecimal;
import java.util.*;

/** Exact minimum-cost assignment with full lane coverage and an upper bound on distinct carriers. */
public class LaneOptimizer {
    public OptimizationResult optimize(ApiModels.InputRequest input, int maxTransporters) {
        if (maxTransporters < 1) throw new IllegalArgumentException("maxTransporters must be at least 1");
        List<ApiModels.LaneInput> lanes = input.lanes();
        List<ApiModels.TransporterInput> carriers = input.transporters();
        if (carriers.isEmpty()) throw new IllegalArgumentException("At least one transporter is required");
        Map<String, ApiModels.TransporterInput> byId = new LinkedHashMap<>();
        carriers.forEach(c -> byId.put(c.id(), c));
        for (var lane : lanes) {
            boolean covered = carriers.stream().flatMap(c -> c.laneQuotes().stream()).anyMatch(q -> q.laneId().equals(lane.id()));
            if (!covered) throw new IllegalArgumentException("Lane " + lane.id() + " has no transporter quote and cannot be covered");
        }
        if (carriers.size() > 18) throw new IllegalArgumentException("The exact optimizer supports up to 18 transporters per run");
        Search search = new Search(lanes, carriers, maxTransporters);
        BigDecimal cost = search.solve(0, 0L);
        if (cost == null) throw new IllegalArgumentException("No complete assignment is possible with maxTransporters=" + maxTransporters);
        List<ApiModels.LaneAssignment> assignments = new ArrayList<>();
        long mask = 0L;
        for (int i=0; i<lanes.size(); i++) {
            long stateMask = mask;
            int selected = search.choice.get(new State(i, stateMask));
            var lane=lanes.get(i); var carrier=carriers.get(selected);
            var quote=carrier.laneQuotes().stream().filter(q -> q.laneId().equals(lane.id())).findFirst().orElseThrow();
            assignments.add(new ApiModels.LaneAssignment(lane.id(),lane.origin(),lane.destination(),carrier.id(),carrier.name(),quote.quote()));
            mask |= 1L << selected;
        }
        return summarize(assignments);
    }

    private static OptimizationResult summarize(List<ApiModels.LaneAssignment> assignments) {
        BigDecimal total = assignments.stream().map(ApiModels.LaneAssignment::quote).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, List<ApiModels.LaneAssignment>> groups = new LinkedHashMap<>();
        assignments.forEach(a -> groups.computeIfAbsent(a.transporterId(), ignored -> new ArrayList<>()).add(a));
        List<ApiModels.TransporterSummary> summary = groups.entrySet().stream().map(e -> new ApiModels.TransporterSummary(
            e.getKey(), e.getValue().getFirst().transporterName(), e.getValue().size(),
            e.getValue().stream().map(ApiModels.LaneAssignment::quote).reduce(BigDecimal.ZERO, BigDecimal::add))).toList();
        return new OptimizationResult(total, List.copyOf(assignments), summary);
    }

    private record State(int laneIndex, long usedMask) {}
    private static final class Search {
        final List<ApiModels.LaneInput> lanes;
        final List<ApiModels.TransporterInput> carriers;
        final int limit;
        final Map<State,BigDecimal> memo = new HashMap<>();
        final Map<State,Integer> choice = new HashMap<>();
        Search(List<ApiModels.LaneInput> lanes, List<ApiModels.TransporterInput> carriers, int limit) { this.lanes=lanes; this.carriers=carriers; this.limit=limit; }
        BigDecimal solve(int i, long used) {
            if (i == lanes.size()) return BigDecimal.ZERO;
            State state = new State(i, used);
            if (memo.containsKey(state)) return memo.get(state);
            var lane=lanes.get(i);
            BigDecimal bestCost=null;
            for (var carrier: carriers) {
                var quote=carrier.laneQuotes().stream().filter(q -> q.laneId().equals(lane.id())).findFirst();
                if (quote.isEmpty()) continue;
                int index=carriers.indexOf(carrier);
                long bit=1L<<index;
                if ((used & bit)==0 && Long.bitCount(used)>=limit) continue;
                BigDecimal tail=solve(i+1,used|bit);
                if (tail==null) continue;
                BigDecimal candidate=quote.get().quote().add(tail);
                if(bestCost==null || candidate.compareTo(bestCost)<0){bestCost=candidate;choice.put(state,index);}
            }
            memo.put(state,bestCost);
            return bestCost;
        }
    }
}
