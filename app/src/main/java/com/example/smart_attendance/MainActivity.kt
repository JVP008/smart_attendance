package com.example.smart_attendance

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.smart_attendance.databinding.ActivityMainBinding
import com.example.smart_attendance.ui.fragments.AttendanceFragment
import com.example.smart_attendance.ui.fragments.AttendanceHistoryFragment
import com.example.smart_attendance.ui.fragments.RegisterFragment
import com.example.smart_attendance.ui.fragments.StudentsFragment
import org.opencv.android.OpenCVLoader
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load OpenCV native library
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV initialization failed!")
        } else {
            Log.d("OpenCV", "OpenCV initialization successful.")
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set the toolbar as the action bar
        setSupportActionBar(binding.toolbar)

        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence,
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    // Exit only if the user presses the negative button.
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        finish()
                    } else {
                        // For other errors (like cancellation), re-prompt authentication.
                        showToast("Authentication cancelled. Please authenticate to continue.")
                        biometricPrompt.authenticate(promptInfo)
                    }
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    super.onAuthenticationSucceeded(result)
                    showToast("Authentication succeeded!")
                    // On success, hide overlay and enable UI
                    binding.overlay.visibility = View.GONE
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    showToast("Authentication failed. Please try again.")
                    // Do not finish. The prompt will allow another attempt.
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for Smart Attendance")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Exit")
            .build()

        authenticateApp()

        // Set up bottom navigation with fragments
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_register -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, RegisterFragment())
                        .commit()
                    true
                }
                R.id.nav_attendance -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, AttendanceFragment())
                        .commit()
                    true
                }
                R.id.nav_students -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, StudentsFragment())
                        .commit()
                    true
                }
                R.id.nav_attendance_history -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, AttendanceHistoryFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }
        // Default fragment
        binding.bottomNav.selectedItemId = R.id.nav_register
    }

    private fun authenticateApp() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                showToast("No biometric features available on this device.")
                finish()
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                showToast("Biometric features are currently unavailable.")
                finish()
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                showToast("No biometrics enrolled. Please set up a fingerprint or face unlock in your device settings.")
                finish()
            }
            else -> {
                showToast("Biometric authentication is not available.")
                finish()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_hamburger_menu -> {
                val popupMenu = PopupMenu(this, binding.toolbar, Gravity.END)
                popupMenu.menuInflater.inflate(R.menu.overflow_menu, popupMenu.menu)
                popupMenu.setOnMenuItemClickListener {
                    onOptionsItemSelected(it) // Re-use existing onOptionsItemSelected for sub-items
                }
                popupMenu.show()
                true
            }
            R.id.action_about_us -> {
                showAboutUsDialog()
                true
            }
            R.id.action_how_to_use_app -> {
                showHowToUseAppDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAboutUsDialog() {
        AlertDialog.Builder(this)
            .setTitle("About Us")
            .setMessage("Government Polytectnic Sakoli Student: \nJayesh V. Patil\nCopyright \u00A9 2025-26")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showHowToUseAppDialog() {
        AlertDialog.Builder(this)
            .setTitle("How to Use App")
            .setMessage("1. Register Tab: Register new students by capturing/selecting their face images.\n2. Attendance Tab: Mark attendance using face recognition.\n3. Students Tab: View a list of all registered students.\n4. Attendance History Tab: View past attendance records.\n5. Export & Clear (in Register Tab): Export attendance data to a CSV file and clear the attendance history.")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}