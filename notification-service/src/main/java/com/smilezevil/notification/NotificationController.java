package com.smilezevil.notification;

import com.smilezevil.common.dto.NotificationReceipt;
import com.smilezevil.common.dto.NotificationRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public NotificationReceipt send(@RequestBody NotificationRequest request) {
        return notificationService.send(request);
    }

    @GetMapping
    public List<NotificationReceipt> history() {
        return notificationService.history();
    }
}