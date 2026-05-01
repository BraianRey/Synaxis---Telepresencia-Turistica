package com.sismptm.client.domain.validation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

//Pruebas unitarias para el RegisterValidator

class RegisterValidatorTest {

    @Test
    fun isValidEmail_returnsTrue_whenEmailHasValidFormat() {
        val isValid = RegisterValidator.isValidEmail("david.perez+turismo@example.com")

        assertTrue(isValid)
    }

    @Test
    fun isValidEmail_returnsFalse_whenEmailHasInvalidFormat() {
        val isValid = RegisterValidator.isValidEmail("david@@example")

        assertFalse(isValid)
    }

    @Test
    fun isFormValid_returnsTrue_whenFullNameEmailPasswordAndTermsAreValid() {
        val isValid = RegisterValidator.isFormValid(
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
        val isValid = RegisterValidator.isFormValid(
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
        val isValid = RegisterValidator.isFormValid(
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
        val isValid = RegisterValidator.isFormValid(
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
        val isValid = RegisterValidator.isFormValid(
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
        val isValid = RegisterValidator.isFormValid(
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
        val isValid = RegisterValidator.isFormValid(
            fullName = "David Perez",
            email = "david@example.com",
            password = "12345678",
            confirmPassword = "",
            acceptedTerms = true
        )

        assertFalse(isValid)
    }
}


