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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private val _servicesState = MutableStateFlow<ClientServicesUiState>(ClientServicesUiState.Idle)
    val servicesState: StateFlow<ClientServicesUiState> = _servicesState.asStateFlow()

    private val _activeServicesState = MutableStateFlow<ClientServicesUiState>(ClientServicesUiState.Idle)
    val activeServicesState: StateFlow<ClientServicesUiState> = _activeServicesState.asStateFlow()

    init {
        // Step 1: Display name immediately from local session
        val localName = SessionManager.userName
        val localPic = SessionManager.picDirectory
        Log.d("HomeDebug", "SessionManager.picDirectory=$localPic, userName=$localName, userId=${SessionManager.userId}")
        _uiState.value = _uiState.value.copy(
            userName = if (localName.isNotBlank()) localName else "Viajero",
            picDirectory = localPic,
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
                val fullName = profile.name.trim()
                _uiState.value = _uiState.value.copy(
                    userName = if (fullName.isNotBlank()) fullName else _uiState.value.userName,
                    picDirectory = profile.picDirectory ?: _uiState.value.picDirectory
                )
            } catch (_: Exception) {
                // Keep local name, don't show error
            }
        }

        loadClientServices()
        loadActiveClientServices()
        startPollingServices()
    }

    private fun startPollingServices() {
        viewModelScope.launch {
            while (true) {
                delay(5000) // Poll every 5 seconds
                loadClientServices(silent = true)
                loadActiveClientServices(silent = true)
            }
        }
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
            val newPicDirectory = uploadProfilePicture(context, uri)
            if (!newPicDirectory.isNullOrBlank()) {
                SessionManager.updatePicDirectory(newPicDirectory)
                _uiState.value = _uiState.value.copy(picDirectory = newPicDirectory)
            } else {
                Log.e("HomeDebug", "Failed to upload profile picture")
            }
        }
    }

    private suspend fun uploadProfilePicture(context: Context, uri: android.net.Uri): String? {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        if (originalBitmap == null) {
            Log.e("HomeDebug", "Could not decode selected image")
            return null
        }

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

        val tempFile = File(context.cacheDir, "profile_upload_${System.currentTimeMillis()}.jpg")
        tempFile.writeBytes(compressedBytes)

        return try {
            val requestBody = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", tempFile.name, requestBody)
            val uploadResponse = RetrofitClient.apiService.uploadProfilePicture(part)
            if (uploadResponse.isSuccessful) {
                uploadResponse.body()?.picDirectory
            } else {
                Log.e("HomeDebug", "Profile upload returned error: ${uploadResponse.code()}")
                null
            }
        } catch (ex: Exception) {
            Log.e("HomeDebug", "Profile upload exception", ex)
            null
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
