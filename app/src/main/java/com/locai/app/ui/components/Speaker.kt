package com.locai.app.ui.components

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Reads replies aloud with the system TextToSpeech engine — on-device, works offline once the
 * phone's voice data is installed. One utterance at a time; [speakingKey] says which is playing.
 */
class Speaker(context: Context) {
    var speakingKey by mutableStateOf<Long?>(null)
        private set
    private var ready = false

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
    }.apply {
        setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = finished(utteranceId)
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = finished(utteranceId)
            override fun onStop(utteranceId: String?, interrupted: Boolean) = finished(utteranceId)
        })
    }

    private fun finished(utteranceId: String?) {
        if (utteranceId == speakingKey?.toString()) speakingKey = null
    }

    /** Starts reading [markdown] (code blocks skipped, markers stripped), or stops if [key] is already playing. Returns false if TTS is unavailable. */
    fun toggle(key: Long, markdown: String): Boolean {
        if (speakingKey == key) {
            tts.stop()
            speakingKey = null
            return true
        }
        if (!ready) return false
        val spoken = splitCodeBlocks(markdown)
            .filterIsInstance<MdBlock.Prose>()
            .joinToString("\n") { proseToAnnotated(it.text, Color.Unspecified).text.replace("•", "") }
        speakingKey = key
        // Long replies can exceed the engine's per-utterance limit; truncate rather than fail.
        tts.speak(spoken.take(TextToSpeech.getMaxSpeechInputLength() - 1), TextToSpeech.QUEUE_FLUSH, null, key.toString())
        return true
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}

@Composable
fun rememberSpeaker(): Speaker {
    val context = LocalContext.current
    val speaker = remember { Speaker(context) }
    DisposableEffect(speaker) { onDispose { speaker.shutdown() } }
    return speaker
}
