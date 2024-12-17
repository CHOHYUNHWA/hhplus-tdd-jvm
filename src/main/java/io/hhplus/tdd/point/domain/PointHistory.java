package io.hhplus.tdd.point.domain;

public record PointHistory(
        long id,
        long userId,
        long amount,
        TransactionType type,
        long updateMillis
) {
    public static PointHistory createHistory(long userId, long amount, TransactionType transactionType) {
        return null;
    }
}
