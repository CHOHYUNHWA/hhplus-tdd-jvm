package io.hhplus.tdd.point;

import io.hhplus.tdd.point.domain.TransactionType;
import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.repository.UserPointRepository;
import io.hhplus.tdd.point.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @InjectMocks
    PointService pointService;

    @Mock
    UserPointRepository userPointRepository;

    @Mock
    PointHistoryRepository pointHistoryRepository;

    static final Long USER_ID = 1L;

    @Test
    @DisplayName("0 이하의 포인트 충전 시 충전 실패")
    void failWhenChargingPointsIsZeroOrNegative() {
        //given
        UserPoint userPoint = new UserPoint(USER_ID,0L, System.currentTimeMillis());

        given(userPointRepository.findById(USER_ID)).willReturn(userPoint);
        //when
        //then
        assertThatThrownBy(() -> {
            pointService.charge(userPoint.id(), 0);
        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충천할 포인트는 0보다 커야 합니다.");

        assertThatThrownBy(() -> {
            pointService.charge(userPoint.id(), -100);
        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충천할 포인트는 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("보유 포인트가 100만 포인트가 넘어갈 경우 충전 실패")
    void failWhenChargingExceedsMaximumLimit() {
        //given
        UserPoint userPoint = new UserPoint(USER_ID, 1_000_000L, System.currentTimeMillis());

        given(userPointRepository.findById(USER_ID)).willReturn(userPoint);
        //when
        // then
        assertThatThrownBy(() -> {
            pointService.charge(userPoint.id(), 100);
        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("최대 충전할 수 있는 포인트는 100만 입니다.");
    }

    @Test
    @DisplayName("0원 이상 충전 후 최대포인트가 초과되지 않는 경우 충전 성공")
    void succeedWhenChargingPointsDoesNotExceedMaximumLimit() {
        //given
        long currentPoint = 1500L;
        long chargeAmount = 1000L;
        long expectedPoint = 2500L;
        UserPoint userPoint = new UserPoint(USER_ID, currentPoint, System.currentTimeMillis());
        given(userPointRepository.findById(USER_ID)).willReturn(userPoint);
        given(userPointRepository.saveOrUpdate(USER_ID, expectedPoint)).willReturn(new UserPoint(USER_ID, expectedPoint, System.currentTimeMillis()));

        //when
        UserPoint result = pointService.charge(USER_ID, chargeAmount);

        //then
        assertThat(result).isNotNull();
        assertThat(result.point()).isEqualTo(expectedPoint);
        verify(userPointRepository).saveOrUpdate(eq(USER_ID), eq(expectedPoint));
        verify(pointHistoryRepository).save(eq(USER_ID), eq(chargeAmount), eq(TransactionType.CHARGE), anyLong());
    }

}
