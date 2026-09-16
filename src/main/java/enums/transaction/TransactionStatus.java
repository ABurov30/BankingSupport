package enums.transaction;

public enum TransactionStatus {
  CREATED,
  FUNDS_RESERVED,
  CARD_LIMIT_RESERVED,
  FUNDS_REQUESTED,
  COMPLETED,
  FAILED,
  COMPENSATED
}
