package com.sismptm.client.ui.screens

internal object RegisterFormValidator {
    const val MIN_PASSWORD_LENGTH = 8
    private val emailRegex = Regex(
        pattern = "^[A-Za-z0-9](?:[A-Za-z0-9._%+-]{0,62}[A-Za-z0-9])?@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z]{2,})+$"
    )

    fun isValidEmail(email: String): Boolean {
        return emailRegex.matches(email.trim())
    }

    fun isFormValid(
        fullName: String,
        email: String,
        password: String,
        acceptedTerms: Boolean
    ): Boolean {
        return acceptedTerms &&
            fullName.isNotBlank() &&
            isValidEmail(email) &&
            password.length >= MIN_PASSWORD_LENGTH
    }
}

