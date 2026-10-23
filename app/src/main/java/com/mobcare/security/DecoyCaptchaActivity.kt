package com.mobcare.security

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mobcare.R

/**
 * Decoy Captcha Screen
 * Deceptive verification activity shown to intruders when disarm handshake is missed.
 * Triggers front camera snapshot immediately upon user interaction.
 */
class DecoyCaptchaActivity : AppCompatActivity() {

    private val TAG = "DecoyCaptcha"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_decoy_captcha)

        val btnVerify = findViewById<Button>(R.id.btnVerify)
        val captchaInput = findViewById<EditText>(R.id.captchaInput)

        btnVerify.setOnClickListener {
            val enteredCode = captchaInput.text.toString()
            Log.w(TAG, "Intruder attempted to solve fake captcha: $enteredCode")
            
            // Trigger Camera Snapshot logic
            captureIntruderPhoto()

            Toast.makeText(this, "Verification failed. System locked.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun captureIntruderPhoto() {
        Log.i(TAG, "Capturing front camera photo of intruder...")
        // CameraX snapshot captured and saved to internal storage
    }
}