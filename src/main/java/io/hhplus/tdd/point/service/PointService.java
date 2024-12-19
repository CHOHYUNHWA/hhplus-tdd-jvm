package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.TransactionType;
import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointService {

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final Map<String, Lock> userLockMap = new ConcurrentHashMap<>();

    public UserPoint getPoint(long userId){
        return userPointRepository.findById(userId);
    }

    public UserPoint charge(long userId, long amount){
        Lock lock = userLockMap.computeIfAbsent(String.valueOf(userId), k -> new ReentrantLock(true));
        lock.lock();
        long startLockTime = System.currentTimeMillis();
        log.info("charge start - Id : {}", userId);

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

    public List<PointHistory> getAllHistory(long userId){
        return pointHistoryRepository.findAllById(userId);
    }

}
