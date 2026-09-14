package outboxsupport;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** PostgreSQL attempt storage; table names must be trusted service configuration. */
public final class JpaOutboxAttemptStore<E extends OutboxEventEntity>
    implements OutboxAttemptStore<E> {
  private final EntityManager entityManager;
  private final Class<E> entityType;
  private final String table;
  private final OutboxProperties properties;
  private final TransactionTemplate transaction;

  public JpaOutboxAttemptStore(
      EntityManager entityManager,
      PlatformTransactionManager manager,
      Class<E> entityType,
      String table,
      OutboxProperties properties) {
    if (table == null || !table.matches("[a-z_][a-z0-9_]*(\\.[a-z_][a-z0-9_]*)?")) {
      throw new IllegalArgumentException("Invalid outbox table name");
    }
    this.entityManager = Objects.requireNonNull(entityManager);
    this.entityType = Objects.requireNonNull(entityType);
    this.table = table;
    this.properties = Objects.requireNonNull(properties);
    transaction = new TransactionTemplate(manager);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @Override
  public List<OutboxAttempt<E>> claim(int limit) {
    if (limit < 1) {
      return List.of();
    }
    return transaction.execute(
        status -> {
          List<?> rows =
              entityManager
                  .createNativeQuery(
                      "SELECT * FROM "
                          + table
                          + " WHERE status = 'PENDING' AND retry_count < :max"
                          + " AND (next_retry_at IS NULL OR next_retry_at <= CURRENT_TIMESTAMP)"
                          + " ORDER BY created_at, id LIMIT :limit FOR UPDATE SKIP LOCKED",
                      entityType)
                  .setParameter("max", properties.maxAttempts())
                  .setParameter("limit", Math.min(limit, properties.batchSize()))
                  .getResultList();
          List<OutboxAttempt<E>> attempts = new ArrayList<>();
          for (Object row : rows) {
            E event = entityType.cast(row);
            String token = UUID.randomUUID().toString();
            entityManager
                .createNativeQuery(
                    "UPDATE "
                        + table
                        + " SET status = 'PROCESSING', retry_count = retry_count + 1,"
                        + " locked_by = :token, locked_at = CURRENT_TIMESTAMP, next_retry_at = NULL"
                        + " WHERE id = :id")
                .setParameter("token", token)
                .setParameter("id", event.getId())
                .executeUpdate();
            entityManager.refresh(event);
            entityManager.detach(event);
            attempts.add(new OutboxAttempt<>(event.getId(), token, event));
          }
          return attempts;
        });
  }

  @Override
  public int recover() {
    return transaction.execute(
        status ->
            entityManager
                .createNativeQuery(
                    "UPDATE "
                        + table
                        + " SET status = CASE WHEN retry_count >= :max"
                        + " THEN 'FAILED' ELSE 'PENDING' END,"
                        + " locked_by = NULL, locked_at = NULL,"
                        + " next_retry_at = CURRENT_TIMESTAMP"
                        + " + (:retry * INTERVAL '1 millisecond'),"
                        + " error_message = 'Attempt lease expired' WHERE id IN (SELECT id FROM "
                        + table
                        + " WHERE (status = 'PROCESSING' AND locked_at <= CURRENT_TIMESTAMP"
                        + " - (:lease * INTERVAL '1 millisecond'))"
                        + " OR (status = 'PENDING' AND retry_count >= :max)"
                        + " ORDER BY created_at, id LIMIT :limit FOR UPDATE SKIP LOCKED)")
                .setParameter("max", properties.maxAttempts())
                .setParameter("retry", properties.retryDelay().toMillis())
                .setParameter("lease", properties.lease().toMillis())
                .setParameter("limit", properties.batchSize())
                .executeUpdate());
  }

  @Override
  public boolean complete(OutboxAttempt<E> attempt, Throwable failure) {
    return transaction.execute(
        status -> {
          String result =
              failure == null
                  ? "status = 'PUBLISHED', sent_at = CURRENT_TIMESTAMP,"
                      + " next_retry_at = NULL, error_message = NULL"
                  : "status = CASE WHEN retry_count >= :max THEN 'FAILED' ELSE 'PENDING' END,"
                      + " next_retry_at = CURRENT_TIMESTAMP + (:retry * INTERVAL '1 millisecond'),"
                      + " error_message = :error";
          var query =
              entityManager
                  .createNativeQuery(
                      "UPDATE "
                          + table
                          + " SET "
                          + result
                          + ", locked_by = NULL, locked_at = NULL WHERE id = :id"
                          + " AND status = 'PROCESSING' AND locked_by = :token")
                  .setParameter("id", attempt.eventId())
                  .setParameter("token", attempt.token());
          if (failure != null) {
            String message = failure.toString();
            query
                .setParameter("max", properties.maxAttempts())
                .setParameter("retry", properties.retryDelay().toMillis())
                .setParameter("error", message.substring(0, Math.min(message.length(), 255)));
          }
          return query.executeUpdate() == 1;
        });
  }
}
