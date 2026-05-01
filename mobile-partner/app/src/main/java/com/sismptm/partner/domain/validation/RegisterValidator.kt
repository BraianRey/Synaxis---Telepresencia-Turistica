package com.sismptm.partner.domain.validation

/**
 * Pure business logic for validating registration form data.
 */
object RegisterValidator {

    private const val MIN_PASSWORD_LENGTH = 8
    private val emailRegex = Regex(
        pattern = "^[A-Za-z0-9](?:[A-Za-z0-9._%+-]{0,62}[A-Za-z0-9])?@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z]{2,})+$"
    )

    fun isValidFullName(fullName: String): Boolean = fullName.trim().isNotEmpty()

    fun isValidEmail(email: String): Boolean = emailRegex.matches(email.trim())

    fun isValidPassword(password: String): Boolean = 
        password.isNotBlank() && password.length >= MIN_PASSWORD_LENGTH

    fun doPasswordsMatch(password: String, confirmPassword: String): Boolean = 
        confirmPassword.isNotEmpty() && password == confirmPassword

    /**
     * Validates the form without password confirmation.
     */
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

    /**
     * Validates the full registration form including password confirmation.
     */
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
