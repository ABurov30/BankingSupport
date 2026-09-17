package cache;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BaseCacheProperties {
  private boolean enabled;
  private Duration l1Ttl;
  private Duration l2Ttl;
  private int l1MaxSize;
}
