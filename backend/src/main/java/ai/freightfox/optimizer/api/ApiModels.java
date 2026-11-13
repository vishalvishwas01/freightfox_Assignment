package ai.freightfox.optimizer.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ApiModels {
    private ApiModels() {}
    public record LaneInput(@NotBlank String id, @NotBlank String origin, @NotBlank String destination) {}
    public record QuoteInput(@NotBlank String laneId, @NotNull @DecimalMin("0.00") BigDecimal quote) {}
    public record TransporterInput(@NotBlank String id, @NotBlank String name, @NotEmpty List<@Valid QuoteInput> laneQuotes) {}
    public record InputRequest(@NotEmpty List<@Valid LaneInput> lanes, @NotEmpty List<@Valid TransporterInput> transporters) {}
    public record InputResponse(String status, String message, UUID runId, int lanes, int transporters) {}
    public record AssignmentRequest(@Min(1) int maxTransporters) {}
    public record LaneAssignment(String laneId, String origin, String destination, String transporterId, String transporterName, BigDecimal quote) {}
    public record TransporterSummary(String transporterId, String transporterName, int laneCount, BigDecimal totalCost) {}
    public record AssignmentResponse(String status, UUID runId, int maxTransporters, int transportersUsed, BigDecimal totalCost, List<LaneAssignment> assignments, List<TransporterSummary> transporterSummary, Instant createdAt) {}
    public record ErrorResponse(String error, String message, Instant timestamp) {}
}
