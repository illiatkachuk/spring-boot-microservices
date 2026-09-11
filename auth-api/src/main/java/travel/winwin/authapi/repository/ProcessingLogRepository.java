package travel.winwin.authapi.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import travel.winwin.authapi.entity.ProcessingLog;

public interface ProcessingLogRepository extends JpaRepository<ProcessingLog, UUID> {
}
