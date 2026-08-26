package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ai

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Locale

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    STOPPED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiVoiceScreen(
    role: String,
    onNavigateBack: () -> Unit,
    viewModel: AiViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    var voiceState by remember { mutableStateOf(VoiceState.IDLE) }
    var statusText by remember { mutableStateOf("Sesli sohbete başlamak için mikrofona dokunun.") }
    var isListeningActive by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val tts = remember {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale("tr", "TR")
                engine?.setSpeechRate(1.0f)
                engine?.setPitch(1.0f)
            }
        }
        engine
    }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    fun startListening() {
        speechRecognizer?.let { recognizer ->
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            try {
                recognizer.cancel()
                recognizer.startListening(intent)
                isListeningActive = true
                voiceState = VoiceState.LISTENING
                statusText = "Dinliyorum, konuşabilirsiniz..."
            } catch (e: Exception) {
                Log.e("AiVoiceScreen", "Error starting speech recognizer", e)
            }
        }
    }

    fun stopListening() {
        speechRecognizer?.cancel()
        isListeningActive = false
        voiceState = VoiceState.IDLE
        statusText = "Dinleme durduruldu. Konuşmak için dokunun."
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListening()
        } else {
            statusText = "Mikrofon izni reddedildi."
        }
    }

    fun toggleListening() {
        if (isListeningActive) {
            stopListening()
        } else {
            tts?.stop() // Konuşuyorsa kes
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun stopSpeakingAndListening() {
        tts?.stop()
        speechRecognizer?.cancel()
        isListeningActive = false
        voiceState = VoiceState.STOPPED
        statusText = "Görüşme durduruldu."
    }

    fun speakResponse(text: String) {
        voiceState = VoiceState.SPEAKING
        statusText = "Yanıt veriyorum..."

        val utteranceId = "ai_tts_utterance"
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                if (isListeningActive) {
                    android.os.Handler(context.mainLooper).post {
                        startListening()
                    }
                }
            }

            override fun onError(utteranceId: String?) {
                if (isListeningActive) {
                    android.os.Handler(context.mainLooper).post {
                        startListening()
                    }
                }
            }
        })

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
            val lastMessage = uiState.messages.last()
            if (lastMessage.sender == AiSender.ASSISTANT && (voiceState == VoiceState.PROCESSING || voiceState == VoiceState.SPEAKING)) {
                speakResponse(lastMessage.text)
            }
        }
    }

    LaunchedEffect(uiState.isLoading) {
        if (uiState.isLoading) {
            voiceState = VoiceState.PROCESSING
            statusText = "Düşünüyorum..."
        }
    }

    DisposableEffect(Unit) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                voiceState = VoiceState.LISTENING
                statusText = "Konuşabilirsiniz..."
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                voiceState = VoiceState.PROCESSING
                statusText = "İşleniyor..."
            }
            override fun onError(error: Int) {
                Log.d("AiVoiceScreen", "Speech recognition error code: $error")
                if (isListeningActive) {
                    android.os.Handler(context.mainLooper).postDelayed({
                        startListening()
                    }, 800)
                }
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0]
                    Log.d("AiVoiceScreen", "Recognized text: $spokenText")
                    viewModel.sendMessage(spokenText, role)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        speechRecognizer?.setRecognitionListener(listener)

        onDispose {
            speechRecognizer?.destroy()
            tts?.stop()
            tts?.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI ile Sesli Görüşme", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        stopSpeakingAndListening()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Durum Kartı
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(14.dp)
                )
            }

            // Mesajlar Alanı (Daha geniş yer kaplaması için weight artırıldı)
            if (uiState.messages.isNotEmpty()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.messages) { message ->
                        val isUser = message.sender == AiSender.USER
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Text(
                                    text = message.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1.2f))
            }

            // Ortadaki Büyük Mikrofon Butonu (Biraz daha aşağıda ve dengeli konumda)
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        when (voiceState) {
                            VoiceState.LISTENING -> MaterialTheme.colorScheme.primaryContainer
                            VoiceState.SPEAKING -> MaterialTheme.colorScheme.tertiaryContainer
                            VoiceState.PROCESSING -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .clickable { toggleListening() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListeningActive) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.weight(0.2f))

            // Alt Kontroller
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { toggleListening() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isListeningActive) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (isListeningActive) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isListeningActive) "Dinlemeyi Durdur" else "Dinlemeyi Başlat")
                    }

                    IconButton(
                        onClick = {
                            tts?.stop()
                            if (isListeningActive) {
                                startListening()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "AI Sesini Durdur", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }

                OutlinedButton(
                    onClick = {
                        stopSpeakingAndListening()
                        onNavigateBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Görüşmeyi Bitir")
                }
            }
        }
    }
}