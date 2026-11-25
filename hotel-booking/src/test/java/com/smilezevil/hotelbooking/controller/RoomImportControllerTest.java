package com.smilezevil.hotelbooking.controller;

import com.smilezevil.hotelbooking.config.SecurityConfig;
import com.smilezevil.hotelbooking.dto.ImportStartedDTO;
import com.smilezevil.hotelbooking.dto.ImportStatusDTO;
import com.smilezevil.hotelbooking.exception.ImportConflictException;
import com.smilezevil.hotelbooking.service.RoomImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = RoomImportController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@Import(SecurityConfig.class)
class RoomImportControllerTest {

    private static final String FILE = "rooms-2027.csv";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoomImportService roomImportService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @Test
    void start_admin_returns202WithExecutionIdAndLocation() throws Exception {
        when(roomImportService.start(FILE, false)).thenReturn(new ImportStartedDTO(42L, FILE, "STARTING"));

        mockMvc.perform(post("/api/imports/rooms").param("file", FILE).with(admin()))
                .andExpect(status().isAccepted())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/imports/rooms/42"))
                .andExpect(jsonPath("$.executionId").value(42))
                .andExpect(jsonPath("$.status").value("STARTING"));
    }

    @Test
    void start_rerunFlag_isPassedToService() throws Exception {
        when(roomImportService.start(FILE, true)).thenReturn(new ImportStartedDTO(43L, FILE, "STARTING"));

        mockMvc.perform(post("/api/imports/rooms").param("file", FILE).param("rerun", "true").with(admin()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.executionId").value(43));
    }

    @Test
    void start_sameFileAgain_returns409WithHint() throws Exception {
        String message = "Файл 'rooms-2027.csv' уже імпортовано (виконання 7). "
                + "Щоб свідомо імпортувати його ще раз, додайте rerun=true";
        when(roomImportService.start(FILE, false)).thenThrow(new ImportConflictException(message));

        mockMvc.perform(post("/api/imports/rooms").param("file", FILE).with(admin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(message));
    }

    @Test
    void start_pathTraversal_returns400() throws Exception {
        String message = "Недопустиме ім'я файлу '../../etc/passwd': можна вказати лише файл з папки імпорту";
        when(roomImportService.start("../../etc/passwd", false)).thenThrow(new IllegalArgumentException(message));

        mockMvc.perform(post("/api/imports/rooms").param("file", "../../etc/passwd").with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(message));
    }

    @Test
    void start_userWithoutAdminRole_returns403() throws Exception {
        mockMvc.perform(post("/api/imports/rooms").param("file", FILE).with(jwt()
                        .jwt(token -> token.claim("preferred_username", "carol"))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(roomImportService);
    }

    @Test
    void status_admin_returnsCounters() throws Exception {
        when(roomImportService.status(42L)).thenReturn(new ImportStatusDTO(42L, FILE, "COMPLETED", "COMPLETED",
                LocalDateTime.of(2026, 11, 25, 20, 0, 0), LocalDateTime.of(2026, 11, 25, 20, 0, 2),
                10, 6, 2, 2, null));

        mockMvc.perform(get("/api/imports/rooms/42").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.readCount").value(10))
                .andExpect(jsonPath("$.writeCount").value(6))
                .andExpect(jsonPath("$.filterCount").value(2))
                .andExpect(jsonPath("$.skipCount").value(2));
    }

    private static RequestPostProcessor admin() {
        return jwt()
                .jwt(token -> token.claim("preferred_username", "bob"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}