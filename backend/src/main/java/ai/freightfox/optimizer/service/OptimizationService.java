package ai.freightfox.optimizer.service;

import ai.freightfox.optimizer.api.ApiModels;
import ai.freightfox.optimizer.domain.LaneOptimizer;
import ai.freightfox.optimizer.domain.OptimizationResult;
import ai.freightfox.optimizer.persistence.OptimizationRunEntity;
import ai.freightfox.optimizer.persistence.OptimizationRunRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class OptimizationService {
    private final JdbcTemplate jdbc;
    private final OptimizationRunRepository runs;
    private final LaneOptimizer optimizer = new LaneOptimizer();
    public OptimizationService(JdbcTemplate jdbc, OptimizationRunRepository runs) { this.jdbc=jdbc; this.runs=runs; }

    @Transactional
    public ApiModels.InputResponse saveInput(ApiModels.InputRequest request) {
        uniqueIds(request.lanes().stream().map(ApiModels.LaneInput::id).toList(), "lane");
        uniqueIds(request.transporters().stream().map(ApiModels.TransporterInput::id).toList(), "transporter");
        Set<String> laneIds=new HashSet<>(); request.lanes().forEach(l -> laneIds.add(l.id()));
        for(var carrier:request.transporters()) {
            Set<String> quoted=new HashSet<>();
            for(var quote:carrier.laneQuotes()) {
                if(!laneIds.contains(quote.laneId())) throw new IllegalArgumentException("Quote references unknown lane " + quote.laneId());
                if(!quoted.add(quote.laneId())) throw new IllegalArgumentException("Transporter " + carrier.id() + " has duplicate quotes for lane " + quote.laneId());
            }
        }
        UUID id=UUID.randomUUID();
        // Let PostgreSQL populate created_at via its DEFAULT now(); passing Instant
        // through JdbcTemplate.setObject is not supported by the PostgreSQL driver.
        jdbc.update("INSERT INTO optimization_run(id,max_transporters,total_cost,selected_transporters) VALUES (?,1,0,0)",id);
        for(var lane:request.lanes()) jdbc.update("INSERT INTO lane(run_id,external_id,origin,destination) VALUES (?,?,?,?)",id,lane.id(),lane.origin(),lane.destination());
        for(var carrier:request.transporters()) {
            jdbc.update("INSERT INTO transporter(run_id,external_id,name) VALUES (?,?,?)",id,carrier.id(),carrier.name());
            for(var quote:carrier.laneQuotes()) jdbc.update("INSERT INTO lane_quote(lane_id,transporter_id,amount) SELECT l.id,t.id,? FROM lane l,transporter t WHERE l.run_id=? AND l.external_id=? AND t.run_id=? AND t.external_id=?",quote.quote(),id,quote.laneId(),id,carrier.id());
        }
        return new ApiModels.InputResponse("success","Input data saved successfully.",id,request.lanes().size(),request.transporters().size());
    }

    @Transactional
    public ApiModels.AssignmentResponse assign(ApiModels.AssignmentRequest request) {
        OptimizationRunEntity run=runs.findFirstByOrderByCreatedAtDesc().orElseThrow(() -> new IllegalArgumentException("Submit lanes and transporter quotes before requesting an assignment"));
        UUID runId=run.getId();
        List<ApiModels.LaneInput> lanes=jdbc.query("SELECT external_id,origin,destination FROM lane WHERE run_id=? ORDER BY id",(rs,n)->new ApiModels.LaneInput(rs.getString(1),rs.getString(2),rs.getString(3)),runId);
        List<ApiModels.TransporterInput> carriers=jdbc.query("SELECT id,external_id,name FROM transporter WHERE run_id=? ORDER BY id",(rs,n)->new ApiModels.TransporterInput(rs.getString(2),rs.getString(3),List.of()),runId);
        List<ApiModels.TransporterInput> withQuotes=new ArrayList<>();
        for(var carrier:carriers) {
            List<ApiModels.QuoteInput> qs=jdbc.query("SELECT l.external_id,q.amount FROM lane_quote q JOIN lane l ON l.id=q.lane_id JOIN transporter t ON t.id=q.transporter_id WHERE t.run_id=? AND t.external_id=? ORDER BY l.id",(rs,n)->new ApiModels.QuoteInput(rs.getString(1),rs.getBigDecimal(2)),runId,carrier.id());
            withQuotes.add(new ApiModels.TransporterInput(carrier.id(),carrier.name(),qs));
        }
        OptimizationResult result=optimizer.optimize(new ApiModels.InputRequest(lanes,withQuotes),request.maxTransporters());
        jdbc.update("DELETE FROM lane_assignment WHERE run_id=?",runId);
        for(var a:result.assignments()) jdbc.update("INSERT INTO lane_assignment(run_id,lane_external_id,transporter_external_id,quote_amount) VALUES (?,?,?,?)",runId,a.laneId(),a.transporterId(),a.quote());
        jdbc.update("UPDATE optimization_run SET max_transporters=?,total_cost=?,selected_transporters=? WHERE id=?",request.maxTransporters(),result.totalCost(),result.transporterSummary().size(),runId);
        return new ApiModels.AssignmentResponse("success",runId,request.maxTransporters(),result.transporterSummary().size(),result.totalCost(),result.assignments(),result.transporterSummary(),Instant.now());
    }

    @Transactional(readOnly=true)
    public ApiModels.AssignmentResponse latest() {
        OptimizationRunEntity run=runs.findFirstByOrderByCreatedAtDesc().orElseThrow(() -> new IllegalArgumentException("No optimization run exists yet"));
        UUID id=run.getId();
        List<ApiModels.LaneAssignment> assignments=jdbc.query("SELECT a.lane_external_id,l.origin,l.destination,a.transporter_external_id,t.name,a.quote_amount FROM lane_assignment a JOIN lane l ON l.run_id=a.run_id AND l.external_id=a.lane_external_id JOIN transporter t ON t.run_id=a.run_id AND t.external_id=a.transporter_external_id WHERE a.run_id=? ORDER BY l.id",(rs,n)->new ApiModels.LaneAssignment(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getBigDecimal(6)),id);
        Map<String,List<ApiModels.LaneAssignment>> groups=new LinkedHashMap<>(); assignments.forEach(a->groups.computeIfAbsent(a.transporterId(),k->new ArrayList<>()).add(a));
        List<ApiModels.TransporterSummary> summaries=groups.entrySet().stream().map(e->new ApiModels.TransporterSummary(e.getKey(),e.getValue().getFirst().transporterName(),e.getValue().size(),e.getValue().stream().map(ApiModels.LaneAssignment::quote).reduce(BigDecimal.ZERO,BigDecimal::add))).toList();
        return new ApiModels.AssignmentResponse("success",id,run.getMaxTransporters(),summaries.size(),run.getTotalCost(),assignments,summaries,run.getCreatedAt());
    }
    private static void uniqueIds(List<String> ids,String type){if(new HashSet<>(ids).size()!=ids.size())throw new IllegalArgumentException("Duplicate " + type + " IDs are not allowed");}
}
