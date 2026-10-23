package com.mobcare.security

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import androidx.core.content.ContextCompat
import com.mobcare.audit.AuditLogManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class IntruderRecord(
    val id: String,
    val filePath: String,
    val timestamp: String,
    val attemptedPin: String
)

object IntruderCaptureManager {

    private const val TAG = "IntruderCaptureManager"
    private const val LOG_FILE = "intruder_records.json"

    fun isCameraPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context.applicationContext,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun captureIntruderPhoto(context: Context, attemptedPin: String) {
        val appContext = context.applicationContext

        // Check camera permission
        if (!isCameraPermissionGranted(appContext)) {
            AuditLogManager.logSystemEvent(
                appContext,
                appContext.packageName,
                "Intruder Guard",
                "[Camera Blocked] Wrong PIN/Password attempted ($attemptedPin), but Camera permission is not granted."
            )
            return
        }

        val cameraManager = appContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return

        try {
            // Find front-facing camera
            var frontCameraId: String? = null
            var targetCharacteristics: CameraCharacteristics? = null

            for (id in cameraManager.cameraIdList) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    frontCameraId = id
                    targetCharacteristics = chars
                    break
                }
            }

            if (frontCameraId == null && cameraManager.cameraIdList.isNotEmpty()) {
                frontCameraId = cameraManager.cameraIdList[0]
                targetCharacteristics = cameraManager.getCameraCharacteristics(frontCameraId)
            }

            val targetId = frontCameraId ?: return
            val chars = targetCharacteristics ?: return

            // Query hardware-supported JPEG sizes (Crucial for high-res devices like Motorola Edge 50 Pro)
            val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val sizes = map?.getOutputSizes(ImageFormat.JPEG) ?: emptyArray()

            val chosenSize: Size = if (sizes.isNotEmpty()) {
                // Find size closest to 640x480 for fast, reliable silent capture
                val targetArea = 640 * 480
                sizes.minByOrNull { abs((it.width * it.height) - targetArea) } ?: sizes[0]
            } else {
                Size(640, 480)
            }

            val thread = HandlerThread("IntruderCameraThread").apply { start() }
            val bgHandler = Handler(thread.looper)

            val imageReader = ImageReader.newInstance(chosenSize.width, chosenSize.height, ImageFormat.JPEG, 2)

            imageReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                if (image != null) {
                    try {
                        val buffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)

                        // Save photo in private internal app storage
                        val dir = File(appContext.filesDir, "intruders")
                        if (!dir.exists()) dir.mkdirs()

                        val timeMillis = System.currentTimeMillis()
                        val photoFile = File(dir, "snap_$timeMillis.jpg")
                        FileOutputStream(photoFile).use { fos ->
                            fos.write(bytes)
                        }

                        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                        val timeStr = sdf.format(Date(timeMillis))

                        recordIntruderEvent(appContext, photoFile.absolutePath, timeStr, attemptedPin)

                        AuditLogManager.logSystemEvent(
                            appContext,
                            appContext.packageName,
                            "Intruder Guard",
                            "[Photo Captured] Intruder photo saved (${chosenSize.width}x${chosenSize.height}). Attempt: $attemptedPin"
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving intruder photo", e)
                    } finally {
                        image.close()
                    }
                }
            }, bgHandler)

            // Open Camera & capture single JPEG frame
            cameraManager.openCamera(targetId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    try {
                        val surfaces = listOf(imageReader.surface)
                        @Suppress("DEPRECATION")
                        camera.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                try {
                                    val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                                        addTarget(imageReader.surface)
                                        set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                                        set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                                    }

                                    session.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                                        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                                            super.onCaptureCompleted(session, request, result)
                                            bgHandler.postDelayed({
                                                try {
                                                    session.close()
                                                    camera.close()
                                                    imageReader.close()
                                                    thread.quitSafely()
                                                } catch (ignored: Exception) {}
                                            }, 50)
                                        }
                                    }, bgHandler)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Capture failed", e)
                                    camera.close()
                                    thread.quitSafely()
                                }
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {
                                camera.close()
                                thread.quitSafely()
                            }
                        }, bgHandler)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error creating capture session", e)
                        camera.close()
                        thread.quitSafely()
                    }
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    thread.quitSafely()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    thread.quitSafely()
                }
            }, bgHandler)

        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException opening camera", e)
        } catch (e: Exception) {
            Log.e(TAG, "Camera open failed", e)
        }
    }

    private fun recordIntruderEvent(context: Context, filePath: String, timestamp: String, pin: String) {
        try {
            val file = File(context.filesDir, LOG_FILE)
            val array = if (file.exists()) {
                val txt = file.readText()
                if (txt.isNotEmpty()) JSONArray(txt) else JSONArray()
            } else {
                JSONArray()
            }

            val record = JSONObject().apply {
                put("id", System.currentTimeMillis().toString())
                put("filePath", filePath)
                put("timestamp", timestamp)
                put("attemptedPin", pin)
            }

            val newArray = JSONArray()
            newArray.put(record)
            for (i in 0 until array.length()) {
                if (i < 50) {
                    newArray.put(array.get(i))
                }
            }
            file.writeText(newArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getIntruderRecords(context: Context): List<IntruderRecord> {
        val list = mutableListOf<IntruderRecord>()
        try {
            val file = File(context.filesDir, LOG_FILE)
            if (!file.exists()) return list
            val content = file.readText()
            if (content.isEmpty()) return list
            val array = JSONArray(content)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    IntruderRecord(
                        id = obj.optString("id"),
                        filePath = obj.optString("filePath"),
                        timestamp = obj.optString("timestamp"),
                        attemptedPin = obj.optString("attemptedPin")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun clearRecords(context: Context) {
        try {
            val records = getIntruderRecords(context)
            for (rec in records) {
                val f = File(rec.filePath)
                if (f.exists()) f.delete()
            }
            val logFile = File(context.filesDir, LOG_FILE)
            if (logFile.exists()) logFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
