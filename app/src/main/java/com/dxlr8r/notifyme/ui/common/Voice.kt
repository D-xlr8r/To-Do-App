package com.dxlr8r.notifyme.ui.common

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/**
 * Returns a function that opens the phone's speech recogniser.
 * "en-IN" understands Indian English; "hi-IN" understands Hindi and Hinglish.
 */
@Composable
fun rememberVoiceInput(language: String, onText: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val callback = rememberUpdatedState(onText)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.let { callback.value(it) }
        }
    }
    return {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, if (language.startsWith("hi")) "बोलिए…" else "Say your reminder…")
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        try {
            launcher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Voice typing isn't available on this phone", Toast.LENGTH_SHORT).show()
        }
    }
}
