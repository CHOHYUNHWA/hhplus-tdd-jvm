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

@Service
@RequiredArgsConstructor
@Slf4j
public class PointService {

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    public UserPoint getPoint(long userId){
        return userPointRepository.findById(userId);
    }

    public UserPoint charge(long userId, long amount){
        UserPoint userPoint = getPoint(userId);
        UserPoint updatedUserPoint = userPoint.charge(amount);

        PointHistory pointHistory = PointHistory.createHistory(userId, amount, TransactionType.CHARGE);
        pointHistoryRepository.save(pointHistory.id(), pointHistory.amount(), pointHistory.type(), System.currentTimeMillis());

        UserPoint result = userPointRepository.saveOrUpdate(updatedUserPoint.id(), updatedUserPoint.point());

        return null;
    }

    public UserPoint use(long userId, long amount) {
        return null;
    }

    public List<PointHistory> getAllHistory(long userId){
        return pointHistoryRepository.findAllById(userId);
    }

}
