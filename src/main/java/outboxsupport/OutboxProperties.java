package outboxsupport;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Validated settings shared by service-owned dispatcher beans. */
@ConfigurationProperties("outbox")
public record OutboxProperties(
    @DefaultValue("5s") Duration polling,
    @DefaultValue("5s") Duration initialDelay,
    @DefaultValue("50") int batchSize,
    @DefaultValue("50") int maxInFlight,
    @DefaultValue("60s") Duration lease,
    @DefaultValue("5s") Duration retryDelay,
    @DefaultValue("5") int maxAttempts) {
  public OutboxProperties {
    positive(polling, "polling");
    positive(lease, "lease");
    positive(retryDelay, "retryDelay");
    if (initialDelay == null || initialDelay.isNegative()) {
      throw new IllegalArgumentException("initialDelay must be nonnegative");
    }
    if (batchSize < 1 || maxInFlight < 1 || maxAttempts < 1) {
      throw new IllegalArgumentException("batchSize, maxInFlight and maxAttempts must be positive");
    }
  }

  public static OutboxProperties defaults() {
    return new OutboxProperties(
        Duration.ofSeconds(5),
        Duration.ofSeconds(5),
        50,
        50,
        Duration.ofSeconds(60),
        Duration.ofSeconds(5),
        5);
  }

  private static void positive(Duration value, String name) {
    if (value == null || value.isNegative() || value.toMillis() < 1) {
      throw new IllegalArgumentException(name + " must be at least one millisecond");
    }
  }
}
