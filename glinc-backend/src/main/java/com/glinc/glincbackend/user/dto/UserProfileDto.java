package com.glinc.glincbackend.user.dto;

import java.time.Instant;
import java.time.LocalDate;

public class UserProfileDto {

    private String email;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String phone;
    // null si el usuario aun no ha elegido rol; el frontend lo usa para el modal.
    private String role;
    private Instant updatedAt;

    public UserProfileDto() {
    }

    public UserProfileDto(String email, String firstName, String lastName,
                          LocalDate birthDate, String phone, String role,
                          Instant updatedAt) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.phone = phone;
        this.role = role;
        this.updatedAt = updatedAt;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
