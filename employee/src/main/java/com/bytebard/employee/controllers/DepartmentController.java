package com.bytebard.employee.controllers;

import com.bytebard.core.api.constants.Routes;
import com.bytebard.core.api.types.ApiResponse;
import com.bytebard.employee.services.DepartmentService;
import com.bytebard.employee.types.MutateDepartmentRequest;
import com.bytebard.employee.types.DepartmentDTO;
import jakarta.ws.rs.QueryParam;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping(Routes.DEPARTMENTS)
@RestController
public class DepartmentController {
    private final DepartmentService departmentService;
    public DepartmentController(final DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DepartmentDTO>> create(@RequestBody final MutateDepartmentRequest request) {
        var response = departmentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(response, HttpStatus.CREATED, true));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartmentDTO>> update(@PathVariable("id") Long id, @RequestBody final MutateDepartmentRequest request) {
        var response = departmentService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(response, HttpStatus.OK, true));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartmentDTO>> delete(@PathVariable("id") Long id) {
        departmentService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(null, HttpStatus.OK, true));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartmentDTO>> getById(@PathVariable("id") Long id) {
        var department = departmentService.get(id);
        return ResponseEntity.ok(new ApiResponse<>(department, HttpStatus.OK, true));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DepartmentDTO>>> getAll(@QueryParam("page") Integer page, @QueryParam("size") Integer size) {
        var departments = departmentService.getAll(page, size);
        return ResponseEntity.ok(new ApiResponse<>(departments, HttpStatus.OK, true));
    }
}
