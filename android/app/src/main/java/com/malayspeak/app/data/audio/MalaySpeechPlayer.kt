package com.malayspeak.app.data.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.malayspeak.app.R
import java.util.Locale

/**
 * How well the device can speak Malay (ms-MY) without a network connection.
 *
 * [UNVERIFIED] is only used when the selected engine reports the Malay language
 * as available but exposes no voice metadata. Playback is still Malay, but the
 * engine cannot prove that the voice is installed offline.
 */
enum class MalayVoiceStatus {
    INITIALIZING,
    READY,
    UNVERIFIED,
    NETWORK_ONLY,
    NOT_INSTALLED,
    ERROR
}

data class SpeechMessage(
    val title: String,
    val body: String,
    val showSettings: Boolean = true,
    val showRetry: Boolean = true
)

data class MalaySpeechState(
    val status: MalayVoiceStatus = MalayVoiceStatus.INITIALIZING,
    val speakingId: Long? = null,
    val message: SpeechMessage? = null,
    val voiceName: String? = null,
    val offlineVerified: Boolean = false
)

/**
 * Activity-scoped Android TextToSpeech wrapper for Malay.
 *
 * Design choices:
 * - One engine instance, so a new phrase always flushes the previous utterance.
 * - No automatic language substitution: if Malay is unsupported, nothing is spoken.
 * - Only voices that are explicitly offline are selected when the engine lists them.
 * - Nothing is spoken until the user taps an audio control.
 */
