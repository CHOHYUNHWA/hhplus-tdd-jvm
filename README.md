# TDD 로 개발하기 와 동시성 제어
> TDD(Test-Driven-Development)를 이용하여, 포인트 충전/사용/내역 관리 API를 구현하고, 이에 발생할 수 있는 동시성 이슈를 해결한다.

## 요구사항
### API 개발
1. PATCH - /point/{id}/charge : 포인트를 충전한다.
2. PATCH - /point/{id}/user : 포인트를 사용한다.
3. GET - /point/{id} : 포인트를 조회한다.
4. GET - /point/{id}/histories : 포인트 내역을 조회한다.

### 기능 요구사항 정의
* 0원 이하의 포인트 사용 시 포인트 사용은 실패해야 한다.
* 사용 포인트가 보유포인트보다 큰 경우 포인트 사용은 실패해야 한다.
* 0원 이하의 포인트 충전 시 포인트 충전은 실패해야 한다.
* 충전 후 포인트가 1,000,000 포인트 이상인 경우 충전은 실패해야 한다.
---

## 구현 및 실행 순서
1. 컨트롤러, 서비스, 레포지토리, 도메인 레이어 분리
2. 각각의 레이어별 인터페이스 구현(껍데기)
3. 구현된 인터페이스 코드를 바탕으로 실패하는 테스트를 작성
4. 실패한 테스트를 토대로 기능구현
5. 테스트를 통과하는 기능 구현 후 다음 테스트 코드 작성 반복
6. 레이어별 단위테스트를 진행후
7. 최종 통합테스트(E2E 테스트) 작성

---
## 동시성 제어 분석
### 아이데이션
* 어떠한 상황에서 동시성 이슈를 고려해야할까?
    1. 일반적으로 동시성 이슈는 공통된 자원에 접근하여 자원의 값이나 상태를 변경시킬때 일어남
    2. 그렇다면, 다수의 유저가 개개인의 자원에 접근하는 경우에는 동시성 이슈가 발생하지 않음
    3. 하나의 유저에 동시에 충전/사용 요청이 동시에 발생 시 요청을 수행하는 시점의 공통된 자원을 가져와 쓰기 혹은 수정을 진행할 경우 동시성 이슈가 발생할 수 있음

* 결론
    * 하나의 유저가 동시에 충전 또는 사용이 반복적으로 발생할 때, 공통된 자원에 접근할 수 없도록 막고 먼저 들어온 요청이 처리된 후 다음 요청을 진행해야함

### 구현방식
#### `ConcurrentHashMap`, `ReentrantLock`을 이용한 구현

* `ConcurrentHashMap`의 특징
    * Thread-Safe 하기 때문에 동시성 처리를 지원 -> 일관된 상태를 보장(데이터 정합성)
* `ReentrantLock`의 특징
    * 유연한 동시성 제어가 가능하다.
    * 공정모드(fairness)를 통해 가장 먼저 실행된 스레드가 자원을 획득하고 순차적인 처리가 가능하다. - `new ReentrantLock(true)`

#### `ConcurrentHashMap`, `ReentrantLock`을 선택한 이유
* 동시성 제어를 위해 여러 키워드 중 대표적으로 `synchronized`, `BlockingQueue`가 있었다.
  * `synchronized`를 사용하지 않은 이유
    * `synchronized`가 메서드에 선언되어지는 경우, 메서드가 통째로 락이 걸리기때문에 예를들어 서로다른 유저가 다른 자원을 쓰기/수정하는 경우 불필요한 대기시간이 발생한다.
  * `BlockingQueue`
    * 미 학습(학습 예정)

<br/>

