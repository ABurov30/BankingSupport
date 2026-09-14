package outboxsupport;

import java.util.UUID;

/** Immutable identity retained independently of the detached event. */
public record OutboxAttempt<E extends OutboxEventEntity>(UUID eventId, String token, E event) {}
