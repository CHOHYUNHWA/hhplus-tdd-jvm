package io.hhplus.tdd.point.repository;

import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.TransactionType;

import java.util.List;

public interface PointHistoryRepository {

    PointHistory save(long userId, long amount, TransactionType type, long updateMillis);

    List<PointHistory> findAllById(long userId);

}
