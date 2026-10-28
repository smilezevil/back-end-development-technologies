package com.smilezevil.notification;

import com.smilezevil.common.dto.NotificationReceipt;
import com.smilezevil.common.dto.NotificationRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationServiceTest {

    private final NotificationService notificationService = new NotificationService();


    @Test
    void send_validRecipient_returnsQueuedReceiptAndKeepsItInHistory() {
        NotificationReceipt receipt = notificationService.send(
                new NotificationRequest(" guest@gmail.com ", "Booking 4", "Confirmed"));

        assertThat(receipt.status()).isEqualTo("QUEUED");
        assertThat(receipt.recipient()).isEqualTo("guest@gmail.com");
        assertThat(receipt.id()).isNotBlank();
        assertThat(notificationService.history()).containsExactly(receipt);
    }

    @Test
    void send_invalidDomain_throwsRecipientRejectedAndKeepsHistoryEmpty() {
        assertThatThrownBy(() -> notificationService.send(
                new NotificationRequest("broken@hotel.invalid", "Booking 5", "Confirmed")))
                .isInstanceOf(RecipientRejectedException.class)
                .hasMessage("Mail server rejected recipient broken@hotel.invalid");
        assertThat(notificationService.history()).isEmpty();
    }

    @Test
    void send_blankRecipient_throwsIllegalArgument() {
        assertThatThrownBy(() -> notificationService.send(new NotificationRequest("  ", "Booking 6", "Confirmed")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Recipient must not be blank");
    }
}