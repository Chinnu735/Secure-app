package com.securefolder.core.security

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.securefolder.core.crypto.CryptoManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Captures front camera photo on failed authentication attempts.
 * Photos are immediately encrypted and stored in the secure vault.
 */
class IntruderDetector(private val context: Context) {

    private var imageCapture: ImageCapture? = null
    private val tag = "IntruderDetector"

    /**
     * Initialize camera for intruder detection.
     */
    fun initialize(lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e(tag, "Failed to initialize camera for intruder detection", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Capture intruder photo silently.
     * Photo is encrypted immediately after capture.
     */
    fun captureIntruder(onCaptured: (String) -> Unit = {}) {
        val imageCapture = imageCapture ?: return

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val tempFile = File(context.cacheDir, "intruder_$timestamp.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    // Encrypt the captured photo immediately
                    try {
                        val encryptedFile = File(
                            getIntruderDir(),
                            "intruder_${timestamp}.enc"
                        )
                        FileInputStream(tempFile).use { input ->
                            FileOutputStream(encryptedFile).use { output ->
                                CryptoManager.encryptStream(input, output)
                            }
                        }
                        // Securely delete the temp file
                        tempFile.delete()
                        onCaptured(encryptedFile.absolutePath)
                    } catch (e: Exception) {
                        Log.e(tag, "Failed to encrypt intruder photo", e)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(tag, "Failed to capture intruder photo", exception)
                    tempFile.delete()
                }
            }
        )
    }

    /**
     * Get list of encrypted intruder photos.
     */
    fun getIntruderPhotos(): List<File> {
        return getIntruderDir().listFiles()?.toList() ?: emptyList()
    }

    /**
     * Decrypt an intruder photo to bytes for display.
     */
    fun decryptIntruderPhoto(encryptedFile: File): ByteArray? {
        return try {
            val encrypted = encryptedFile.readBytes()
            CryptoManager.decryptBytes(encrypted)
        } catch (e: Exception) {
            Log.e(tag, "Failed to decrypt intruder photo", e)
            null
        }
    }

    /**
     * Delete all intruder photos.
     */
    fun clearIntruderPhotos() {
        getIntruderDir().listFiles()?.forEach { it.delete() }
    }

    private fun getIntruderDir(): File {
        return File(context.filesDir, "intruder_photos").also {
            if (!it.exists()) it.mkdirs()
        }
    }
}
