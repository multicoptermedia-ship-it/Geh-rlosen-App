package de.multicoptermedia.gehoerlosenapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LiveTranscriptScreen()
                }
            }
        }
    }
}

@Composable
private fun LiveTranscriptScreen() {
    val context = LocalContext.current

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isListening by remember { mutableStateOf(false) }
    var partialText by remember { mutableStateOf("") }
    var transcript by remember { mutableStateOf(emptyList<String>()) }
    var statusText by remember { mutableStateOf("Bereit") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        statusText = if (granted) "Mikrofon freigegeben" else "Mikrofon-Zugriff benötigt"
    }

    val recognizerAvailable = SpeechRecognizer.isRecognitionAvailable(context)

    val speechRecognizer = remember {
        if (recognizerAvailable) SpeechRecognizer.createSpeechRecognizer(context) else null
    }

    fun recognitionIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.GERMANY.toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    fun startListening() {
        if (!recognizerAvailable || speechRecognizer == null) {
            statusText = "Keine Spracherkennung auf diesem Gerät verfügbar"
            return
        }

        if (!hasMicPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        partialText = ""
        isListening = true
        statusText = "Ich höre zu …"
        speechRecognizer.startListening(recognitionIntent())
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
        statusText = "Gestoppt"
    }

    DisposableEffect(speechRecognizer) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                statusText = "Sprich jetzt"
            }

            override fun onBeginningOfSpeech() {
                statusText = "Sprache erkannt"
            }

            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() {
                statusText = "Verarbeite …"
            }

            override fun onError(error: Int) {
                partialText = ""

                if (isListening) {
                    statusText = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Warte auf Sprache …"
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Netzwerkfehler bei der Spracherkennung"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mikrofon-Zugriff fehlt"
                        else -> "Spracherkennung kurz unterbrochen"
                    }

                    if (
                        error == SpeechRecognizer.ERROR_NO_MATCH ||
                        error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                        error == SpeechRecognizer.ERROR_CLIENT
                    ) {
                        speechRecognizer?.cancel()
                        speechRecognizer?.startListening(recognitionIntent())
                    } else {
                        isListening = false
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                val result = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()

                if (result.isNotEmpty()) {
                    transcript = transcript + result
                }

                partialText = ""

                if (isListening) {
                    statusText = "Ich höre weiter zu …"
                    speechRecognizer?.startListening(recognitionIntent())
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                partialText = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }

        speechRecognizer?.setRecognitionListener(listener)

        onDispose {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        }
    }

    val displayText = buildString {
        if (transcript.isNotEmpty()) {
            append(transcript.joinToString(separator = "\n\n"))
        }

        if (partialText.isNotBlank()) {
            if (isNotEmpty()) append("\n\n")
            append(partialText)
        }
    }.ifBlank {
        "Der erkannte Text erscheint hier."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = "Gespräch Live",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = statusText,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = displayText,
            fontSize = 30.sp,
            lineHeight = 39.sp,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    if (isListening) stopListening() else startListening()
                }
            ) {
                Text(if (isListening) "Stoppen" else "Zuhören", fontSize = 18.sp)
            }

            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    transcript = emptyList()
                    partialText = ""
                    statusText = if (isListening) "Ich höre zu …" else "Bereit"
                }
            ) {
                Text("Text löschen", fontSize = 18.sp)
            }
        }
    }
}
