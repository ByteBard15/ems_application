package com.bytebard.employee.controllers;

import com.bytebard.core.api.constants.Routes;
import com.bytebard.employee.services.DepartmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DepartmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {"spring.cloud.config.enabled=false"})
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DepartmentService departmentService;

    @Test
    void createEndpoint_returnsCreated() throws Exception {
        when(departmentService.create(any())).thenReturn(null);
        var url = Routes.DEPARTMENTS;

        mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Engineering\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void updateEndpoint_returnsOk() throws Exception {
        when(departmentService.update(any(), any())).thenReturn(null);
        var url = String.format("%s/%d", Routes.DEPARTMENTS, 1L);

        mockMvc.perform(put(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Eng - Updated\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteEndpoint_returnsOk() throws Exception {
        doNothing().when(departmentService).delete(any());
        var url = String.format("%s/%d", Routes.DEPARTMENTS, 1L);

        mockMvc.perform(delete(url))
                .andExpect(status().isOk());
    }

    @Test
    void getByIdEndpoint_returnsOk() throws Exception {
        when(departmentService.get(any())).thenReturn(null);
        var url = String.format("%s/%d", Routes.DEPARTMENTS, 1L);

        mockMvc.perform(get(url))
                .andExpect(status().isOk());
    }

    @Test
    void getAllEndpoint_returnsOk() throws Exception {
        when(departmentService.getAll(any(), any())).thenReturn(null);
        var url = Routes.DEPARTMENTS + "?page=0&size=10";

        mockMvc.perform(get(url))
                .andExpect(status().isOk());
    }
}
