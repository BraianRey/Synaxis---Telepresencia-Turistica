package com.sismptm.client.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sismptm.client.R
import com.sismptm.client.data.remote.LoginRequest
import com.sismptm.client.data.remote.RetrofitClient
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.headlineLarge
        )
        
        Spacer(modifier = Modifier.height(32.dp))

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

        Spacer(modifier = Modifier.height(24.dp))

        // Botón de Ping para probar conexión
        OutlinedButton(
            onClick = {
                scope.launch {
                    try {
                        val response = RetrofitClient.apiService.ping()
                        if (response.isSuccessful) {
                            val body = response.body()
                            Toast.makeText(context, "Ping OK: ${body?.status}", Toast.LENGTH_SHORT).show()
                            println("Ping exitoso: ${body?.status} a las ${body?.timestamp}")
                        } else {
                            Toast.makeText(context, "Error Ping: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Fallo conexión: ${e.message}", Toast.LENGTH_LONG).show()
                        println("Error de red: ${e.message}")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Probar Conexión (Ping)")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    try {
                        val response = RetrofitClient.apiService.loginUser(LoginRequest(email, password))
                        if (response.isSuccessful) {
                            println("Successful connection: Login successful")
                        } else {
                            println("Connection error: ${response.code()} - Login failed (proceeding anyway for testing)")
                        }
                    } catch (e: Exception) {
                        println("Connection failure: ${e.message} - Login failed (proceeding anyway for testing)")
                    } finally {
                        isLoading = false
                        onLoginSuccess()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Ingresar")
            }
        }

        TextButton(onClick = onNavigateToRegister) {
            Text(stringResource(id = R.string.already_have_account))
        }
    }
}
