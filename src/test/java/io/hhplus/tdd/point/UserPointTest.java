package io.hhplus.tdd.point;

import io.hhplus.tdd.point.domain.TransactionType;
import io.hhplus.tdd.point.domain.UserPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UserPointTest {

    static final Long USER_ID = 1L;

    @Test
    @DisplayName("0 이하의 금액 충전시 테스트 실패")
    void chargeFailsWhenAmountIsZeroOrNegative() {
        //given
        UserPoint userPoint = UserPoint.empty(USER_ID);

        //when - then
        assertThatThrownBy(() -> userPoint.charge(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충천할 포인트는 0보다 커야 합니다.");

        assertThatThrownBy(() -> userPoint.charge(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충천할 포인트는 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("충전양이 100만 이상일 경우 테스트 실패")
    void chargeFailsWhenAmountExceedsLimit(){
        //given
        UserPoint userPoint = UserPoint.empty(USER_ID);

        //when - then
        assertThatThrownBy(() -> userPoint.charge(1_000_001L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("최대 충전할 수 있는 포인트는 100만 입니다.");
    }

    @Test
    @DisplayName("충전 성공 시 포인트가 증가")
    void pointsIncreaseWhenChargeIsSuccessful() {

        //give
        UserPoint userPoint = UserPoint.empty(USER_ID);

        //when - then
        UserPoint updatedUserPoint = userPoint.charge(1000L);
        assertThat(updatedUserPoint.point()).isEqualTo(1000L);

    }

    @Test
    @DisplayName("사용할 포인트가 0보다 작을 경우 실패")
    void failWhenUsedPointsAreNegative() {
        // given
        UserPoint userPoint = new UserPoint(USER_ID, 1000L, System.currentTimeMillis());

        // when -then
        assertThatThrownBy(() -> userPoint.use(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용할 포인트는 0 이상이어야 합니다.");

        assertThatThrownBy(() -> userPoint.use(-1000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용할 포인트는 0 이상이어야 합니다.");
    }

    @Test
    @DisplayName("사용할 포인트가 보유 포인트 보다 작을 경우 실패")
    void failWhenUsedPointsExceedAvailablePoints() {
        // given
        UserPoint userPoint = new UserPoint(USER_ID, 1000L, System.currentTimeMillis());
        // when
        // then
        assertThatThrownBy(() -> userPoint.use(1500L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용할 포인트는 보유 포인트보다 작아야 합니다.");
    }

    @Test
    @DisplayName("사용 성공 시 보유 포인트 차감")
    void deductPointsWhenUsageIsSuccessful() {
        // given
        UserPoint userPoint = new UserPoint(USER_ID, 10000L, System.currentTimeMillis());
        // when
        // then
        UserPoint usedUserPoint = userPoint.use(1500L);
        assertThat(usedUserPoint.point()).isEqualTo(8500L);
    }


}
