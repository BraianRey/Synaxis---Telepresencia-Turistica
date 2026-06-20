package com.sismptm.client.ui.features.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.client.core.network.RetrofitClient
import com.sismptm.client.data.remote.api.dto.ServiceResponse
import com.sismptm.client.core.session.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.sismptm.client.core.events.ProfileEvents
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import com.sismptm.client.domain.model.Destination
import com.sismptm.client.domain.model.HomeUiState
import com.sismptm.client.domain.model.MapPin

class HomeViewModel : ViewModel() {

    sealed interface ClientServicesUiState {
        object Idle : ClientServicesUiState
        object Loading : ClientServicesUiState
        data class Success(val services: List<ServiceResponse>) : ClientServicesUiState
        data class Error(val message: String) : ClientServicesUiState
    }

    sealed interface ProfilePhotoUploadState {
        object Idle : ProfilePhotoUploadState
        object Loading : ProfilePhotoUploadState
        data class Error(val message: String) : ProfilePhotoUploadState
    }

    private val _photoUploadState = MutableStateFlow<ProfilePhotoUploadState>(ProfilePhotoUploadState.Idle)
    val photoUploadState: StateFlow<ProfilePhotoUploadState> = _photoUploadState.asStateFlow()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private val _servicesState = MutableStateFlow<ClientServicesUiState>(ClientServicesUiState.Idle)
    val servicesState: StateFlow<ClientServicesUiState> = _servicesState.asStateFlow()

    private val _activeServicesState = MutableStateFlow<ClientServicesUiState>(ClientServicesUiState.Idle)
    val activeServicesState: StateFlow<ClientServicesUiState> = _activeServicesState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        // Step 1: Display name immediately from local session
        val localName = SessionManager.userName
        Log.d("HomeDebug", "userName=$localName, userId=${SessionManager.userId}")
        _uiState.value = _uiState.value.copy(
            userName = if (localName.isNotBlank()) localName else "Viajero",
            isLoading = false,
            destinations = listOf(
                Destination(1, "Popayan", "Colombia", "Puente del Humilladero", 3),
                Destination(2, "Cali", "Colombia", "Cristo Rey", 2),
                Destination(3, "Medellin", "Colombia", "Comuna 13", 4),
                Destination(4, "Bogota", "Colombia", "La Candelaria", 5)
            ),
            mapPins = listOf(
                MapPin(1, "Popayan", 3, 0.3f, 0.6f),
                MapPin(2, "Cali", 2, 0.7f, 0.3f),
                MapPin(3, "Medellin", 4, 0.6f, 0.7f),
                MapPin(4, "Bogota", 5, 0.2f, 0.8f)
            )
        )

        // Step 2: Refresh from backend in background
        val apiService = RetrofitClient.apiService
        viewModelScope.launch {
            try {
                val profile = apiService.getMyProfile()
                val fullName = profile.name?.trim().takeUnless { it.isNullOrBlank() }
                _uiState.value = _uiState.value.copy(
                    userName = fullName ?: _uiState.value.userName
                )
            } catch (_: Exception) {
                // Keep local name, don't show error
            }
        }

