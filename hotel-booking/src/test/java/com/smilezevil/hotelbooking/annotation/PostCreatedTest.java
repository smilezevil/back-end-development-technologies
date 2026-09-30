package com.smilezevil.hotelbooking.annotation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smilezevil.hotelbooking.controller.HotelController;
import com.smilezevil.hotelbooking.dto.HotelDTO;
import com.smilezevil.hotelbooking.service.HotelService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = HotelController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class PostCreatedTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HotelService hotelService;

    static class ReferenceEndpoints {

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        void withOriginalAnnotations() {
        }

        @PostCreated
        void withComposedAnnotation() {
        }
    }


    @Test
    void postCreated_givesSameMappingAndStatusAsOriginalAnnotations() throws Exception {
        Method original = ReferenceEndpoints.class.getDeclaredMethod("withOriginalAnnotations");
        Method composed = ReferenceEndpoints.class.getDeclaredMethod("withComposedAnnotation");

        RequestMapping originalMapping = AnnotatedElementUtils.findMergedAnnotation(original, RequestMapping.class);
        RequestMapping composedMapping = AnnotatedElementUtils.findMergedAnnotation(composed, RequestMapping.class);
        ResponseStatus originalStatus = AnnotatedElementUtils.findMergedAnnotation(original, ResponseStatus.class);
        ResponseStatus composedStatus = AnnotatedElementUtils.findMergedAnnotation(composed, ResponseStatus.class);

        assertThat(composedMapping.method()).containsExactly(RequestMethod.POST);
        assertThat(composedMapping.method()).isEqualTo(originalMapping.method());
        assertThat(composedMapping.path()).isEqualTo(originalMapping.path());
        assertThat(composedStatus.code()).isEqualTo(HttpStatus.CREATED);
        assertThat(composedStatus.code()).isEqualTo(originalStatus.code());
    }


    @Test
    void createHotel_validRequest_returns201AndCallsService() throws Exception {
        HotelDTO request = new HotelDTO();
        request.setName("Bukovina Palace");
        request.setCity("Чернівці");

        HotelDTO saved = new HotelDTO();
        saved.setId(1L);
        saved.setName("Bukovina Palace");
        saved.setCity("Чернівці");

        when(hotelService.create(any(HotelDTO.class))).thenReturn(saved);

        mockMvc.perform(post("/api/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Bukovina Palace"));

        verify(hotelService).create(any(HotelDTO.class));
    }

    @Test
    void createHotel_blankName_returns400AndServiceNotCalled() throws Exception {
        HotelDTO request = new HotelDTO();
        request.setName("");

        mockMvc.perform(post("/api/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(hotelService);
    }

    @Test
    void putToCreateUrl_returns405_becauseOnlyPostIsMapped() throws Exception {
        mockMvc.perform(put("/api/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(hotelService);
    }
}