package com.bytebard.employee.controllers;

import com.bytebard.core.api.constants.Routes;
import com.bytebard.core.api.types.ApiResponse;
import com.bytebard.core.api.types.UserDTO;
import com.bytebard.employee.services.EmployeeService;
import com.bytebard.employee.types.MutateUserRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping(Routes.USERS)
@RestController
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserDTO>> create(@RequestBody MutateUserRequest request) {
        var user = employeeService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(user, HttpStatus.CREATED, true));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> update(@PathVariable("id") Long id, @RequestBody MutateUserRequest request) {
        var user = employeeService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(user, HttpStatus.OK, true));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> get(@PathVariable("id") Long id) {
        var user = employeeService.getById(id);
        return ResponseEntity.ok(new ApiResponse<>(user, HttpStatus.OK, true));
    }
}
