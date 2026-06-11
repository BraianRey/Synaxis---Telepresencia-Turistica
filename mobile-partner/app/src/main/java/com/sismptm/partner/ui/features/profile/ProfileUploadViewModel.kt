package com.sismptm.partner.ui.features.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.partner.core.events.ProfileEvents
import com.sismptm.partner.core.network.RetrofitClient
import com.sismptm.partner.core.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.ByteArrayOutputStream
import java.io.File

class ProfileUploadViewModel : ViewModel() {
    sealed interface ProfilePhotoUploadState {
        object Idle : ProfilePhotoUploadState
        object Loading : ProfilePhotoUploadState
        data class Error(val message: String) : ProfilePhotoUploadState
    }

    private val _photoUploadState = MutableStateFlow<ProfilePhotoUploadState>(ProfilePhotoUploadState.Idle)
    val photoUploadState: StateFlow<ProfilePhotoUploadState> = _photoUploadState.asStateFlow()

    fun updateProfilePicture(context: Context, uri: android.net.Uri) {
        viewModelScope.launch {
            _photoUploadState.value = ProfilePhotoUploadState.Loading
            val success = uploadProfilePicture(context, uri)
            if (success) {
                _photoUploadState.value = ProfilePhotoUploadState.Idle
                try {
                    ProfileEvents.profilePictureUpdated.emit(Unit)
                } catch (ex: Exception) {
                    Log.w("ProfileUpload", "Failed to emit profile update event", ex)
                }
            } else {
                _photoUploadState.value = ProfilePhotoUploadState.Error("No se pudo subir la foto. Intenta nuevamente.")
                Log.e("ProfileUpload", "Failed to upload profile picture")
            }
        }
    }

    fun updateProfilePicture(context: Context, bitmap: Bitmap) {
        viewModelScope.launch {
            _photoUploadState.value = ProfilePhotoUploadState.Loading
            val success = uploadProfilePicture(context, bitmap)
            if (success) {
                _photoUploadState.value = ProfilePhotoUploadState.Idle
                try {
                    ProfileEvents.profilePictureUpdated.emit(Unit)
                } catch (ex: Exception) {
                    Log.w("ProfileUpload", "Failed to emit profile update event", ex)
                }
            } else {
                _photoUploadState.value = ProfilePhotoUploadState.Error("No se pudo subir la foto. Intenta nuevamente.")
                Log.e("ProfileUpload", "Failed to upload profile picture from camera")
            }
        }
    }

    private suspend fun uploadProfilePicture(context: Context, uri: android.net.Uri): Boolean {
        var originalBitmap: Bitmap? = null
        var inputStream = context.contentResolver.openInputStream(uri)
        try {
            originalBitmap = BitmapFactory.decodeStream(inputStream)
        } catch (ex: Exception) {
            Log.w("ProfileUpload", "decodeStream failed", ex)
        } finally {
            inputStream?.close()
        }

        if (originalBitmap == null) {
            try {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                pfd?.use { fd ->
                    originalBitmap = BitmapFactory.decodeFileDescriptor(fd.fileDescriptor)
                }
            } catch (ex: Exception) {
                Log.w("ProfileUpload", "decodeFileDescriptor failed", ex)
            }
        }

        if (originalBitmap == null) {
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
                Log.w("ProfileUpload", "fallback decode failed", ex)
            }
        }

        if (originalBitmap == null) {
            Log.e("ProfileUpload", "Could not decode selected image")
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
                val picDirectory = uploadResponse.body()?.picDirectory
                if (!picDirectory.isNullOrBlank()) {
                    SessionManager.picDirectory = picDirectory
                    true
                } else {
                    false
                }
            } else {
                Log.e("ProfileUpload", "Profile upload returned error: ${uploadResponse.code()}")
                false
            }
        } catch (ex: Exception) {
            Log.e("ProfileUpload", "Profile upload exception", ex)
            false
        } finally {
            tempFile.delete()
        }
    }
}
