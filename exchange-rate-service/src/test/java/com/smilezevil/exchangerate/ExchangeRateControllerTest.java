package com.smilezevil.exchangerate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExchangeRateController.class)
@Import(ExchangeRateService.class)
class ExchangeRateControllerTest {

    @Autowired
    private MockMvc mockMvc;


    @Test
    void rate_knownCurrency_returns200WithRate() throws Exception {
        mockMvc.perform(get("/rates/EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.uahPerUnit").value(45.0));
    }

    @Test
    void convert_eurAmount_returnsConvertedAmount() throws Exception {
        mockMvc.perform(get("/rates/eur/convert").param("amountUah", "1200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.amount").value(26.67));
    }

    @Test
    void rate_unknownCurrency_returns404WithApiError() throws Exception {
        mockMvc.perform(get("/rates/XYZ"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("XYZ")));
    }

    @Test
    void convert_negativeAmount_returns400WithApiError() throws Exception {
        mockMvc.perform(get("/rates/EUR/convert").param("amountUah", "-5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}