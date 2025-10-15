package com.bytebard.employee.controllers;

import com.bytebard.core.api.constants.Routes;
import com.bytebard.core.api.types.MvcApiReponse;
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
    public ResponseEntity<MvcApiReponse<DepartmentDTO>> create(@RequestBody final MutateDepartmentRequest request) {
        var response = departmentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new MvcApiReponse<>(response, HttpStatus.CREATED, true));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MvcApiReponse<DepartmentDTO>> update(@PathVariable("id") Long id, @RequestBody final MutateDepartmentRequest request) {
        var response = departmentService.update(id, request);
        return ResponseEntity.ok(new MvcApiReponse<>(response, HttpStatus.OK, true));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MvcApiReponse<DepartmentDTO>> delete(@PathVariable("id") Long id) {
        departmentService.delete(id);
        return ResponseEntity.ok(new MvcApiReponse<>(null, HttpStatus.OK, true));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MvcApiReponse<DepartmentDTO>> getById(@PathVariable("id") Long id) {
        var department = departmentService.get(id);
        return ResponseEntity.ok(new MvcApiReponse<>(department, HttpStatus.OK, true));
    }

    @GetMapping
    public ResponseEntity<MvcApiReponse<List<DepartmentDTO>>> getAll(@RequestParam("page") Integer page, @RequestParam("size") Integer size) {
        var departments = departmentService.getAll(page, size);
        return ResponseEntity.ok(new MvcApiReponse<>(departments, HttpStatus.OK, true));
    }
}
