package com.sismptm.partner.ui.features.auth

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sismptm.partner.R
import com.sismptm.partner.core.network.RetrofitClient
import com.sismptm.partner.domain.validation.RegisterValidator
import com.sismptm.partner.ui.common.ProfilePictureUpload
import com.sismptm.partner.ui.theme.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

private data class CityOption(val label: String, val longitude: Double, val latitude: Double)

private val cityOptions = listOf(
    CityOption("Popayán", -76.6134, 2.4382),
    CityOption("Cali", -76.5320, 3.4516),
    CityOption("Medellín", -75.5636, 6.2518),
    CityOption("Bogotá", -74.0721, 4.7110)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: RegisterViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf<CityOption?>(null) }
    var areaExpanded by remember { mutableStateOf(false) }
    var acceptedTerms by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val emailHasError = email.isNotBlank() && !RegisterValidator.isValidEmail(email)
    val passwordMismatch = confirmPassword.isNotBlank() && !RegisterValidator.doPasswordsMatch(password, confirmPassword)

    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState is RegisterViewModel.RegisterUiState.Loading || isUploading

    val isFormValid = RegisterValidator.isFormValid(
        fullName = name,
        email = email,
        password = password,
        confirmPassword = confirmPassword,
        acceptedTerms = acceptedTerms
    ) && selectedCity != null

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            uploadError = null
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is RegisterViewModel.RegisterUiState.Success) {
            onRegisterSuccess()
            viewModel.resetState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 24.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.register_title),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.register_subtitle),
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        ProfilePictureUpload(
            onPhotoClick = { imagePickerLauncher.launch("image/*") },
            selectedImageUri = selectedImageUri
        )

        if (uploadError != null) {
            Text(
                text = uploadError!!,
                color = Error,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(id = R.string.full_name), color = TextTertiary) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BorderFocus,
                unfocusedBorderColor = BorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedLabelColor = TextTertiary,
                unfocusedLabelColor = TextTertiary,
                focusedContainerColor = InputBackground,
                unfocusedContainerColor = InputBackground
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(id = R.string.email), color = TextTertiary) },
            isError = emailHasError,
            supportingText = { if (emailHasError) Text(stringResource(R.string.invalid_email), color = Error) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BorderFocus,
                unfocusedBorderColor = BorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedLabelColor = TextTertiary,
                unfocusedLabelColor = TextTertiary,
                focusedContainerColor = InputBackground,
                unfocusedContainerColor = InputBackground,
                errorBorderColor = Error
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(id = R.string.password), color = TextTertiary) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BorderFocus,
                unfocusedBorderColor = BorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedLabelColor = TextTertiary,
                unfocusedLabelColor = TextTertiary,
                focusedContainerColor = InputBackground,
                unfocusedContainerColor = InputBackground
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text(stringResource(id = R.string.confirm_password), color = TextTertiary) },
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }
            },
            isError = passwordMismatch,
            supportingText = { if (passwordMismatch) Text(stringResource(R.string.passwords_do_not_match), color = Error) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BorderFocus,
                unfocusedBorderColor = BorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedLabelColor = TextTertiary,
                unfocusedLabelColor = TextTertiary,
                focusedContainerColor = InputBackground,
                unfocusedContainerColor = InputBackground,
                errorBorderColor = Error
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        ExposedDropdownMenuBox(
            expanded = areaExpanded,
            onExpandedChange = { areaExpanded = !areaExpanded }
        ) {
            OutlinedTextField(
                value = selectedCity?.label.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(id = R.string.select_area), color = TextTertiary) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = areaExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BorderFocus,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedLabelColor = TextTertiary,
                    unfocusedLabelColor = TextTertiary,
                    focusedContainerColor = InputBackground,
                    unfocusedContainerColor = InputBackground
                )
            )
            ExposedDropdownMenu(
                expanded = areaExpanded,
                onDismissRequest = { areaExpanded = false },
                modifier = Modifier.background(CardBackground)
            ) {
                cityOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label, color = TextPrimary) },
                        onClick = {
                            selectedCity = option
                            areaExpanded = false
                        },
                        modifier = Modifier.background(CardBackground)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(
                checked = acceptedTerms,
                onCheckedChange = { acceptedTerms = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = PrimaryAccent,
                    uncheckedColor = BorderSubtle
                )
            )
            Text(
                text = stringResource(id = R.string.accept_terms),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState is RegisterViewModel.RegisterUiState.Error) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Text(
                    text = (uiState as RegisterViewModel.RegisterUiState.Error).message,
                    color = ErrorLight,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Button(
            onClick = {
                scope.launch {
                    val uri = selectedImageUri
                    var picDirectory: String? = null

                    if (uri != null) {
                        isUploading = true
                        try {
                            // Decode original image
                            val inputStream = context.contentResolver.openInputStream(uri)
                            val originalBitmap = BitmapFactory.decodeStream(inputStream)
                            inputStream?.close()

                            if (originalBitmap == null) {
                                throw IllegalStateException("Could not decode image")
                            }

                            // Resize if larger than 800px on any dimension
                            val maxDimension = 800
                            val scaleRatio = minOf(
                                maxDimension.toFloat() / originalBitmap.width,
                                maxDimension.toFloat() / originalBitmap.height,
                                1.0f
                            )
                            val resizedBitmap = if (scaleRatio < 1.0f) {
                                val newWidth = (originalBitmap.width * scaleRatio).toInt()
                                val newHeight = (originalBitmap.height * scaleRatio).toInt()
                                Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
                            } else {
                                originalBitmap
                            }

                            // Compress to JPEG 80%
                            val baos = ByteArrayOutputStream()
                            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                            val compressedBytes = baos.toByteArray()
                            baos.close()

                            val tempFile = File(context.cacheDir, "profile_upload_${System.currentTimeMillis()}.jpg")
                            tempFile.writeBytes(compressedBytes)

                            val requestBody = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                            val part = MultipartBody.Part.createFormData("file", tempFile.name, requestBody)
                            val uploadResponse = RetrofitClient.apiService.uploadProfilePicture(part)
                            if (uploadResponse.isSuccessful) {
                                picDirectory = uploadResponse.body()?.picDirectory
                            }
                            tempFile.delete()
                        } catch (e: Exception) {
                            uploadError = "Image upload failed: ${e.localizedMessage ?: "Unknown error"}. Registration will continue without photo."
                        } finally {
                            isUploading = false
                        }
                    }

                    viewModel.register(
                        name = name,
                        email = email,
                        password = password,
                        longitude = selectedCity?.longitude ?: 0.0,
                        latitude = selectedCity?.latitude ?: 0.0,
                        termsAccepted = acceptedTerms,
                        picDirectory = picDirectory
                    )
                }
            },
            enabled = isFormValid && !isLoading,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = TextPrimary, strokeWidth = 2.dp)
            } else {
                Text(text = stringResource(id = R.string.register_button), fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text(text = stringResource(id = R.string.already_have_account), color = PrimaryAccent)
        }
    }
}
