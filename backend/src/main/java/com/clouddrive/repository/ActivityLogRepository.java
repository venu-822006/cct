package com.clouddrive.repository;

import com.clouddrive.entity.ActivityLog;
import com.clouddrive.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findTop50ByUserOrderByTimestampDesc(User user);
}
