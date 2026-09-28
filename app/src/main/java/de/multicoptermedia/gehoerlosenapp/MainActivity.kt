package de.multicoptermedia.gehoerlosenapp

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.os.Bundle
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import de.multicoptermedia.gehoerlosenapp.speech.ConversationAudioController
import de.multicoptermedia.gehoerlosenapp.speech.SherpaGermanFastConformerEngine
import de.multicoptermedia.gehoerlosenapp.speech.TranscriptSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { LiveTranscriptScreen() } } }
    }
}

@Composable
private fun LiveTranscriptScreen() {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasMicPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var isListening by remember { mutableStateOf(false) }
    var partialText by remember { mutableStateOf("") }
    var transcript by remember { mutableStateOf(emptyList<TranscriptSegment>()) }
    var statusText by remember { mutableStateOf("Starte …") }
    var startAfterPermission by remember { mutableStateOf(false) }
    var followLive by remember { mutableStateOf(true) }
    var microphoneLevel by remember { mutableFloatStateOf(0f) }
    var showPermissionSettings by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val controller = remember {
        ConversationAudioController(speechEngine = SherpaGermanFastConformerEngine(context.applicationContext))
    }

    fun startListening() {
        if (!hasMicPermission) return
        partialText = ""
        statusText = "Starte Offline-Erkennung …"
        controller.start(
            onPartial = { text ->
                activity.lifecycleScope.launch(Dispatchers.Main.immediate) { partialText = text }
            },
            onFinal = { utterance ->
                activity.lifecycleScope.launch(Dispatchers.Main.immediate) {
                    if (utterance.text.isNotBlank()) {
                        transcript = transcript + TranscriptSegment(
                            segmentId = utterance.segmentId,
                            text = utterance.text
                        )
                    }
                    partialText = ""
                }
            },
            onSpeaker = { assignment ->
                activity.lifecycleScope.launch(Dispatchers.Main.immediate) {
                    transcript = transcript.map { segment ->
                        if (segment.segmentId == assignment.segmentId) {
                            segment.copy(speakerId = assignment.speakerId)
                        } else {
                            segment
                        }
                    }
                }
            },
            onStatus = { status ->
                activity.lifecycleScope.launch(Dispatchers.Main.immediate) {
                    statusText = status
                    isListening = status.startsWith("●")
                }
            },
            onError = { error ->
                activity.lifecycleScope.launch(Dispatchers.Main.immediate) {
                    statusText = error
                    isListening = false
                }
            },
            onLevel = { level ->
                activity.lifecycleScope.launch(Dispatchers.Main.immediate) { microphoneLevel = level }
            }
        )
    }

    fun pauseListening() {
        controller.stop()
        isListening = false
        microphoneLevel = 0f
        statusText = "Pausiert"
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasMicPermission = granted
        if (granted) {
            showPermissionSettings = false
            startAfterPermission = true
            statusText = "Starte …"
        } else {
            showPermissionSettings = !activity.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
            statusText = if (showPermissionSettings) {
                "Mikrofon ist blockiert · Bitte in den App-Einstellungen erlauben"
            } else {
                "Mikrofon wird zum Lesen von Gesprächen benötigt · Weiter drücken"
            }
        }
    }

    DisposableEffect(lifecycleOwner, controller) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    controller.stop()
                    isListening = false
                    microphoneLevel = 0f
                    statusText = "Pausiert · App nicht im Vordergrund"
                }
                Lifecycle.Event.ON_RESUME -> {
                    val permissionNowGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (permissionNowGranted && !hasMicPermission) {
                        hasMicPermission = true
                        showPermissionSettings = false
                        statusText = "Mikrofon freigegeben · Weiter drücken zum Zuhören"
                    } else if (!isListening && hasMicPermission && statusText == "Pausiert · App nicht im Vordergrund") {
                        statusText = "Pausiert · Weiter drücken zum Zuhören"
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.release()
        }
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

    LaunchedEffect(followLive) {
        while (followLive) {
            val remaining = scrollState.maxValue - scrollState.value
            if (remaining > 0) {
                // Follow the live conversation gently instead of jumping one block at a time.
                val step = remaining.coerceAtMost(18)
                scrollState.scrollTo(scrollState.value + step)
            }
            delay(16)
        }
    }

    LaunchedEffect(scrollState) {
        var previousValue = scrollState.value
        snapshotFlow { Triple(scrollState.value, scrollState.maxValue, scrollState.isScrollInProgress) }
            .collectLatest { (value, maxValue, isScrolling) ->
                val userMovedUp = isScrolling && value < previousValue
                if (userMovedUp && value < maxValue - 24) {
                    followLive = false
                }
                previousValue = value
            }
    }

    val speakerColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary
    )

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Gespräch Live", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(statusText, fontSize = 18.sp, modifier = Modifier.padding(top = 6.dp))
        if (isListening) {
            Text(
                text = "● Zuhören",
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Spacer(Modifier.height(18.dp))

        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState)) {
            if (transcript.isEmpty() && partialText.isBlank()) {
                Text("Gesprochener Text erscheint hier.", fontSize = 32.sp, lineHeight = 42.sp)
            }
            transcript.forEach { segment ->
                Text(
                    segment.speakerId?.let { "Person " + (it + 1) } ?: "Sprecher unbekannt",
                    color = segment.speakerId?.let { speakerColors[it % speakerColors.size] } ?: MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    segment.text,
                    color = segment.speakerId?.let { speakerColors[it % speakerColors.size] } ?: MaterialTheme.colorScheme.onSurface,
                    fontSize = 32.sp,
                    lineHeight = 42.sp
                )
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
            ) { Text("↓ Zum aktuellen Gespräch", fontSize = 19.sp) }
        }

        if (showPermissionSettings && !hasMicPermission) {
            Button(
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + context.packageName)
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            ) { Text("App-Einstellungen öffnen", fontSize = 19.sp) }
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

        Text(
            "powered by MCM-Dronetech GmbH · Testversion 0.2.0",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}
