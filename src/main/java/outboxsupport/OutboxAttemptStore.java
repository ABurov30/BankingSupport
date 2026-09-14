package outboxsupport;

import java.util.List;

public interface OutboxAttemptStore<E extends OutboxEventEntity> {
  List<OutboxAttempt<E>> claim(int limit);

  int recover();

  boolean complete(OutboxAttempt<E> attempt, Throwable failure);
}