        loadClientServices()
        loadActiveClientServices()
        // Polling must be started by the UI when the Home screen is visible to avoid
        // background network traffic when the screen is not active.
    }

    fun startPollingServices() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(5000) // Poll every 5 seconds
                loadClientServices(silent = true)
                loadActiveClientServices(silent = true)
            }
        }
    }

    fun stopPollingServices() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun loadClientServices(silent: Boolean = false) {
        val clientId = SessionManager.userId
        if (clientId == -1L) {
            if (!silent) _servicesState.value = ClientServicesUiState.Error("Session expired. Please log in again.")
            return
        }

        viewModelScope.launch {
            if (!silent) _servicesState.value = ClientServicesUiState.Loading
            runCatching {
                RetrofitClient.apiService.getServicesByClient(clientId)
            }.onSuccess { response ->
                if (response.isSuccessful) {
                    val services = response.body().orEmpty()
                        .sortedByDescending { it.serviceId }
                    _servicesState.value = ClientServicesUiState.Success(services)
                } else {
                    if (!silent) {
                        _servicesState.value = ClientServicesUiState.Error(
                            parseBackendError(response.code(), response.errorBody()?.string())
                        )
                    }
                }
            }.onFailure { ex ->
                if (!silent) {
                    _servicesState.value = ClientServicesUiState.Error(
                        ex.localizedMessage ?: "Connection error"
                    )
                }
            }
        }
    }

    fun loadActiveClientServices(silent: Boolean = false) {
        val clientId = SessionManager.userId
        if (clientId == -1L) {
            if (!silent) _activeServicesState.value = ClientServicesUiState.Error("Session expired. Please log in again.")
            return
        }

        viewModelScope.launch {
            if (!silent) _activeServicesState.value = ClientServicesUiState.Loading
            runCatching {
                RetrofitClient.apiService.getActiveServicesByClient(clientId)
            }.onSuccess { response ->
                if (response.isSuccessful) {
                    val services = response.body().orEmpty()
                        .sortedByDescending { it.serviceId }
                    _activeServicesState.value = ClientServicesUiState.Success(services)
                } else {
                    if (!silent) {
                        _activeServicesState.value = ClientServicesUiState.Error(
                            parseBackendError(response.code(), response.errorBody()?.string())
                        )
                    }
                }
            }.onFailure { ex ->
                if (!silent) {
                    _activeServicesState.value = ClientServicesUiState.Error(
                        ex.localizedMessage ?: "Connection error"
                    )
                }
            }
        }
    }

    fun updateProfilePicture(context: Context, uri: android.net.Uri) {
        viewModelScope.launch {
            _photoUploadState.value = ProfilePhotoUploadState.Loading
            val success = uploadProfilePicture(context, uri)
            if (success) {
                _photoUploadState.value = ProfilePhotoUploadState.Idle
                // Notify listeners (e.g., ProfileViewModel) to reload the picture
                try {
                    ProfileEvents.profilePictureUpdated.emit(Unit)
                } catch (ex: Exception) {
                    Log.w("HomeDebug", "Failed to emit profile update event", ex)
                }
            } else {
                _photoUploadState.value = ProfilePhotoUploadState.Error("No se pudo subir la foto. Intenta nuevamente.")
                Log.e("HomeDebug", "Failed to upload profile picture")
            }
        }
    }

    fun updateProfilePicture(context: Context, bitmap: Bitmap) {
        viewModelScope.launch {
            _photoUploadState.value = ProfilePhotoUploadState.Loading
            val success = uploadProfilePicture(context, bitmap)
            if (success) {
                _photoUploadState.value = ProfilePhotoUploadState.Idle
                // Notify listeners to reload the picture
                try {
                    ProfileEvents.profilePictureUpdated.emit(Unit)
                } catch (ex: Exception) {
                    Log.w("HomeDebug", "Failed to emit profile update event", ex)
                }
            } else {
                _photoUploadState.value = ProfilePhotoUploadState.Error("No se pudo subir la foto. Intenta nuevamente.")
                Log.e("HomeDebug", "Failed to upload profile picture from camera")
            }
        }
    }

    private suspend fun uploadProfilePicture(context: Context, uri: android.net.Uri): Boolean {
        var originalBitmap: Bitmap? = null
        var inputStream = context.contentResolver.openInputStream(uri)
        try {
            originalBitmap = BitmapFactory.decodeStream(inputStream)
        } catch (ex: Exception) {
            Log.w("HomeDebug", "decodeStream failed", ex)
        } finally {
            inputStream?.close()
        }

        if (originalBitmap == null) {
            // Try file descriptor approach
            try {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                pfd?.use { fd ->
                    originalBitmap = BitmapFactory.decodeFileDescriptor(fd.fileDescriptor)
                }
            } catch (ex: Exception) {
                Log.w("HomeDebug", "decodeFileDescriptor failed", ex)
            }
        }

        if (originalBitmap == null) {
            // Fallback: copy to temp file and decode
            try {
                val tempFile = File(context.cacheDir, "profile_src_${System.currentTimeMillis()}")
                context.contentResolver.openInputStream(uri)?.use { ins ->
                    tempFile.outputStream().use { outs ->
                        ins.copyTo(outs)
                    }
                }
                originalBitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                tempFile.delete()
            } catch (ex: Exception) {
                Log.w("HomeDebug", "fallback decode failed", ex)
            }
        }

        if (originalBitmap == null) {
            Log.e("HomeDebug", "Could not decode selected image")
            return false
        }

        return uploadProfilePicture(context, originalBitmap)
    }

    private suspend fun uploadProfilePicture(context: Context, bitmap: Bitmap): Boolean {
        val maxDimension = 800
        val scaleRatio = minOf(
            maxDimension.toFloat() / bitmap.width,
            maxDimension.toFloat() / bitmap.height,
            1.0f
        )

        val resizedBitmap = if (scaleRatio < 1.0f) {
            val newWidth = (bitmap.width * scaleRatio).toInt()
            val newHeight = (bitmap.height * scaleRatio).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }

        val baos = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val compressedBytes = baos.toByteArray()
        baos.close()

        val tempFile = File(context.cacheDir, "profile_upload_${System.currentTimeMillis()}.jpg")
        tempFile.writeBytes(compressedBytes)

        return try {
            val requestBody = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", tempFile.name, requestBody)
            val uploadResponse = RetrofitClient.apiService.uploadProfilePicture(part)
            if (uploadResponse.isSuccessful) {
                uploadResponse.body()?.success == true
            } else {
                Log.e("HomeDebug", "Profile upload returned error: ${uploadResponse.code()}")
                false
            }
        } catch (ex: Exception) {
            Log.e("HomeDebug", "Profile upload exception", ex)
            false
        } finally {
            tempFile.delete()
        }
    }

    private fun parseBackendError(code: Int, body: String?): String {
        val backendMessage = runCatching {
            if (body.isNullOrBlank()) "" else JSONObject(body).optString("error", "")
        }.getOrDefault("")

        if (backendMessage.isNotBlank()) return backendMessage

        return when (code) {
            401 -> "Unauthorized. Please log in again."
            403 -> "You do not have permission to view these services."
            404 -> "Services not found."
            else -> "Server error ($code). Please try again."
        }
    }
}
