package com.smilezevil.notification;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import(NotificationService.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;


    @Test
    void send_validRecipient_returns202Queued() throws Exception {
        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipient":"guest@gmail.com","subject":"Booking 4","message":"Confirmed"}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.recipient").value("guest@gmail.com"));
    }

    @Test
    void send_invalidDomain_returns422WithApiError() throws Exception {
        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipient":"broken@hotel.invalid","subject":"Booking 5","message":"Confirmed"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("Mail server rejected recipient broken@hotel.invalid"));
    }

    @Test
    void history_afterSend_containsRecipient() throws Exception {
        mockMvc.perform(post("/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"recipient":"history@gmail.com","subject":"Booking 7","message":"Confirmed"}
                        """));

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].recipient", hasItem("history@gmail.com")));
    }
}