class MalaySpeechPlayer(context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow(MalaySpeechState())
    val state: StateFlow<MalaySpeechState> = _state.asStateFlow()

    private var tts: TextToSpeech? = null
    private var released = false
    private var activeUtterance: String? = null
    private var pending: PendingPhrase? = null
    private var utteranceSequence = 0
    private var initGeneration = 0

    private data class PendingPhrase(val phraseId: Long, val text: String)

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit

        override fun onDone(utteranceId: String?) {
            finishUtterance(utteranceId, failed = false)
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            finishUtterance(utteranceId, failed = true)
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            finishUtterance(utteranceId, failed = true)
        }

        override fun onStop(utteranceId: String?, interrupted: Boolean) {
            finishUtterance(utteranceId, failed = false)
        }
    }

    init {
        initialiseEngine()
    }

    fun speak(phraseId: Long, malayText: String) {
        val text = malayText.trim()
        if (text.isEmpty()) return

        when (_state.value.status) {
            MalayVoiceStatus.INITIALIZING -> {
                // The user tapped before the engine finished starting. Keep the tap
                // and show the shared stop/cancel state until speech begins.
                if (_state.value.speakingId == phraseId) {
                    stop()
                    return
                }
                pending = PendingPhrase(phraseId, text)
                _state.value = _state.value.copy(
                    speakingId = phraseId,
                    message = null
                )
            }

            MalayVoiceStatus.READY,
            MalayVoiceStatus.UNVERIFIED -> {
                if (_state.value.speakingId == phraseId) {
                    stop()
                    return
                }
                pending = null
                startSpeaking(phraseId, text)
            }

            MalayVoiceStatus.NETWORK_ONLY,
            MalayVoiceStatus.NOT_INSTALLED,
            MalayVoiceStatus.ERROR -> {
                pending = null
                _state.value = _state.value.copy(
                    speakingId = null,
                    message = unavailableMessage(_state.value.status)
                )
            }
        }
    }

    fun stop() {
        pending = null
        activeUtterance = null
        try {
            tts?.stop()
        } catch (_: Throwable) {
            // Some engines throw if they are shutting down; state is cleared below.
        }
        _state.value = _state.value.copy(speakingId = null)
    }

    fun dismissMessage() {
        _state.value = _state.value.copy(message = null)
    }

    /**
     * Re-check the engine, for example after the user installs Malay voice data.
     */
    fun refresh() {
        if (released) return
        stop()
        try {
            tts?.shutdown()
        } catch (_: Throwable) {
            // Ignore engine shutdown failures; a fresh engine is created below.
        }
        tts = null
        initialiseEngine()
    }

    /**
     * Called when the activity resumes. Re-check only when the previous state was
     * unusable, so normal playback is never interrupted.
     */
    fun refreshIfUnavailable() {
        if (released) return
        when (_state.value.status) {
            MalayVoiceStatus.NETWORK_ONLY,
            MalayVoiceStatus.NOT_INSTALLED,
            MalayVoiceStatus.ERROR -> refresh()
            else -> Unit
        }
    }

    fun openTtsSettings() {
        val candidates = listOf(
            Intent("com.android.settings.TTS_SETTINGS"),
            Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )
        for (intent in candidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(appContext.packageManager) == null) continue
            try {
                appContext.startActivity(intent)
                return
            } catch (_: Throwable) {
                // Try the next settings destination.
            }
        }
    }

    fun release() {
        released = true
        initGeneration += 1
        pending = null
        activeUtterance = null
        mainHandler.removeCallbacksAndMessages(null)
        _state.value = _state.value.copy(speakingId = null)
        try {
            tts?.stop()
        } catch (_: Throwable) {
            // Ignore; shutdown follows.
        }
        try {
            tts?.shutdown()
        } catch (_: Throwable) {
            // Ignore shutdown failures during activity teardown.
        }
        tts = null
    }

    private fun initialiseEngine() {
        if (released) return
        val generation = ++initGeneration
        _state.value = MalaySpeechState(status = MalayVoiceStatus.INITIALIZING)
        tts = TextToSpeech(appContext) { status ->
            mainHandler.post { handleInit(generation, status) }
        }
    }

    private fun handleInit(generation: Int, status: Int) {
        if (released || generation != initGeneration) return
        val engine = tts ?: return
        if (status != TextToSpeech.SUCCESS) {
            failInit(MalayVoiceStatus.ERROR)
            return
        }

        try {
            engine.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            engine.setSpeechRate(LEARNER_SPEECH_RATE)
            engine.setPitch(1.0f)
            engine.setOnUtteranceProgressListener(progressListener)
        } catch (_: Throwable) {
            failInit(MalayVoiceStatus.ERROR)
            return
        }

        evaluateMalayVoice(engine)
    }

    private fun failInit(status: MalayVoiceStatus, voiceName: String? = null) {
        val hadPending = pending != null
        pending = null
        _state.value = MalaySpeechState(
            status = status,
            voiceName = voiceName,
            message = if (hadPending) unavailableMessage(status) else null
        )
    }

    private fun evaluateMalayVoice(engine: TextToSpeech) {
        val languageResult = try {
            engine.setLanguage(MALAY_LOCALE)
        } catch (_: Throwable) {
            TextToSpeech.LANG_NOT_SUPPORTED
        }

        if (languageResult < TextToSpeech.LANG_AVAILABLE) {
            failInit(MalayVoiceStatus.NOT_INSTALLED)
            return
        }

        val voices = try {
            engine.voices
        } catch (_: Throwable) {
            null
        }

        val offlineVoice = selectOfflineMalayVoice(voices)
        if (offlineVoice != null) {
            val applied = try {
                engine.setVoice(offlineVoice) == TextToSpeech.SUCCESS
            } catch (_: Throwable) {
                false
            }
            if (applied) {
                _state.value = MalaySpeechState(
                    status = MalayVoiceStatus.READY,
                    voiceName = offlineVoice.name,
                    offlineVerified = true
                )
                resumePending()
                return
            }
        }

        val current = try {
            engine.voice
        } catch (_: Throwable) {
            null
        }

        when {
            current != null && !current.locale.language.equals("ms", ignoreCase = true) -> {
                // The engine fell back to another language. Never use it silently.
                failInit(MalayVoiceStatus.NOT_INSTALLED)
            }

            current != null -> {
                val installed = !current.isNetworkConnectionRequired &&
                    !current.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
                when {
                    installed -> {
                        _state.value = MalaySpeechState(
                            status = MalayVoiceStatus.READY,
                            voiceName = current.name,
                            offlineVerified = true
                        )
                        resumePending()
                    }

                    current.isNetworkConnectionRequired -> failInit(
                        status = MalayVoiceStatus.NETWORK_ONLY,
                        voiceName = current.name
                    )

                    else -> failInit(MalayVoiceStatus.NOT_INSTALLED)
                }
            }

            voices != null && voices.isNotEmpty() -> {
                val hasMalay = voices.any { it.locale.language.equals("ms", ignoreCase = true) }
                failInit(
                    if (hasMalay) MalayVoiceStatus.NETWORK_ONLY
                    else MalayVoiceStatus.NOT_INSTALLED
                )
            }

            else -> {
                // The engine affirms ms-MY but cannot enumerate voices. Playback is
                // still Malay; the UI does not claim that it was offline-verified.
                _state.value = MalaySpeechState(
                    status = MalayVoiceStatus.UNVERIFIED,
                    offlineVerified = false
                )
                resumePending()
            }
        }
    }

    private fun selectOfflineMalayVoice(voices: Set<Voice>?): Voice? {
        if (voices.isNullOrEmpty()) return null
        val malay = voices.filter { it.locale.language.equals("ms", ignoreCase = true) }
        if (malay.isEmpty()) return null
        val malaysia = malay.filter { it.locale.country.equals("MY", ignoreCase = true) }
        val candidates = if (malaysia.isNotEmpty()) malaysia else malay
        return candidates
            .filter { voice ->
                !voice.isNetworkConnectionRequired &&
                    !voice.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
            }
            .sortedWith(
                compareByDescending<Voice> { it.quality }
                    .thenBy { it.latency }
            )
            .firstOrNull()
    }

    private fun resumePending() {
        val next = pending ?: return
        pending = null
        startSpeaking(next.phraseId, next.text)
    }

    private fun startSpeaking(phraseId: Long, text: String) {
        val engine = tts
        if (engine == null) {
            pending = PendingPhrase(phraseId, text)
            return
        }

        try {
            engine.stop()
        } catch (_: Throwable) {
            // Ignore stop failures before starting a new utterance.
        }
        utteranceSequence += 1
        val utteranceId = "malay-phrase-$phraseId-$utteranceSequence"
        activeUtterance = utteranceId
        _state.value = _state.value.copy(
            speakingId = phraseId,
            message = null
        )

        val result = try {
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } catch (_: Throwable) {
            TextToSpeech.ERROR
        }

        if (result == TextToSpeech.ERROR) {
            activeUtterance = null
            _state.value = _state.value.copy(
                speakingId = null,
                message = unavailableMessage(MalayVoiceStatus.ERROR)
            )
        }
    }

    private fun finishUtterance(utteranceId: String?, failed: Boolean) {
        mainHandler.post {
            if (utteranceId != activeUtterance) return@post
            activeUtterance = null
            _state.value = _state.value.copy(
                speakingId = null,
                message = if (failed) {
                    unavailableMessage(MalayVoiceStatus.ERROR)
                } else {
                    _state.value.message
                }
            )
        }
    }

    private fun unavailableMessage(status: MalayVoiceStatus): SpeechMessage {
        val bodyRes = when (status) {
            MalayVoiceStatus.NETWORK_ONLY -> R.string.audio_network_only
            MalayVoiceStatus.NOT_INSTALLED -> R.string.audio_not_installed
            MalayVoiceStatus.ERROR -> R.string.audio_engine_error
            else -> R.string.audio_unavailable_generic
        }
        return SpeechMessage(
            title = appContext.getString(R.string.audio_unavailable_title),
            body = appContext.getString(bodyRes)
        )
    }

    private companion object {
        val MALAY_LOCALE: Locale = Locale("ms", "MY")
        const val LEARNER_SPEECH_RATE = 0.85f
    }
}
