package com.bytebard.employee.controllers;

import com.bytebard.core.api.constants.Routes;
import com.bytebard.core.api.types.MvcApiReponse;
import com.bytebard.core.api.types.UserDTO;
import com.bytebard.employee.services.EmployeeService;
import com.bytebard.employee.types.MutateUserRequest;
import jakarta.ws.rs.QueryParam;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping(Routes.USERS)
@RestController
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    public ResponseEntity<MvcApiReponse<UserDTO>> create(@RequestBody MutateUserRequest request) {
        var user = employeeService.create(request);
        return ResponseEntity.ok(new MvcApiReponse<>(user, HttpStatus.CREATED, true));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MvcApiReponse<UserDTO>> update(@PathVariable("id") Long id, @RequestBody MutateUserRequest request) {
        var user = employeeService.update(id, request);
        return ResponseEntity.ok(new MvcApiReponse<>(user, HttpStatus.OK, true));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MvcApiReponse<UserDTO>> get(@PathVariable("id") Long id) {
        var user = employeeService.getById(id);
        return ResponseEntity.ok(new MvcApiReponse<>(user, HttpStatus.OK, true));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MvcApiReponse<Object>> delete(@PathVariable("id") Long id) {
        employeeService.delete(id);
        return ResponseEntity.ok(new MvcApiReponse<>(null, HttpStatus.OK, true));
    }

    @GetMapping
    public ResponseEntity<MvcApiReponse<List<UserDTO>>> getAll(@QueryParam("page") Integer page, @QueryParam("size") Integer size) {
        var users = employeeService.getAllUsers(page, size);
        return ResponseEntity.ok(new MvcApiReponse<>(users, HttpStatus.OK, true));
    }
}
