package com.smilezevil.hotelbooking.controller;

import com.smilezevil.hotelbooking.config.SecurityConfig;
import com.smilezevil.hotelbooking.dto.RevisionDTO;
import com.smilezevil.hotelbooking.exception.ResourceNotFoundException;
import com.smilezevil.hotelbooking.service.HistoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = HistoryController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@Import(SecurityConfig.class)
class HistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HistoryService historyService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @Test
    void roomHistory_admin_returns200WithVersions() throws Exception {
        when(historyService.roomHistory(1L)).thenReturn(List.of(
                new RevisionDTO(1, LocalDateTime.of(2026, 11, 25, 19, 25, 44), "bob", "MOD",
                        Map.of("roomNumber", "101", "pricePerNight", 1400)),
                new RevisionDTO(2, LocalDateTime.of(2026, 11, 25, 19, 25, 49), "bob", "MOD",
                        Map.of("roomNumber", "101", "pricePerNight", 1450))));

        mockMvc.perform(get("/api/history/rooms/1").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].revision").value(1))
                .andExpect(jsonPath("$[0].changedBy").value("bob"))
                .andExpect(jsonPath("$[0].change").value("MOD"))
                .andExpect(jsonPath("$[1].snapshot.pricePerNight").value(1450));
    }

    @Test
    void roomHistory_userWithoutAdminRole_returns403() throws Exception {
        mockMvc.perform(get("/api/history/rooms/1").with(jwt()
                        .jwt(token -> token.claim("preferred_username", "carol"))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(historyService);
    }

    @Test
    void roomHistory_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/history/rooms/1"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(historyService);
    }

    @Test
    void hotelHistory_noVersions_returns404WithMessage() throws Exception {
        when(historyService.hotelHistory(99L))
                .thenThrow(new ResourceNotFoundException("Історії змін для Hotel #99 немає"));

        mockMvc.perform(get("/api/history/hotels/99").with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Історії змін для Hotel #99 немає"));
    }

    private static RequestPostProcessor admin() {
        return jwt()
                .jwt(token -> token.claim("preferred_username", "bob"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}