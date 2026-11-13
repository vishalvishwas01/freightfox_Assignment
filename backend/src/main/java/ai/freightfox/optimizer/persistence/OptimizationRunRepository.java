package ai.freightfox.optimizer.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;
public interface OptimizationRunRepository extends JpaRepository<OptimizationRunEntity, UUID> {
    Optional<OptimizationRunEntity> findFirstByOrderByCreatedAtDesc();
}
