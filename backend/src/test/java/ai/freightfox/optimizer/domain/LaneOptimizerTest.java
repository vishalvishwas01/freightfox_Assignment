package ai.freightfox.optimizer.domain;

import ai.freightfox.optimizer.api.ApiModels;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LaneOptimizerTest {
    private final LaneOptimizer optimizer=new LaneOptimizer();
    private ApiModels.TransporterInput carrier(String id, Object... pairs){var q=new java.util.ArrayList<ApiModels.QuoteInput>();for(int i=0;i<pairs.length;i+=2)q.add(new ApiModels.QuoteInput((String)pairs[i],new BigDecimal(pairs[i+1].toString())));return new ApiModels.TransporterInput(id,id,q);}
    @Test void minimizesCostAndHonorsCarrierCap(){var lanes=List.of(new ApiModels.LaneInput("L1","A","B"),new ApiModels.LaneInput("L2","B","C"));var input=new ApiModels.InputRequest(lanes,List.of(carrier("T1","L1",10,"L2",100),carrier("T2","L1",12,"L2",11),carrier("T3","L1",8,"L2",30)));var result=optimizer.optimize(input,2);assertEquals(new BigDecimal("19"),result.totalCost());assertEquals(2,result.transporterSummary().size());assertEquals("T3",result.assignments().get(0).transporterId());}
    @Test void failsWhenLaneHasNoQuotes(){var input=new ApiModels.InputRequest(List.of(new ApiModels.LaneInput("L1","A","B")),List.of(carrier("T1","L2",4)));var e=assertThrows(IllegalArgumentException.class,()->optimizer.optimize(input,1));assertTrue(e.getMessage().contains("L1"));}
    @Test void rejectsZeroLimit(){var input=new ApiModels.InputRequest(List.of(new ApiModels.LaneInput("L1","A","B")),List.of(carrier("T1","L1",4)));assertThrows(IllegalArgumentException.class,()->optimizer.optimize(input,0));}
    @Test void failsWhenCarrierLimitCannotCoverAllLanes(){var input=new ApiModels.InputRequest(List.of(new ApiModels.LaneInput("L1","A","B"),new ApiModels.LaneInput("L2","B","C")),List.of(carrier("T1","L1",4),carrier("T2","L2",5)));var e=assertThrows(IllegalArgumentException.class,()->optimizer.optimize(input,1));assertTrue(e.getMessage().contains("No complete assignment"));}
}
