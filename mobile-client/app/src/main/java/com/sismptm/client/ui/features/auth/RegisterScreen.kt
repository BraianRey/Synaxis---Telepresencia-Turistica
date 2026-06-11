package com.sismptm.client.ui.features.auth

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import android.content.Context
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
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
import com.sismptm.client.R
import com.sismptm.client.domain.validation.RegisterValidator
import com.sismptm.client.ui.common.ProfilePictureUpload
import com.sismptm.client.ui.theme.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

private data class RegisterFormState(
    val fullName: String,
    val onFullNameChange: (String) -> Unit,
    val email: String,
    val onEmailChange: (String) -> Unit,
    val emailHasError: Boolean,
    val password: String,
    val onPasswordChange: (String) -> Unit,
    val passwordVisible: Boolean,
    val onTogglePasswordVisible: () -> Unit,
    val confirmPassword: String,
    val onConfirmPasswordChange: (String) -> Unit,
    val confirmPasswordVisible: Boolean,
    val onToggleConfirmPasswordVisible: () -> Unit,
    val passwordMismatch: Boolean,
    val acceptedTerms: Boolean,
    val onAcceptedTermsChange: (Boolean) -> Unit,
    val focusManager: FocusManager
)

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
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showPhotoOptions by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val emailHasError = email.isNotBlank() && !RegisterValidator.isValidEmail(email)
    val passwordMismatch = confirmPassword.isNotBlank() && password != confirmPassword

    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState is RegisterViewModel.RegisterUiState.Loading || isUploading

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            selectedImageBitmap = null
            uploadError = null
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            selectedImageBitmap = it
            selectedImageUri = null
            uploadError = null
        }
    }

    // Navigate to home when registration is successful
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
            .imePadding()
            .padding(top = 32.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RegisterHeader()

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ProfileSection(
                onPhotoClick = { showPhotoOptions = true },
                selectedImageUri = selectedImageUri,
                selectedImageBitmap = selectedImageBitmap,
                uploadError = uploadError
            )
            DropdownMenu(
                expanded = showPhotoOptions,
                onDismissRequest = { showPhotoOptions = false },
                modifier = Modifier.wrapContentSize()
            ) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.choose_from_gallery)) },
                    onClick = {
                        showPhotoOptions = false
                        imagePickerLauncher.launch("image/*")
                    }
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.take_photo)) },
                    onClick = {
                        showPhotoOptions = false
                        cameraLauncher.launch(null)
                    }
                )
            }
        }

        if (uploadError != null) {
            Text(
                text = uploadError!!,
                color = Error,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        RegisterForm(
            state = RegisterFormState(
                fullName = fullName,
                onFullNameChange = { fullName = it },
                email = email,
                onEmailChange = { email = it },
                emailHasError = emailHasError,
                password = password,
                onPasswordChange = { password = it },
                passwordVisible = passwordVisible,
                onTogglePasswordVisible = { passwordVisible = !passwordVisible },
                confirmPassword = confirmPassword,
                onConfirmPasswordChange = { confirmPassword = it },
                confirmPasswordVisible = confirmPasswordVisible,
                onToggleConfirmPasswordVisible = { confirmPasswordVisible = !confirmPasswordVisible },
                passwordMismatch = passwordMismatch,
                acceptedTerms = acceptedTerms,
                onAcceptedTermsChange = { acceptedTerms = it },
                focusManager = focusManager
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        RegisterErrorCard(uiState = uiState)

        RegisterActionButton(
            isLoading = isLoading,
            enabled = RegisterValidator.isFormValid(
                fullName = fullName,
                email = email,
                password = password,
                confirmPassword = confirmPassword,
                acceptedTerms = acceptedTerms
            ) && !isLoading,
            onClick = {
                scope.launch {
                    handleRegisterClick(
                        context,
                        selectedImageUri = selectedImageUri,
                        selectedImageBitmap = selectedImageBitmap,
                        onUploadError = { uploadError = it },
                        onUploadingChange = { isUploading = it },
                        viewModel = viewModel,
                        fullName = fullName,
                        email = email,
                        password = password,
                        acceptedTerms = acceptedTerms
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        RegisterFooter(onNavigateToLogin = onNavigateToLogin)

        Spacer(modifier = Modifier.height(20.dp))
    }
}

private suspend fun handleRegisterClick(
    context: Context,
    selectedImageUri: Uri?,
    selectedImageBitmap: Bitmap?,
    onUploadError: (String?) -> Unit,
    onUploadingChange: (Boolean) -> Unit,
    viewModel: RegisterViewModel,
    fullName: String,
    email: String,
    password: String,
    acceptedTerms: Boolean
) {
    val profilePictureBase64 = if (selectedImageUri != null || selectedImageBitmap != null) {
        onUploadingChange(true)
        try {
            prepareProfilePictureBase64(context, selectedImageUri, selectedImageBitmap)
        } catch (e: Exception) {
            Log.e("RegisterDebug", "Image processing exception", e)
            onUploadError("Could not prepare profile image. Registration will continue without photo.")
            null
        } finally {
            onUploadingChange(false)
        }
    } else {
        null
    }

    viewModel.register(
        name = fullName,
        email = email,
        password = password,
        termsAccepted = acceptedTerms,
        picDirectory = null,
        profilePictureBase64 = profilePictureBase64
    )
}

private suspend fun prepareProfilePictureBase64(
    context: Context,
    selectedImageUri: Uri?,
    selectedImageBitmap: Bitmap?
): String? {
    val originalBitmap = when {
        selectedImageBitmap != null -> selectedImageBitmap
        selectedImageUri != null -> {
            val inputStream = context.contentResolver.openInputStream(selectedImageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bitmap
        }
        else -> null
    } ?: return null

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

    val baos = ByteArrayOutputStream()
    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
    val compressedBytes = baos.toByteArray()
    baos.close()

    return Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
}

@Composable
private fun ProfileSection(
    onPhotoClick: () -> Unit,
    selectedImageUri: Uri?,
    selectedImageBitmap: Bitmap?,
    uploadError: String?
) {
    ProfilePictureUpload(
        onPhotoClick = onPhotoClick,
        selectedImageUri = selectedImageUri,
        selectedImageBitmap = selectedImageBitmap,
        modifier = Modifier.padding(vertical = 24.dp)
    )
    UploadErrorMessage(uploadError)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RegisterForm(state: RegisterFormState) {
    val fullNameBringIntoViewRequester = remember { BringIntoViewRequester() }
    val emailBringIntoViewRequester = remember { BringIntoViewRequester() }
    val passwordBringIntoViewRequester = remember { BringIntoViewRequester() }
    val confirmPasswordBringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    RegisterInputField(
        value = state.fullName,
        onValueChange = state.onFullNameChange,
        placeholderResId = R.string.full_name_placeholder,
        modifier = Modifier.bringIntoViewRequester(fullNameBringIntoViewRequester)
            .onFocusChanged { if (it.isFocused) scope.launch { fullNameBringIntoViewRequester.bringIntoView() } },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { state.focusManager.moveFocus(FocusDirection.Down) }
        )
    )

    Spacer(modifier = Modifier.height(12.dp))

    RegisterInputField(
        value = state.email,
        onValueChange = state.onEmailChange,
        placeholderResId = R.string.email_placeholder,
        modifier = Modifier.bringIntoViewRequester(emailBringIntoViewRequester)
            .onFocusChanged { if (it.isFocused) scope.launch { emailBringIntoViewRequester.bringIntoView() } },
        isError = state.emailHasError,
        supportingText = if (state.emailHasError) {
            {
                Text(text = stringResource(R.string.invalid_email), color = Error)
            }
        } else null,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { state.focusManager.moveFocus(FocusDirection.Down) }
        )
    )

    Spacer(modifier = Modifier.height(12.dp))

    RegisterPasswordField(
        value = state.password,
        onValueChange = state.onPasswordChange,
        placeholderResId = R.string.password_placeholder,
        modifier = Modifier.bringIntoViewRequester(passwordBringIntoViewRequester)
            .onFocusChanged { if (it.isFocused) scope.launch { passwordBringIntoViewRequester.bringIntoView() } },
        passwordVisible = state.passwordVisible,
        onVisibilityToggle = state.onTogglePasswordVisible,
        keyboardActions = KeyboardActions(
            onNext = { state.focusManager.moveFocus(FocusDirection.Down) }
        )
    )

    Spacer(modifier = Modifier.height(12.dp))

    RegisterPasswordField(
        value = state.confirmPassword,
        onValueChange = state.onConfirmPasswordChange,
        placeholderResId = R.string.confirm_password,
        modifier = Modifier.bringIntoViewRequester(confirmPasswordBringIntoViewRequester)
            .onFocusChanged { if (it.isFocused) scope.launch { confirmPasswordBringIntoViewRequester.bringIntoView() } },
        passwordVisible = state.confirmPasswordVisible,
        onVisibilityToggle = state.onToggleConfirmPasswordVisible,
        isError = state.passwordMismatch,
        supportingText = if (state.passwordMismatch) {
            {
                Text(text = stringResource(R.string.passwords_do_not_match), color = Error)
            }
        } else null,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { state.focusManager.clearFocus() }
        )
    )

    Spacer(modifier = Modifier.height(20.dp))

    TermsAcceptanceRow(
        acceptedTerms = state.acceptedTerms,
        onCheckedChange = state.onAcceptedTermsChange
    )
}

@Composable
private fun RegisterHeader() {
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
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.register_subtitle),
            fontSize = 14.sp,
            color = TextTertiary
        )
    }
}

@Composable
private fun UploadErrorMessage(uploadError: String?) {
    if (uploadError == null) return

    Text(
        text = uploadError,
        color = Error,
        fontSize = 12.sp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Suppress("LongParameterList")
@Composable
private fun RegisterInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholderResId: Int,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(stringResource(placeholderResId), color = TextTertiary) },
        isError = isError,
        supportingText = supportingText,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(lineHeight = 20.sp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryAccent,
            unfocusedBorderColor = BorderSubtle,
            focusedContainerColor = CardBackground,
            unfocusedContainerColor = CardBackground,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        )
    )
}

@Suppress("LongParameterList")
@Composable
private fun RegisterPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholderResId: Int,
    passwordVisible: Boolean,
    onVisibilityToggle: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    RegisterInputField(
        value = value,
        onValueChange = onValueChange,
        placeholderResId = placeholderResId,
        modifier = modifier,
        isError = isError,
        supportingText = supportingText,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onVisibilityToggle) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    tint = TextTertiary
                )
            }
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions
    )
}

@Composable
private fun TermsAcceptanceRow(
    acceptedTerms: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Checkbox(
            checked = acceptedTerms,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = PrimaryAccent,
                uncheckedColor = TextTertiary
            )
        )
        Text(
            text = stringResource(R.string.accept_terms),
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RegisterErrorCard(uiState: RegisterViewModel.RegisterUiState) {
    if (uiState !is RegisterViewModel.RegisterUiState.Error) return

    Card(
        colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.1f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Text(
            text = uiState.message,
            color = Error,
            fontSize = 13.sp,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun RegisterActionButton(
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryAccent,
            disabledContainerColor = PrimaryAccent.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = TextPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = stringResource(R.string.get_started),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun RegisterFooter(onNavigateToLogin: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.already_user),
            fontSize = 14.sp,
            color = TextTertiary
        )
        Text(
            text = stringResource(R.string.sign_in),
            fontSize = 14.sp,
            color = PrimaryAccent,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onNavigateToLogin)
        )
    }
}
