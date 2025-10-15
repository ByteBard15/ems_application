package com.bytebard.employee.types;

public class MutateDepartmentRequest {
    private String name;

    public MutateDepartmentRequest(String name) {
        this.name = name;
    }

    public MutateDepartmentRequest() {}

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
