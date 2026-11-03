package com.smilezevil.notification;

import com.smilezevil.common.dto.NotificationReceipt;
import com.smilezevil.common.dto.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final String REJECTED_DOMAIN = ".invalid";

    private final List<NotificationReceipt> sent = new CopyOnWriteArrayList<>();

    public NotificationReceipt send(NotificationRequest request) {
        String recipient = request.recipient() == null ? "" : request.recipient().trim();
        if (recipient.isEmpty()) {
            throw new IllegalArgumentException("Recipient must not be blank");
        }
        if (recipient.endsWith(REJECTED_DOMAIN)) {
            throw new RecipientRejectedException("Mail server rejected recipient " + recipient);
        }
        NotificationReceipt receipt = new NotificationReceipt(UUID.randomUUID().toString(), recipient, "QUEUED");
        sent.add(receipt);
        log.info("NOTIFY {} [{}] handled on {}", recipient, request.subject(), Thread.currentThread());
        return receipt;
    }

    public List<NotificationReceipt> history() {
        return List.copyOf(sent);
    }
}