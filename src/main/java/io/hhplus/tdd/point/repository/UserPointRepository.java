package io.hhplus.tdd.point.repository;

import io.hhplus.tdd.point.domain.UserPoint;

public interface UserPointRepository {

    UserPoint findById(Long id);

    UserPoint saveOrUpdate(long id, long amount);
}