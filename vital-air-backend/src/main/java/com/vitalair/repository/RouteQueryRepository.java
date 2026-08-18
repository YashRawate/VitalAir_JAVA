package com.vitalair.repository;

import com.vitalair.entity.RouteQuery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteQueryRepository extends JpaRepository<RouteQuery, Long> {
    List<RouteQuery> findByUserIdOrderByCreatedAtDesc(Long userId);
}
