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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import de.multicoptermedia.gehoerlosenapp.speech.TranscriptSegment
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { LiveTranscriptScreen() } } }
    }
}

@Composable
private fun LiveTranscriptScreen() {
    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var isListening by remember { mutableStateOf(false) }
    var partialText by remember { mutableStateOf("") }
    var transcript by remember { mutableStateOf(emptyList<TranscriptSegment>()) }
    var statusText by remember { mutableStateOf("Starte …") }
    var startAfterPermission by remember { mutableStateOf(false) }
    var followLive by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()

    val recognizerAvailable = SpeechRecognizer.isRecognitionAvailable(context)
    val speechRecognizer = remember {
        if (recognizerAvailable) SpeechRecognizer.createSpeechRecognizer(context) else null
    }

    fun recognitionIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.GERMANY.toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
    }

    fun startListening() {
        if (!recognizerAvailable || speechRecognizer == null) {
            statusText = "Spracherkennung nicht verfügbar"
            return
        }
        if (!hasMicPermission) return
        partialText = ""
        isListening = true
        statusText = "● Ich höre zu"
        speechRecognizer.startListening(recognitionIntent())
    }

    fun pauseListening() {
        isListening = false
        speechRecognizer?.cancel()
        statusText = "Pausiert"
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasMicPermission = granted
        if (granted) {
            startAfterPermission = true
            statusText = "Starte …"
        } else statusText = "Mikrofon-Zugriff erforderlich"
    }

    DisposableEffect(speechRecognizer) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { statusText = "● Ich höre zu" }
            override fun onBeginningOfSpeech() { statusText = "● Sprache erkannt" }
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() { statusText = "● Verarbeite Sprache …" }

            override fun onError(error: Int) {
                partialText = ""
                if (!isListening) return
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT, SpeechRecognizer.ERROR_CLIENT -> {
                        statusText = "● Ich höre zu"
                        speechRecognizer?.cancel()
                        speechRecognizer?.startListening(recognitionIntent())
                    }
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                        statusText = "Offline-Sprachmodell noch nicht verfügbar"
                        isListening = false
                    }
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        statusText = "Mikrofon-Zugriff erforderlich"
                        isListening = false
                    }
                    else -> {
                        statusText = "Spracherkennung unterbrochen"
                        isListening = false
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                val result = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim().orEmpty()
                if (result.isNotEmpty()) transcript = transcript + TranscriptSegment(text = result)
                partialText = ""
                if (isListening) {
                    statusText = "● Ich höre zu"
                    speechRecognizer?.startListening(recognitionIntent())
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                partialText = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            }
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }
        speechRecognizer?.setRecognitionListener(listener)
        onDispose { speechRecognizer?.cancel(); speechRecognizer?.destroy() }
    }

    LaunchedEffect(Unit) {
        if (hasMicPermission) startListening() else {
            statusText = "Mikrofon einmalig freigeben"
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    LaunchedEffect(startAfterPermission) {
        if (startAfterPermission && hasMicPermission) {
            startAfterPermission = false
            startListening()
        }
    }

    // Follow new text only while the user is in live mode.
    LaunchedEffect(transcript.size, partialText, followLive) {
        if (followLive) scrollState.animateScrollTo(scrollState.maxValue)
    }
    // A deliberate upward scroll switches to reading mode.
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress && scrollState.value < scrollState.maxValue - 24) followLive = false
    }

    fun speakerColor(id: Int?): Color = when (id) {
        0 -> MaterialTheme.colorScheme.primary
        1 -> MaterialTheme.colorScheme.tertiary
        2 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurface
    }
    fun speakerLabel(id: Int?) = id?.let { "Person " + (it + 1) } ?: "Sprecher unbekannt"

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Gespräch Live", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(statusText, fontSize = 18.sp, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(18.dp))

        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState)
        ) {
            if (transcript.isEmpty() && partialText.isBlank()) {
                Text("Gesprochener Text erscheint hier.", fontSize = 32.sp, lineHeight = 42.sp)
            }
            transcript.forEach { segment ->
                Text(
                    speakerLabel(segment.speakerId),
                    color = speakerColor(segment.speakerId),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(segment.text, color = speakerColor(segment.speakerId), fontSize = 32.sp, lineHeight = 42.sp)
            }
            if (partialText.isNotBlank()) {
                Text("Sprecher unbekannt", fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                Text(partialText, fontSize = 32.sp, lineHeight = 42.sp)
            }
        }

        if (!followLive) {
            Button(
                onClick = { followLive = true },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            ) {
                Text("↓ Zum aktuellen Gespräch", fontSize = 19.sp)
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    if (!hasMicPermission) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    else if (isListening) pauseListening() else startListening()
                }
            ) { Text(if (isListening) "Pause" else "Weiter", fontSize = 20.sp) }

            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    transcript = emptyList()
                    partialText = ""
                    followLive = true
                }
            ) { Text("Text löschen", fontSize = 20.sp) }
        }
    }
}
