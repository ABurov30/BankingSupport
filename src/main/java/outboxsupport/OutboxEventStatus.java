package outboxsupport;

public enum OutboxEventStatus {
  PENDING,
  PROCESSING,
  PUBLISHED,
  FAILED
}
