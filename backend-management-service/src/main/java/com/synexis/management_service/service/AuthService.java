package com.synexis.management_service.service;

public interface AuthService {

    public String encodePassword(String rawPassword);
    public boolean passwordMatches(String rawPassword, String storedHash);

}
