package ai.freightfox.optimizer.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="optimization_run")
public class OptimizationRunEntity {
    @Id private UUID id;
    @Column(name="max_transporters", nullable=false) private int maxTransporters;
    @Column(name="total_cost", nullable=false, precision=16, scale=2) private BigDecimal totalCost;
    @Column(name="selected_transporters", nullable=false) private int selectedTransporters;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    protected OptimizationRunEntity() {}
    public OptimizationRunEntity(UUID id,int maxTransporters,BigDecimal totalCost,int selectedTransporters,Instant createdAt){this.id=id;this.maxTransporters=maxTransporters;this.totalCost=totalCost;this.selectedTransporters=selectedTransporters;this.createdAt=createdAt;}
    public UUID getId(){return id;} public int getMaxTransporters(){return maxTransporters;} public BigDecimal getTotalCost(){return totalCost;} public int getSelectedTransporters(){return selectedTransporters;} public Instant getCreatedAt(){return createdAt;}
}
