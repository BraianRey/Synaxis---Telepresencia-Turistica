package com.sismptm.partner.ui.features.auth

object RegisterFormValidator {

    fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        return trimmed.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()
    }

    fun isValidPassword(password: String): Boolean {
        return password.trim().length >= 8
    }

    fun doPasswordsMatch(password: String, confirmPassword: String): Boolean {
        return password == confirmPassword && confirmPassword.isNotEmpty()
    }

    fun isFormValid(
        fullName: String,
        email: String,
        password: String,
        acceptedTerms: Boolean
    ): Boolean {
        return fullName.trim().isNotEmpty()
                && isValidEmail(email)
                && isValidPassword(password)
                && acceptedTerms
    }

    fun isFormValid(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String,
        acceptedTerms: Boolean
    ): Boolean {
        return fullName.trim().isNotEmpty()
                && isValidEmail(email)
                && isValidPassword(password)
                && doPasswordsMatch(password, confirmPassword)
                && acceptedTerms
    }
}
