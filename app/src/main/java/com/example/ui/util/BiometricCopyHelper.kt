package com.example.ui.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Gates copying a sensitive string (an API key) behind the device's own
 * biometric/lock-screen challenge, so a key can't be lifted onto the
 * clipboard by anyone who has grabbed an unlocked phone but doesn't know
 * the fingerprint/face/PIN.
 *
 * Requires the app module to depend on `androidx.biometric:biometric:1.2.0`
 * (add it to build.gradle if it isn't already there) and requires the
 * hosting Activity to be a [FragmentActivity] (MainActivity needs to extend
 * that instead of plain ComponentActivity — FragmentActivity already
 * extends ComponentActivity, so this is a drop-in change).
 */
object BiometricCopyHelper {

    private const val AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_WEAK or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL

    /** True if the device has *some* way to authenticate (biometric or a screen lock). */
    fun hasUsableLock(context: Context): Boolean {
        return BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Copies [textToCopy] to the clipboard only after the user confirms with
     * biometrics, or — if the phone has no biometrics enrolled — its device
     * PIN/pattern/password. If the phone has no lock configured at all,
     * there is nothing to gate with, so the copy proceeds directly rather
     * than blocking the user with no way through.
     */
    fun copyWithAuthentication(
        activity: FragmentActivity,
        label: String,
        textToCopy: String,
        onCopied: () -> Unit = {},
        onFailed: (String) -> Unit = {}
    ) {
        if (textToCopy.isBlank()) return

        val canAuth = BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            performCopy(activity, label, textToCopy)
            onCopied()
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    performCopy(activity, label, textToCopy)
                    onCopied()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // errorCode 10/13 are user-cancel — no need to surface those as failures
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        onFailed(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    // Wrong fingerprint/face — the system prompt stays open for
                    // another attempt on its own, nothing to do here.
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Confirm it's you")
            .setSubtitle("Authenticate to copy your API key")
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()

        prompt.authenticate(promptInfo)
    }

    private fun performCopy(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "API Key copied", Toast.LENGTH_SHORT).show()
    }
}
