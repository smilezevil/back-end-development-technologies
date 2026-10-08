package com.smilezevil.hotelbooking.aspect;

import com.smilezevil.hotelbooking.dto.GuestDTO;
import com.smilezevil.hotelbooking.entity.Guest;
import com.smilezevil.hotelbooking.mapper.GuestMapper;
import com.smilezevil.hotelbooking.repository.GuestRepository;
import com.smilezevil.hotelbooking.service.GuestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(MaskSensitiveDataProxyTest.TestConfig.class)
class MaskSensitiveDataProxyTest {

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @Import({GuestService.class, MaskSensitiveDataAspect.class})
    static class TestConfig {
    }

    @MockBean
    private GuestRepository guestRepository;

    @MockBean
    private GuestMapper guestMapper;

    @Autowired
    private GuestService guestService;

    @BeforeEach
    void setUp() {
        Guest guest = new Guest();
        guest.setId(1L);
        when(guestRepository.findAll()).thenReturn(List.of(guest));
        when(guestMapper.toDto(guest)).thenAnswer(invocation -> guestDto());
    }


    @Test
    void findAll_calledFromOutside_throughProxy_masksPersonalData() {
        List<GuestDTO> result = guestService.findAll();

        assertThat(result.get(0).getEmail()).isEqualTo("n***@gmail.com");
        assertThat(result.get(0).getPhone()).isEqualTo("*********2233");
        assertThat(result.get(0).getFirstName()).isEqualTo("Анастасія");
    }


    @Test
    void findAll_calledViaThisInsideService_bypassesProxy_dataStaysUnmasked() {
        List<GuestDTO> result = guestService.findAllForReport();

        assertThat(result.get(0).getEmail()).isEqualTo("nastya.d@gmail.com");
        assertThat(result.get(0).getPhone()).isEqualTo("+380501112233");
    }


    @Test
    void guestService_fromSpring_isAopProxy() {
        assertThat(AopUtils.isAopProxy(guestService)).isTrue();
    }


    private static GuestDTO guestDto() {
        GuestDTO dto = new GuestDTO();
        dto.setId(1L);
        dto.setFirstName("Анастасія");
        dto.setLastName("Дем'янчук");
        dto.setEmail("nastya.d@gmail.com");
        dto.setPhone("+380501112233");
        return dto;
    }
}