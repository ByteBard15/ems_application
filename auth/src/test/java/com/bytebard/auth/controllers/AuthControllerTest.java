package com.bytebard.auth.controllers;

import com.bytebard.auth.AuthApplication;
import com.bytebard.auth.service.AuthService;
import com.bytebard.core.api.constants.Routes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {"spring.cloud.config.enabled=false"}, inheritLocations = false, inheritProperties = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void loginEndpoint_returnsOk() throws Exception {
        when(authService.login(any())).thenReturn(null);
        var url = String.format("%s%s", Routes.AUTH, Routes.LOGIN);

        mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"u\",\"password\":\"p\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void changePasswordEndpoint_returnsOk() throws Exception {
        doNothing().when(authService).changePassword(any());
        var url = String.format("%s%s", Routes.AUTH, Routes.CHANGE_PASSWORD);

        mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"email\",\"oldPassword\":\"o\",\"newPassword\":\"n\"}"))
                .andExpect(status().isOk());
    }
}
