package com.smilezevil.notification;

import com.smilezevil.common.dto.NotificationReceipt;
import com.smilezevil.common.dto.NotificationRequest;
import com.smilezevil.common.event.BookingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final String REJECTED_DOMAIN = ".invalid";

    private final List<NotificationReceipt> sent = new CopyOnWriteArrayList<>();
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    public NotificationReceipt send(NotificationRequest request) {
        String recipient = request.recipient() == null ? "" : request.recipient().trim();
        if (recipient.isEmpty()) {
            throw new IllegalArgumentException("Recipient must not be blank");
        }
        if (recipient.endsWith(REJECTED_DOMAIN)) {
            throw new RecipientRejectedException("Mail server rejected recipient " + recipient);
        }
        NotificationReceipt receipt = new NotificationReceipt(
                UUID.randomUUID().toString(), recipient, request.subject(), "QUEUED");
        sent.add(receipt);
        log.info("NOTIFY {} [{}] handled on {}", recipient, request.subject(), Thread.currentThread());
        return receipt;
    }

    public boolean notifyAbout(BookingEvent event) {
        if (!processedEventIds.add(event.eventId())) {
            log.info("DUPLICATE event {} for booking {} ignored", event.eventId(), event.bookingId());
            return false;
        }
        String recipient = event.guestEmail() == null ? "" : event.guestEmail().trim();
        String status = recipient.endsWith(REJECTED_DOMAIN) ? "REJECTED" : "SENT";
        NotificationReceipt receipt = new NotificationReceipt(
                UUID.randomUUID().toString(), recipient, subjectFor(event), status);
        sent.add(receipt);
        log.info("NOTIFY {} [{}] {} handled on {}", recipient, receipt.subject(), status, Thread.currentThread());
        return true;
    }

    public List<NotificationReceipt> history() {
        return List.copyOf(sent);
    }

    private String subjectFor(BookingEvent event) {
        return switch (event.type()) {
            case CREATED -> "Booking #" + event.bookingId() + " confirmed: "
                    + event.checkInDate() + " - " + event.checkOutDate();
            case UPDATED -> "Booking #" + event.bookingId() + " changed: "
                    + event.checkInDate() + " - " + event.checkOutDate();
            case CANCELLED -> "Booking #" + event.bookingId() + " cancelled";
        };
    }
}