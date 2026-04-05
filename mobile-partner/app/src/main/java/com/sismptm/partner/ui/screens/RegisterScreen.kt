package com.sismptm.partner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sismptm.partner.R
import com.sismptm.partner.location.LocationService

/**
 * Screen for new partner registration.
 */
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: RegisterViewModel = viewModel()
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var areaIdText by remember { mutableStateOf("") }
    var acceptedTerms by remember { mutableStateOf(false) }

    val emailHasError = email.isNotBlank() && !RegisterFormValidator.isValidEmail(email)
    val passwordMismatch = confirmPassword.isNotBlank() && password != confirmPassword
    val areaId = areaIdText.toIntOrNull()

    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState is RegisterViewModel.RegisterUiState.Loading

    val isFormValid = RegisterFormValidator.isFormValid(
        fullName = name,
        email = email,
        password = password,
        confirmPassword = confirmPassword,
        acceptedTerms = acceptedTerms
    ) && areaId != null

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
        Text(
            text = stringResource(id = R.string.register_title),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.register_subtitle),
            fontSize = 14.sp,
            color = Color(0xFF9E9E9E)
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(id = R.string.full_name)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(id = R.string.email)) },
            isError = emailHasError,
            supportingText = { if (emailHasError) Text(stringResource(R.string.invalid_email)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(id = R.string.password)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text(stringResource(id = R.string.confirm_password)) },
            visualTransformation = PasswordVisualTransformation(),
            isError = passwordMismatch,
            supportingText = { if (passwordMismatch) Text(stringResource(R.string.passwords_do_not_match)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = areaIdText,
            onValueChange = { areaIdText = it.filter { c -> c.isDigit() } },
            label = { Text(stringResource(id = R.string.area_id)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = areaIdText.isNotBlank() && areaId == null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = acceptedTerms,
                onCheckedChange = { acceptedTerms = it },
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1E88E5))
            )
            Text(
                text = stringResource(id = R.string.accept_terms),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Error banner
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
                    name = name,
                    email = email,
                    password = password,
                    areaId = areaId ?: 0,
                    termsAccepted = acceptedTerms
                )
            },
            enabled = isFormValid && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1565C0),
                disabledContainerColor = Color(0xFF90CAF9)
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
                    text = stringResource(id = R.string.register_button),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text(
                text = stringResource(id = R.string.already_have_account),
                color = Color(0xFF1565C0)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
