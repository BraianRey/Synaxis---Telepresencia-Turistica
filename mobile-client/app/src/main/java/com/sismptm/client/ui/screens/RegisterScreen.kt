package com.sismptm.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sismptm.client.R
import com.sismptm.client.ui.components.ProfilePictureUpload

/**
 * Screen that handles the registration of a new user.
 * @param onRegisterSuccess Callback triggered upon successful registration.
 * @param onNavigateToLogin Callback to navigate back to the login screen.
 */
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: RegisterViewModel = viewModel()
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var acceptedTerms by remember { mutableStateOf(false) }
    val emailHasError = email.isNotBlank() && !RegisterFormValidator.isValidEmail(email)
    val passwordMismatch = confirmPassword.isNotBlank() && password != confirmPassword

    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState is RegisterViewModel.RegisterUiState.Loading

    // Navegar al home cuando el registro sea exitoso
    LaunchedEffect(uiState) {
        if (uiState is RegisterViewModel.RegisterUiState.Success) {
            onRegisterSuccess()
            viewModel.resetState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 24.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.register_title),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.register_subtitle),
                fontSize = 14.sp,
                color = Color(0xFF9E9E9E)
            )
        }

        ProfilePictureUpload(
            onPhotoClick = { },
            modifier = Modifier.padding(vertical = 24.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            placeholder = { Text(stringResource(R.string.full_name_placeholder)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1E88E5),
                unfocusedBorderColor = Color(0xFFE0E0E0)
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text(stringResource(R.string.email_placeholder)) },
            isError = emailHasError,
            supportingText = {
                if (emailHasError) {
                    Text(text = stringResource(R.string.invalid_email))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1E88E5),
                unfocusedBorderColor = Color(0xFFE0E0E0)
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text(stringResource(R.string.password_placeholder)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1E88E5),
                unfocusedBorderColor = Color(0xFFE0E0E0)
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            placeholder = { Text(stringResource(R.string.confirm_password)) },
            visualTransformation = PasswordVisualTransformation(),
            isError = passwordMismatch,
            supportingText = {
                if (passwordMismatch) {
                    Text(text = stringResource(R.string.passwords_do_not_match))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1E88E5),
                unfocusedBorderColor = Color(0xFFE0E0E0)
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Checkbox(
                checked = acceptedTerms,
                onCheckedChange = { acceptedTerms = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF1E88E5)
                )
            )
            Text(
                text = stringResource(R.string.accept_terms),
                fontSize = 13.sp,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Mostrar error si la llamada al backend falla
        if (uiState is RegisterViewModel.RegisterUiState.Error) {
            val errorMsg = (uiState as RegisterViewModel.RegisterUiState.Error).message
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = errorMsg,
                    color = Color(0xFFC62828),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Button(
            onClick = {
                viewModel.register(
                    name = fullName,
                    email = email,
                    password = password,
                    termsAccepted = acceptedTerms
                )
            },
            enabled = RegisterFormValidator.isFormValid(
                fullName = fullName,
                email = email,
                password = password,
                confirmPassword = confirmPassword,
                acceptedTerms = acceptedTerms
            ) && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E88E5),
                disabledContainerColor = Color(0xFFBBDEFB)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = stringResource(R.string.get_started),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.already_user),
                fontSize = 14.sp,
                color = Color(0xFF9E9E9E)
            )
            Text(
                text = stringResource(R.string.sign_in),
                fontSize = 14.sp,
                color = Color(0xFF1E88E5),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onNavigateToLogin)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
