package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.repository.PointHistoryRepositoryImpl;
import io.hhplus.tdd.point.repository.UserPointRepository;
import io.hhplus.tdd.point.repository.UserPointRepositoryImpl;
import io.hhplus.tdd.point.service.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

public class ConcurrentTest {

    private static final Logger log = LoggerFactory.getLogger(ConcurrentTest.class);


    UserPointTable userPointTable = new UserPointTable();
    UserPointRepository userPointRepository = new UserPointRepositoryImpl(userPointTable);
    PointHistoryTable pointHistoryTable = new PointHistoryTable();
    PointHistoryRepository pointHistoryRepository = new PointHistoryRepositoryImpl(pointHistoryTable);
    PointService pointService = new PointService(userPointRepository, pointHistoryRepository);

    final long USER_ID = 1L;

    @BeforeEach
    void initCharge(){
        log.info("========초기 포인트 충전 시작========");
        pointService.charge(USER_ID, 10000L);
        log.info("========초기 포인트 충전 완료========");
    }


    @Test
    @DisplayName("동시에 한명에게 여러번 요청시 충전 성공")
    void succeedWhenMultipleChargeRequestsAreProcessedForSingleUser() throws InterruptedException {
        //given
        UserPoint prevUserPoint = pointService.getPoint(USER_ID);

        int threadCount = 10;
        final ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount);

        //When
        for(int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.charge(USER_ID, 500);
                } finally {
                    latch.countDown(); // 스레드 작업 완료 후 카운트 다운
                }
            });
        }
        latch.await();
        executorService.shutdown();

        UserPoint currentUserPoint = pointService.getPoint(USER_ID);
        List<PointHistory> pointHistoryList = pointService.getAllHistory(USER_ID);

        //then
        assertThat(currentUserPoint.point()).isEqualTo(prevUserPoint.point() + 500 * threadCount);
        assertThat(pointHistoryList.size()).isEqualTo(threadCount + 1);
    }

    @Test
    @DisplayName("동시에 한명에게 여러번 사용시 사용 성공")
    void succeedWhenMultipleUseRequestsAreProcessedForSingleUser() throws InterruptedException {
        //given
        UserPoint prevUserPoint = pointService.getPoint(USER_ID);

        int threadCount = 10;
        final ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount);

        //when
        for(int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.use(USER_ID, 250);
                } finally {
                    latch.countDown(); // 스레드 작업 완료 후 카운트 다운
                }
            });
        }
        latch.await();
        executorService.shutdown();

        UserPoint currentUserPoint = pointService.getPoint(USER_ID);
        List<PointHistory> pointHistoryList = pointService.getAllHistory(USER_ID);

        //then
        assertThat(currentUserPoint.point()).isEqualTo(prevUserPoint.point() - threadCount * 250);
        assertThat(pointHistoryList.size()).isEqualTo(threadCount + 1);
    }

    @Test
    @DisplayName("동시에 한명에게 충전과 사용 성공")
    void succeedWhenChargeAndUseRequestsAreProcessedSimultaneouslyForSingleUser() throws InterruptedException {
        //given
        UserPoint prevUserPoint = pointService.getPoint(USER_ID);

        int threadCount = 4;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        executorService.submit(() -> {
            try {
                pointService.charge(USER_ID, 500);
            } finally {
                latch.countDown(); // 스레드 작업 완료 후 카운트 다운
            }
        });

        executorService.submit(() -> {
            try {
                pointService.use(USER_ID, 350);
            } finally {
                latch.countDown(); // 스레드 작업 완료 후 카운트 다운
            }
        });

        executorService.submit(() -> {
            try {
                pointService.charge(USER_ID, 700);
            } finally {
                latch.countDown(); // 스레드 작업 완료 후 카운트 다운
            }
        });

        executorService.submit(() -> {
            try {
                pointService.use(USER_ID, 250);
            } finally {
                latch.countDown(); // 스레드 작업 완료 후 카운트 다운
            }
        });

        latch.await();
        executorService.shutdown();

        //when
        UserPoint currentUserPoint = pointService.getPoint(USER_ID);
        List<PointHistory> pointHistoryList = pointService.getAllHistory(USER_ID);

        //then
        assertThat(currentUserPoint.point()).isEqualTo(prevUserPoint.point() + 600);
        assertThat(pointHistoryList.size()).isEqualTo(threadCount + 1);
    }

}
