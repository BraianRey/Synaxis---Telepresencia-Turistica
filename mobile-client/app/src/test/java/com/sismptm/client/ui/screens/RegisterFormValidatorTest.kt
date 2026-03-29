package com.sismptm.client.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

//Pruebas unitarias para el RegisterFormValidator

class RegisterFormValidatorTest {

    @Test
    fun isValidEmail_returnsTrue_whenEmailHasValidFormat() {
        val isValid = RegisterFormValidator.isValidEmail("david.perez+turismo@example.com")

        assertTrue(isValid)
    }

    @Test
    fun isValidEmail_returnsFalse_whenEmailHasInvalidFormat() {
        val isValid = RegisterFormValidator.isValidEmail("david@@example")

        assertFalse(isValid)
    }

    @Test
    fun isFormValid_returnsTrue_whenFullNameEmailPasswordAndTermsAreValid() {
        val isValid = RegisterFormValidator.isFormValid(
            fullName = "David Perez",
            email = "david@example.com",
            password = "12345678",
            confirmPassword = "12345678",
            acceptedTerms = true
        )

        assertTrue(isValid)
    }

    @Test
    fun isFormValid_returnsFalse_whenFullNameIsBlank() {
        val isValid = RegisterFormValidator.isFormValid(
            fullName = "   ",
            email = "david@example.com",
            password = "12345678",
            confirmPassword = "12345678",
            acceptedTerms = true
        )

        assertFalse(isValid)
    }

    @Test
    fun isFormValid_returnsFalse_whenPasswordIsTooShort() {
        val isValid = RegisterFormValidator.isFormValid(
            fullName = "David Perez",
            email = "david@example.com",
            password = "1234567",
            confirmPassword = "1234567",
            acceptedTerms = true
        )

        assertFalse(isValid)
    }

    @Test
    fun isFormValid_returnsFalse_whenEmailFormatIsInvalid() {
        val isValid = RegisterFormValidator.isFormValid(
            fullName = "David Perez",
            email = "david.example.com",
            password = "12345678",
            confirmPassword = "12345678",
            acceptedTerms = true
        )

        assertFalse(isValid)
    }

    @Test
    fun isFormValid_returnsFalse_whenTermsAreNotAccepted() {
        val isValid = RegisterFormValidator.isFormValid(
            fullName = "David Perez",
            email = "david@example.com",
            password = "12345678",
            confirmPassword = "12345678",
            acceptedTerms = false
        )

        assertFalse(isValid)
    }

    @Test
    fun isFormValid_returnsFalse_whenPasswordsDoNotMatch() {
        val isValid = RegisterFormValidator.isFormValid(
            fullName = "David Perez",
            email = "david@example.com",
            password = "12345678",
            confirmPassword = "87654321",
            acceptedTerms = true
        )

        assertFalse(isValid)
    }

    @Test
    fun isFormValid_returnsFalse_whenConfirmPasswordIsBlank() {
        val isValid = RegisterFormValidator.isFormValid(
            fullName = "David Perez",
            email = "david@example.com",
            password = "12345678",
            confirmPassword = "",
            acceptedTerms = true
        )

        assertFalse(isValid)
    }
}

