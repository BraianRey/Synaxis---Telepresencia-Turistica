package com.sismptm.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sismptm.client.R
import com.sismptm.client.data.remote.RegisterRequest
import com.sismptm.client.data.remote.RetrofitClient
import kotlinx.coroutines.launch
import java.util.Date

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var acceptedTerms by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    val creationDate = remember { Date().toString() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.register_title),
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(id = R.string.full_name)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(id = R.string.email)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(id = R.string.password)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text(stringResource(id = R.string.confirm_password)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = acceptedTerms,
                onCheckedChange = { acceptedTerms = it }
            )
            Text(
                text = stringResource(id = R.string.accept_terms),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    try {
                        val request = RegisterRequest(name, email, password, creationDate)
                        val response = RetrofitClient.apiService.registerUser(request)
                        if (response.isSuccessful) {
                            println("Successful connection: Registration successful")
                        } else {
                            println("Connection error: ${response.code()} - Registration failed (proceeding anyway for testing)")
                        }
                    } catch (e: Exception) {
                        println("Connection failure: ${e.message} - Registration failed (proceeding anyway for testing)")
                    } finally {
                        isLoading = false
                        onRegisterSuccess() // Navega de todas formas para pruebas
                    }
                }
            },
            enabled = acceptedTerms && !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(stringResource(id = R.string.register_button))
            }
        }

        TextButton(onClick = onNavigateToLogin) {
            Text(stringResource(id = R.string.already_have_account))
        }
    }
}
