package com.sismptm.client.domain.validation

/**
 * Business logic for validating the registration form data.
 */
object RegisterValidator {
    const val MIN_PASSWORD_LENGTH = 8
    
    private val emailRegex = Regex(
        pattern = "^[A-Za-z0-9](?:[A-Za-z0-9._%+-]{0,62}[A-Za-z0-9])?@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z]{2,})+$"
    )

    /**
     * Checks if the provided email string follows a valid email format.
     */
    fun isValidEmail(email: String): Boolean {
        return emailRegex.matches(email.trim())
    }

    /**
     * Validates the entire registration form.
     */
    fun isFormValid(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String,
        acceptedTerms: Boolean
    ): Boolean {
        return acceptedTerms &&
            fullName.isNotBlank() &&
            isValidEmail(email) &&
            password.length >= MIN_PASSWORD_LENGTH &&
            password == confirmPassword
    }
}
