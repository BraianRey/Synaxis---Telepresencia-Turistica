package com.synexis.management_service.service;

import com.synexis.management_service.dto.RegisterRequest;
import com.synexis.management_service.dto.RegisterResponse;
import com.synexis.management_service.exception.EmailAlreadyExistsException;
import com.synexis.management_service.models.User;
import com.synexis.management_service.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates authentication-related business logic (registration for now). Delegates persistence to
 * {@link UserRepository} and password hashing to Spring Security's {@link PasswordEncoder} (BCrypt).
 *
 * <p>How it works: {@link #register(RegisterRequest)} normalizes email, rejects duplicates, encodes the password,
 * saves a new {@link User}, and maps the result to {@link RegisterResponse}. Runs in a transaction so save + read
 * stay consistent.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user. Throws {@link EmailAlreadyExistsException} if the email is already taken (handled as
     * {@code 409} by {@link com.synexis.management_service.exception.RestExceptionHandler RestExceptionHandler}).
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setProfilePicturePath(normalizeProfilePicturePath(request.profilePicturePath()));

        User saved = userRepository.save(user);
        return new RegisterResponse(
                saved.getId(), saved.getEmail(), saved.getRole(), saved.getProfilePicturePath());
    }

    /** Trims and keeps null/empty as absent; callers may send paths like {@code picProfile/avatar.png}. */
    private static String normalizeProfilePicturePath(String path) {
        if (path == null) {
            return null;
        }
        String t = path.trim();
        return t.isEmpty() ? null : t;
    }
}
