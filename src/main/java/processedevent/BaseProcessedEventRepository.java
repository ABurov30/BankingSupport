package processedevent;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseProcessedEventRepository<T extends ProcessedEvent>
    extends JpaRepository<T, UUID> {

  boolean existsByEventKey(String eventKey);
}
