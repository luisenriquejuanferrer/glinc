package com.glinc.glincbackend.bridge.dto;

public class CreateSessionRequest {

    private String email;
    private String password;

    public CreateSessionRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
