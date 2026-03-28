package com.sismptm.partner.ui.screens

internal object RegisterFormValidator {

    const val MIN_PASSWORD_LENGTH = 8
    private val emailRegex = Regex(
        pattern = "^[A-Za-z0-9](?:[A-Za-z0-9._%+-]{0,62}[A-Za-z0-9])?@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z]{2,})+$"
    )

    fun isValidFullName(fullName: String): Boolean {
        return fullName.trim().isNotEmpty()
    }

    fun isValidEmail(email: String): Boolean {
        return emailRegex.matches(email.trim())
    }

    fun isValidPassword(password: String): Boolean {
        return password.isNotBlank() && password.length >= MIN_PASSWORD_LENGTH
    }

    fun doPasswordsMatch(password: String, confirmPassword: String): Boolean {
        return confirmPassword.isNotEmpty() && password == confirmPassword
    }

    fun isFormValid(
        fullName: String,
        email: String,
        password: String,
        acceptedTerms: Boolean
    ): Boolean {
        return acceptedTerms &&
                isValidFullName(fullName) &&
                isValidEmail(email) &&
                isValidPassword(password)
    }

    fun isFormValid(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String,
        acceptedTerms: Boolean
    ): Boolean {
        return acceptedTerms &&
                isValidFullName(fullName) &&
                isValidEmail(email) &&
                isValidPassword(password) &&
                doPasswordsMatch(password, confirmPassword)
    }

}