#### Service 코드
```java
public UserPoint charge(long userId, long amount){
    Lock lock = userLockMap.computeIfAbsent(String.valueOf(userId), k -> new ReentrantLock(true));
    lock.lock();
    long startLockTime = System.currentTimeMillis();
    log.info("charge start - acquired Id : {}", userId);

    try {
        UserPoint userPoint = getPoint(userId);
        UserPoint updatedUserPoint = userPoint.charge(amount);
        log.info("충전 완료 : UserPoint[id={}, point={}->{}]",updatedUserPoint.id(),userPoint.point(),updatedUserPoint.point());

        pointHistoryRepository.save(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());

        return userPointRepository.saveOrUpdate(updatedUserPoint.id(), updatedUserPoint.point());
    } finally {
        long lockEndTime = System.currentTimeMillis();
        log.info("charge completed - completed Id : {}, time taken: {}",userId, lockEndTime - startLockTime);
        lock.unlock();
    }
}

public UserPoint use(long userId, long amount) {
    Lock lock = userLockMap.computeIfAbsent(String.valueOf(userId), k -> new ReentrantLock(true));
    lock.lock();
    long startLockTime = System.currentTimeMillis();
    log.info("use start - acquired Id : {}", userId);

    try {
        UserPoint userPoint = getPoint(userId);
        UserPoint usedUserPoint = userPoint.use(amount);
        log.info("사용 완료 : UserPoint[id={}, point={}->{}]",usedUserPoint.id(),userPoint.point(),usedUserPoint.point());

        pointHistoryRepository.save(userId,amount,TransactionType.USE, System.currentTimeMillis());

        return userPointRepository.saveOrUpdate(usedUserPoint.id(), usedUserPoint.point());
    }
    finally {
        long lockEndTime = System.currentTimeMillis();
        log.info("use completed - completed Id : {}, time taken: {}",userId, lockEndTime - startLockTime);
        lock.unlock();
    }
}

```
<br><br>
#### Test 코드
```java
@BeforeEach
void initCharge(){
    log.info("========초기 포인트 충전 시작========");
    pointService.charge(USER_ID, 10000L);
    pointService.charge(USER_ID_2, 10000L);
    pointService.charge(USER_ID_3, 10000L);
    log.info("========초기 포인트 충전 완료========");
}

@Test
@DisplayName("동시에 여러명에게 충전과 사용 성공")
void succeedWhenChargeAndUseRequestsAreProcessedSimultaneouslyForSingleUser() throws InterruptedException {
    //given
    UserPoint prevUserPoint = pointService.getPoint(USER_ID);
    UserPoint prevUserPoint2 = pointService.getPoint(USER_ID_2);
    UserPoint prevUserPoint3 = pointService.getPoint(USER_ID_3);

    int threadCount = 4;
    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);

    executorService.submit(() -> {
        try {
            pointService.charge(USER_ID, 500);
            pointService.charge(USER_ID_2, 500);
            pointService.charge(USER_ID_3, 500);
        } finally {
            latch.countDown(); // 스레드 작업 완료 후 카운트 다운
        }
    });

    executorService.submit(() -> {
        try {
            pointService.use(USER_ID, 350);
            pointService.use(USER_ID_2, 350);
            pointService.use(USER_ID_3, 350);
        } finally {
            latch.countDown(); // 스레드 작업 완료 후 카운트 다운
        }
    });

    executorService.submit(() -> {
        try {
            pointService.charge(USER_ID, 700);
            pointService.charge(USER_ID_2, 700);
            pointService.charge(USER_ID_3, 700);
        } finally {
            latch.countDown(); // 스레드 작업 완료 후 카운트 다운
        }
    });

    executorService.submit(() -> {
        try {
            pointService.use(USER_ID, 250);
            pointService.use(USER_ID_2, 250);
            pointService.use(USER_ID_3, 250);
        } finally {
            latch.countDown(); // 스레드 작업 완료 후 카운트 다운
        }
    });

    latch.await();
    executorService.shutdown();

    //when
    UserPoint currentUserPoint1 = pointService.getPoint(USER_ID);
    UserPoint currentUserPoint2 = pointService.getPoint(USER_ID_2);
    UserPoint currentUserPoint3 = pointService.getPoint(USER_ID_3);
    List<PointHistory> pointHistoryList1 = pointService.getAllHistory(USER_ID);
    List<PointHistory> pointHistoryList2 = pointService.getAllHistory(USER_ID_2);
    List<PointHistory> pointHistoryList3 = pointService.getAllHistory(USER_ID_3);

    //then
    assertThat(currentUserPoint1.point()).isEqualTo(prevUserPoint.point() + 600);
    assertThat(currentUserPoint2.point()).isEqualTo(prevUserPoint2.point() + 600);
    assertThat(currentUserPoint3.point()).isEqualTo(prevUserPoint3.point() + 600);
    assertThat(pointHistoryList1.size()).isEqualTo(threadCount + 1);
    assertThat(pointHistoryList2.size()).isEqualTo(threadCount + 1);
    assertThat(pointHistoryList3.size()).isEqualTo(threadCount + 1);
}
```
<br><br>
#### 결과 로그
* 동시에 포인트 충전/사용 요청 발생 시 스레드는 요청 순서에 따라 자원에 대한 락을 획득하고 처리한 후에 락을 반환한다.
* 로그에서 확인할 수 있듯이 포인트가 순차적으로 충전/사용이 반복된다.
* 이를 통해서 동시에 충전/사용 요청이 발생하더라도 데이터 정합성을 유지할 수 있다.

