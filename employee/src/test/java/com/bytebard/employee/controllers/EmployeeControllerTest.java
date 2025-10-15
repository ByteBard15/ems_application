package com.bytebard.employee.controllers;

import com.bytebard.core.api.constants.Routes;
import com.bytebard.employee.services.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(controllers = EmployeeController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import="
})
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeService employeeService;

    @Test
    void createEndpoint_returnsOk() throws Exception {
        when(employeeService.create(any())).thenReturn(null);
        var url = Routes.USERS;

        mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"John\",\"lastName\":\"Doe\",\"email\":\"john.doe@example.com\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateEndpoint_returnsOk() throws Exception {
        when(employeeService.update(any(), any())).thenReturn(null);
        var url = String.format("%s/%d", Routes.USERS, 1L);

        mockMvc.perform(put(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"John\",\"lastName\":\"Doe Updated\",\"email\":\"john.updated@example.com\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void getByIdEndpoint_returnsOk() throws Exception {
        when(employeeService.getById(any())).thenReturn(null);
        var url = String.format("%s/%d", Routes.USERS, 1L);

        mockMvc.perform(get(url))
                .andExpect(status().isOk());
    }

    @Test
    void deleteEndpoint_returnsOk() throws Exception {
        doNothing().when(employeeService).delete(any());
        var url = String.format("%s/%d", Routes.USERS, 1L);

        mockMvc.perform(delete(url))
                .andExpect(status().isOk());
    }

    @Test
    void getAllEndpoint_returnsOk() throws Exception {
        when(employeeService.getAllUsers(any(), any())).thenReturn(null);
        var url = Routes.USERS + "?page=0&size=10";

        mockMvc.perform(get(url))
                .andExpect(status().isOk());
    }
}
