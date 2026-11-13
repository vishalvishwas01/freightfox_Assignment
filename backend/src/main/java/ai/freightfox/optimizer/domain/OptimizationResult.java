package ai.freightfox.optimizer.domain;

import ai.freightfox.optimizer.api.ApiModels;
import java.math.BigDecimal;
import java.util.List;

public record OptimizationResult(BigDecimal totalCost, List<ApiModels.LaneAssignment> assignments, List<ApiModels.TransporterSummary> transporterSummary) {}