* USER 1,2,3 의 요청 완료는 비동기 적으로 작동한다 
* 하지만 동일한 자원에 접근 시에는 락의 반환을 기다렸다가, 대기 시간이 가장 긴 스레드가 다음 순서로 락을 획득하여 순차적으로 처리 된다.
```
10:44:27.658 [Test worker] INFO io.hhplus.tdd.point.ConcurrentTest -- ========초기 포인트 충전 시작========
10:44:27.662 [Test worker] INFO io.hhplus.tdd.point.service.PointService -- charge start - acquired Id : 1
10:44:27.845 [Test worker] INFO io.hhplus.tdd.point.service.PointService -- 충전 완료 : UserPoint[id=1, point=0->10000]
10:44:28.441 [Test worker] INFO io.hhplus.tdd.point.service.PointService -- charge completed - completed Id : 1, time taken: 779
10:44:28.442 [Test worker] INFO io.hhplus.tdd.point.service.PointService -- charge start - acquired Id : 2
10:44:28.499 [Test worker] INFO io.hhplus.tdd.point.service.PointService -- 충전 완료 : UserPoint[id=2, point=0->10000]
10:44:28.795 [Test worker] INFO io.hhplus.tdd.point.service.PointService -- charge completed - completed Id : 2, time taken: 353
10:44:28.795 [Test worker] INFO io.hhplus.tdd.point.service.PointService -- charge start - acquired Id : 3
10:44:28.838 [Test worker] INFO io.hhplus.tdd.point.service.PointService -- 충전 완료 : UserPoint[id=3, point=0->10000]
10:44:29.188 [Test worker] INFO io.hhplus.tdd.point.service.PointService -- charge completed - completed Id : 3, time taken: 393
10:44:29.189 [Test worker] INFO io.hhplus.tdd.point.ConcurrentTest -- ========초기 포인트 충전 완료========
10:44:29.578 [pool-1-thread-1] INFO io.hhplus.tdd.point.service.PointService -- charge start - acquired Id : 1
10:44:29.598 [pool-1-thread-1] INFO io.hhplus.tdd.point.service.PointService -- 충전 완료 : UserPoint[id=1, point=10000->10500]
10:44:29.775 [pool-1-thread-1] INFO io.hhplus.tdd.point.service.PointService -- charge completed - completed Id : 1, time taken: 197
10:44:29.776 [pool-1-thread-1] INFO io.hhplus.tdd.point.service.PointService -- charge start - acquired Id : 2
10:44:29.776 [pool-1-thread-2] INFO io.hhplus.tdd.point.service.PointService -- use start - acquired Id : 1
10:44:29.850 [pool-1-thread-2] INFO io.hhplus.tdd.point.service.PointService -- 사용 완료 : UserPoint[id=1, point=10500->10150]
10:44:29.893 [pool-1-thread-1] INFO io.hhplus.tdd.point.service.PointService -- 충전 완료 : UserPoint[id=2, point=10000->10500]
10:44:29.914 [pool-1-thread-1] INFO io.hhplus.tdd.point.service.PointService -- charge completed - completed Id : 2, time taken: 138
10:44:29.914 [pool-1-thread-1] INFO io.hhplus.tdd.point.service.PointService -- charge start - acquired Id : 3
10:44:30.103 [pool-1-thread-1] INFO io.hhplus.tdd.point.service.PointService -- 충전 완료 : UserPoint[id=3, point=10000->10500]
10:44:30.290 [pool-1-thread-2] INFO io.hhplus.tdd.point.service.PointService -- use completed - completed Id : 1, time taken: 514
10:44:30.291 [pool-1-thread-2] INFO io.hhplus.tdd.point.service.PointService -- use start - acquired Id : 2
10:44:30.291 [pool-1-thread-3] INFO io.hhplus.tdd.point.service.PointService -- charge start - acquired Id : 1
10:44:30.292 [pool-1-thread-2] INFO io.hhplus.tdd.point.service.PointService -- 사용 완료 : UserPoint[id=2, point=10500->10150]
10:44:30.365 [pool-1-thread-3] INFO io.hhplus.tdd.point.service.PointService -- 충전 완료 : UserPoint[id=1, point=10150->10850]
10:44:30.489 [pool-1-thread-1] INFO io.hhplus.tdd.point.service.PointService -- charge completed - completed Id : 3, time taken: 575
10:44:30.643 [pool-1-thread-2] INFO io.hhplus.tdd.point.service.PointService -- use completed - completed Id : 2, time taken: 352
10:44:30.643 [pool-1-thread-2] INFO io.hhplus.tdd.point.service.PointService -- use start - acquired Id : 3
10:44:30.714 [pool-1-thread-2] INFO io.hhplus.tdd.point.service.PointService -- 사용 완료 : UserPoint[id=3, point=10500->10150]
10:44:30.728 [pool-1-thread-3] INFO io.hhplus.tdd.point.service.PointService -- charge completed - completed Id : 1, time taken: 437
10:44:30.728 [pool-1-thread-3] INFO io.hhplus.tdd.point.service.PointService -- charge start - acquired Id : 2
10:44:30.728 [pool-1-thread-4] INFO io.hhplus.tdd.point.service.PointService -- use start - acquired Id : 1
10:44:30.773 [pool-1-thread-3] INFO io.hhplus.tdd.point.service.PointService -- 충전 완료 : UserPoint[id=2, point=10150->10850]
10:44:30.781 [pool-1-thread-4] INFO io.hhplus.tdd.point.service.PointService -- 사용 완료 : UserPoint[id=1, point=10850->10600]
10:44:30.959 [pool-1-thread-3] INFO io.hhplus.tdd.point.service.PointService -- charge completed - completed Id : 2, time taken: 231
10:44:31.165 [pool-1-thread-2] INFO io.hhplus.tdd.point.service.PointService -- use completed - completed Id : 3, time taken: 522
10:44:31.165 [pool-1-thread-4] INFO io.hhplus.tdd.point.service.PointService -- use completed - completed Id : 1, time taken: 437
10:44:31.166 [pool-1-thread-4] INFO io.hhplus.tdd.point.service.PointService -- use start - acquired Id : 2
10:44:31.166 [pool-1-thread-3] INFO io.hhplus.tdd.point.service.PointService -- charge start - acquired Id : 3
10:44:31.305 [pool-1-thread-4] INFO io.hhplus.tdd.point.service.PointService -- 사용 완료 : UserPoint[id=2, point=10850->10600]
10:44:31.341 [pool-1-thread-3] INFO io.hhplus.tdd.point.service.PointService -- 충전 완료 : UserPoint[id=3, point=10150->10850]
10:44:31.491 [pool-1-thread-4] INFO io.hhplus.tdd.point.service.PointService -- use completed - completed Id : 2, time taken: 325
10:44:31.738 [pool-1-thread-3] INFO io.hhplus.tdd.point.service.PointService -- charge completed - completed Id : 3, time taken: 572
10:44:31.739 [pool-1-thread-4] INFO io.hhplus.tdd.point.service.PointService -- use start - acquired Id : 3
10:44:31.884 [pool-1-thread-4] INFO io.hhplus.tdd.point.service.PointService -- 사용 완료 : UserPoint[id=3, point=10850->10600]
10:44:32.317 [pool-1-thread-4] INFO io.hhplus.tdd.point.service.PointService -- use completed - completed Id : 3, time taken: 578
```




