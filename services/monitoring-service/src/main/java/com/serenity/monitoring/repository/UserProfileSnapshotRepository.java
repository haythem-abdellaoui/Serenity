package com.serenity.monitoring.repository;

import com.serenity.monitoring.entity.UserProfileSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserProfileSnapshotRepository extends JpaRepository<UserProfileSnapshot, Long> {

    List<UserProfileSnapshot> findAllByUserIdIn(Collection<Long> userIds);

    Optional<UserProfileSnapshot> findByUserId(Long userId);
}
