package com.clouddrive.controller;

import com.clouddrive.dto.ActivityLogDTO;
import com.clouddrive.entity.User;
import com.clouddrive.service.AuthService;
import com.clouddrive.service.ActivityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    private final ActivityService activityService;
    private final AuthService authService;

    public ActivityController(ActivityService activityService, AuthService authService) {
        this.activityService = activityService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<ActivityLogDTO>> getUserActivities(@AuthenticationPrincipal UserDetails userDetails) {
        User user = authService.getUserByUsername(userDetails.getUsername());
        return ResponseEntity.ok(activityService.getUserActivities(user));
    }
}
