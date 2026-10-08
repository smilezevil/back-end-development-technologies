package com.smilezevil.hotelbooking.aspect;

import com.smilezevil.hotelbooking.annotation.NormalizeInput;
import com.smilezevil.hotelbooking.dto.GuestDTO;
import com.smilezevil.hotelbooking.dto.HotelDTO;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.annotation.Annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NormalizeInputAspectTest {

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private Signature signature;

    private final NormalizeInputAspect aspect = new NormalizeInputAspect();

    @BeforeEach
    void setUp() {
        lenient().when(joinPoint.getSignature()).thenReturn(signature);
        lenient().when(signature.toShortString()).thenReturn("GuestService.create(..)");
    }

    @Test
    void normalize_guestWithMessyInput_trimsFieldsAndLowercasesEmail() {
        GuestDTO guest = new GuestDTO();
        guest.setFirstName("  Анастасія ");
        guest.setLastName("Дем'янчук");
        guest.setEmail("  Nastya.D@Gmail.COM ");
        guest.setPhone(" +380501112233 ");
        when(joinPoint.getArgs()).thenReturn(new Object[]{guest});

        aspect.normalize(joinPoint, normalizeInput("email"));

        assertThat(guest.getFirstName()).isEqualTo("Анастасія");
        assertThat(guest.getLastName()).isEqualTo("Дем'янчук");
        assertThat(guest.getEmail()).isEqualTo("nastya.d@gmail.com");
        assertThat(guest.getPhone()).isEqualTo("+380501112233");
    }

    @Test
    void normalize_hotelWithRepeatedSpaces_collapsesThemButKeepsLineBreaks() {
        HotelDTO hotel = new HotelDTO();
        hotel.setName("  Bukovina    Palace ");
        hotel.setCity("Чернівці");
        hotel.setDescription("Готель у центрі.\nПоруч парк.");
        when(joinPoint.getArgs()).thenReturn(new Object[]{hotel});

        aspect.normalize(joinPoint, normalizeInput());

        assertThat(hotel.getName()).isEqualTo("Bukovina Palace");
        assertThat(hotel.getCity()).isEqualTo("Чернівці");
        assertThat(hotel.getDescription()).isEqualTo("Готель у центрі.\nПоруч парк.");
    }

    @Test
    void normalize_updateWithIdAndDto_normalizesOnlyDtoAndKeepsId() {
        GuestDTO guest = new GuestDTO();
        guest.setEmail(" NEW@GMAIL.COM ");
        Object[] args = {1L, guest};
        when(joinPoint.getArgs()).thenReturn(args);

        aspect.normalize(joinPoint, normalizeInput("email"));

        assertThat(args[0]).isEqualTo(1L);
        assertThat(guest.getEmail()).isEqualTo("new@gmail.com");
    }

    @Test
    void normalize_dtoWithNullFields_leavesThemNull() {
        GuestDTO guest = new GuestDTO();
        guest.setEmail("guest@gmail.com");
        when(joinPoint.getArgs()).thenReturn(new Object[]{guest});

        aspect.normalize(joinPoint, normalizeInput("email"));

        assertThat(guest.getFirstName()).isNull();
        assertThat(guest.getPhone()).isNull();
        assertThat(guest.getEmail()).isEqualTo("guest@gmail.com");
    }

    @Test
    void normalize_emailNotListedInLowercase_onlyTrimsAndKeepsCase() {
        GuestDTO guest = new GuestDTO();
        guest.setEmail("  Nastya.D@Gmail.COM ");
        when(joinPoint.getArgs()).thenReturn(new Object[]{guest});

        aspect.normalize(joinPoint, normalizeInput());

        assertThat(guest.getEmail()).isEqualTo("Nastya.D@Gmail.COM");
    }


    private static NormalizeInput normalizeInput(String... lowercase) {
        return new NormalizeInput() {
            @Override
            public String[] lowercase() {
                return lowercase;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return NormalizeInput.class;
            }
        };
    }
}