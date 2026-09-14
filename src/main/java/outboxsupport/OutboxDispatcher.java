package outboxsupport;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Bounded asynchronous dispatch. The sender must return promptly with the broker ack stage. */
public final class OutboxDispatcher<E extends OutboxEventEntity> {
  private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);
  private final OutboxAttemptStore<E> store;
  private final OutboxProperties properties;
  private final MeterRegistry meters;
  private final String name;
  private final AtomicInteger inFlight = new AtomicInteger();
  private final TransactionTemplate outsideTransaction;

  public OutboxDispatcher(
      OutboxAttemptStore<E> store,
      OutboxProperties properties,
      PlatformTransactionManager manager,
      MeterRegistry meters,
      String name) {
    this.store = Objects.requireNonNull(store);
    this.properties = Objects.requireNonNull(properties);
    this.meters = Objects.requireNonNull(meters);
    this.name = Objects.requireNonNull(name);
    outsideTransaction = new TransactionTemplate(manager);
    outsideTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
    meters.gauge(
        "outbox.in.flight",
        java.util.List.of(io.micrometer.core.instrument.Tag.of("outbox", name)),
        inFlight);
  }

  public synchronized void dispatch(Function<E, ? extends CompletionStage<?>> sender) {
    Objects.requireNonNull(sender);
    outsideTransaction.executeWithoutResult(
        status -> {
          meters.counter("outbox.recovered", "outbox", name).increment(store.recover());
          int free = properties.maxInFlight() - inFlight.get();
          if (free <= 0) {
            return;
          }
          var attempts = store.claim(Math.min(free, properties.batchSize()));
          inFlight.addAndGet(attempts.size());
          for (var attempt : attempts) {
            meters.counter("outbox.claimed", "outbox", name).increment();
            long start = System.nanoTime();
            try {
              Objects.requireNonNull(sender.apply(attempt.event()), "sender returned null")
                  .whenComplete((result, failure) -> finish(attempt, failure, start));
            } catch (Exception failure) {
              finish(attempt, failure, start);
            }
          }
        });
  }

  private void finish(OutboxAttempt<E> attempt, Throwable failure, long start) {
    try {
      boolean updated = store.complete(attempt, failure);
      String outcome = !updated ? "stale" : failure == null ? "published" : "failed";
      meters.counter("outbox.completed", "outbox", name, "outcome", outcome).increment();
    } catch (RuntimeException error) {
      meters.counter("outbox.callback.errors", "outbox", name).increment();
      log.error(
          "Outbox completion failed; lease recovery will retry event {}", attempt.eventId(), error);
    } finally {
      inFlight.decrementAndGet();
      meters
          .timer("outbox.ack.duration", "outbox", name)
          .record(System.nanoTime() - start, java.util.concurrent.TimeUnit.NANOSECONDS);
    }
  }
}
