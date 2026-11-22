package com.clouddrive.service;

import com.clouddrive.dto.ActivityLogDTO;
import com.clouddrive.entity.ActivityLog;
import com.clouddrive.entity.User;
import com.clouddrive.repository.ActivityLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ActivityService {

    private final ActivityLogRepository activityLogRepository;

    public ActivityService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    @Transactional
    public void log(User user, String action, String targetName, String details) {
        ActivityLog log = new ActivityLog(user, action, targetName, details);
        activityLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<ActivityLogDTO> getUserActivities(User user) {
        return activityLogRepository.findTop50ByUserOrderByTimestampDesc(user)
                .stream()
                .map(ActivityLogDTO::fromEntity)
                .toList();
    }
}
