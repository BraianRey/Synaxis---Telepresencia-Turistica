package com.synexis.management_service.service;

import com.synexis.management_service.dto.response.LoginResponse;

public interface LoginService {

    LoginResponse loginClient(String email, String password);

    LoginResponse loginPartner(String email, String password);
}

