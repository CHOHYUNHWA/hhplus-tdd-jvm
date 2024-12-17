package io.hhplus.tdd.point.domain;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {
    private static final long MAX_AMOUNT = 1_000_000L;

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }

    public UserPoint charge(long amount) {

        if(amount <= 0){
            throw new IllegalArgumentException("충천할 포인트는 0보다 커야 합니다.");
        }

        if(amount + this.point > MAX_AMOUNT){
            throw new IllegalArgumentException("최대 충전할 수 있는 포인트는 100만 입니다.");
        }

        return new UserPoint(id,  this.point + amount, System.currentTimeMillis());
    }
}
