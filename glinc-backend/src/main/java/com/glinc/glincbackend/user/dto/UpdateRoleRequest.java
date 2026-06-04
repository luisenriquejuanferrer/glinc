package com.glinc.glincbackend.user.dto;

// Body de PUT /api/user/role. role debe ser "CAREGIVER" o "DOCTOR".
public class UpdateRoleRequest {

    private String role;

    public UpdateRoleRequest() {
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
