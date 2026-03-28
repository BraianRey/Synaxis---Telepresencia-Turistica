package com.sismptm.partner.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisterFormValidatorTest {

    @Test
    fun isValidEmail_acceptsTrimmedValidEmail() {
        assertTrue(RegisterFormValidator.isValidEmail("  partner@example.com  "))
    }

    @Test
    fun isValidEmail_rejectsInvalidEmail() {
        assertFalse(RegisterFormValidator.isValidEmail("partner.example.com"))
    }

    @Test
    fun isValidPassword_rejectsBlankOrShortPassword() {
        assertFalse(RegisterFormValidator.isValidPassword("        "))
        assertFalse(RegisterFormValidator.isValidPassword("short7"))
    }

    @Test
    fun isValidPassword_acceptsPasswordWithExactMinimumLength() {
        assertTrue(RegisterFormValidator.isValidPassword("abcd1234"))
    }

    @Test
    fun doPasswordsMatch_rejectsEmptyConfirmation() {
        assertFalse(RegisterFormValidator.doPasswordsMatch("segura123", ""))
    }

    @Test
    fun isFormValid_basicOverload_acceptsValidData() {
        assertTrue(
            RegisterFormValidator.isFormValid(
                fullName = "Ana Pérez",
                email = "ana@example.com",
                password = "segura123",
                acceptedTerms = true
            )
        )
    }

    @Test
    fun isFormValid_basicOverload_rejectsWhenTermsAreNotAccepted() {
        assertFalse(
            RegisterFormValidator.isFormValid(
                fullName = "Ana Pérez",
                email = "ana@example.com",
                password = "segura123",
                acceptedTerms = false
            )
        )
    }

    @Test
    fun isFormValid_fullOverload_acceptsCompleteValidForm() {
        assertTrue(
            RegisterFormValidator.isFormValid(
                fullName = "Ana Pérez",
                email = "ana@example.com",
                password = "segura123",
                confirmPassword = "segura123",
                acceptedTerms = true
            )
        )
    }

    @Test
    fun isFormValid_fullOverload_rejectsWhenPasswordsDoNotMatch() {
        assertFalse(
            RegisterFormValidator.isFormValid(
                fullName = "Ana Pérez",
                email = "ana@example.com",
                password = "segura123",
                confirmPassword = "segura124",
                acceptedTerms = true
            )
        )
    }

    @Test
    fun isFormValid_fullOverload_rejectsInvalidEmail() {
        assertFalse(
            RegisterFormValidator.isFormValid(
                fullName = "Ana Pérez",
                email = "ana.example.com",
                password = "segura123",
                confirmPassword = "segura123",
                acceptedTerms = true
            )
        )
    }

    @Test
    fun isFormValid_fullOverload_rejectsWhenTermsAreNotAccepted() {
        assertFalse(
            RegisterFormValidator.isFormValid(
                fullName = "Ana Pérez",
                email = "ana@example.com",
                password = "segura123",
                confirmPassword = "segura123",
                acceptedTerms = false
            )
        )
    }

    @Test
    fun isFormValid_fullOverload_rejectsBlankFullNameEvenWithSpaces() {
        assertFalse(
            RegisterFormValidator.isFormValid(
                fullName = "   ",
                email = "ana@example.com",
                password = "segura123",
                confirmPassword = "segura123",
                acceptedTerms = true
            )
        )
    }
}


