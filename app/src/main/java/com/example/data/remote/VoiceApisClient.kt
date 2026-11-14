package com.example.data.remote

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.example.core.security.ApiSecretsResolver
import com.example.data.model.ProviderDiagnostic
import com.example.data.model.ProviderSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class ElevenLabsVoiceProfile(
    val voiceId: String,
    val name: String,
    val category: String
)

data class DeepgramTranscriptionResult(
    val transcript: String,
    val confidence: Double,
    val modelUsed: String,
    val durationSec: Double
)

class VoiceApisClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private var mediaPlayer: MediaPlayer? = null
    private var audioRecord: AudioRecord? = null
    private var recordThread: Thread? = null
    private val isRecordingFlag = AtomicBoolean(false)
    private val pcmBuffer = ByteArrayOutputStream()
    private var recordStartedMs: Long = 0L

    // ==================== ELEVENLABS NEURAL TTS ====================

    suspend fun synthesizeAndPlay(
        text: String,
        settings: ProviderSettings,
        context: Context,
        onPlaybackDone: () -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = ApiSecretsResolver.resolveElevenLabsKey(settings)
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("DISCONNECTED: Add ELEVENLABS_API_KEY in AI Studio Secrets or Settings.")
            )
        }
        val cleanText = text.trim().take(2400)
        if (cleanText.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("No text provided for ElevenLabs synthesis."))
        }
        val voiceId = settings.elevenLabsVoiceId.trim().ifBlank { DEFAULT_ELEVENLABS_VOICE_ID }
        val modelId = settings.elevenLabsModelId.trim().ifBlank { "eleven_multilingual_v2" }

        val payload = JSONObject().apply {
            put("text", cleanText)
            put("model_id", modelId)
            put("voice_settings", JSONObject().apply {
                put("stability", 0.45)
                put("similarity_boost", 0.78)
            })
        }

        val req = Request.Builder()
            .url("https://api.elevenlabs.io/v1/text-to-speech/$voiceId?output_format=mp3_44100_128")
            .header("xi-api-key", apiKey)
            .header("Accept", "audio/mpeg")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return@withContext try {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val errBody = resp.body?.string().orEmpty().take(180)
                    return@withContext Result.failure(
                        IllegalStateException("ElevenLabs HTTP ${resp.code}: $errBody")
                    )
                }
                val bytes = resp.body?.bytes()
                    ?: return@withContext Result.failure(IllegalStateException("ElevenLabs returned empty audio body."))
                if (bytes.isEmpty()) {
                    return@withContext Result.failure(IllegalStateException("ElevenLabs returned 0 audio bytes."))
                }

                val outFile = File(context.cacheDir, "aetherion_elevenlabs_tts.mp3")
                outFile.writeBytes(bytes)

                withContext(Dispatchers.Main) {
                    stopPlayback()
                    val player = MediaPlayer().apply {
                        setDataSource(outFile.absolutePath)
                        setOnCompletionListener { mp ->
                            runCatching { mp.release() }
                            if (mediaPlayer === mp) mediaPlayer = null
                            onPlaybackDone()
                        }
                        setOnErrorListener { mp, _, _ ->
                            runCatching { mp.release() }
                            if (mediaPlayer === mp) mediaPlayer = null
                            onPlaybackDone()
                            true
                        }
                        prepare()
                        start()
                    }
                    mediaPlayer = player
                }
                Result.success("Playing ${bytes.size / 1024} KB via ElevenLabs ($modelId)")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun stopPlayback() {
        val mp = mediaPlayer
        mediaPlayer = null
        if (mp != null) {
            runCatching {
                if (mp.isPlaying) mp.stop()
                mp.release()
            }
        }
    }

    suspend fun fetchElevenLabsVoices(settings: ProviderSettings): Result<List<ElevenLabsVoiceProfile>> =
        withContext(Dispatchers.IO) {
            val apiKey = ApiSecretsResolver.resolveElevenLabsKey(settings)
            if (apiKey.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("Add ELEVENLABS_API_KEY in AI Studio Secrets or Settings.")
                )
            }
            val req = Request.Builder()
                .url("https://api.elevenlabs.io/v1/voices")
                .header("xi-api-key", apiKey)
                .build()
            try {
                client.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        return@withContext Result.failure(IllegalStateException("ElevenLabs HTTP ${resp.code}: ${body.take(150)}"))
                    }
                    val arr = JSONObject(body).optJSONArray("voices")
                    val voices = mutableListOf<ElevenLabsVoiceProfile>()
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val v = arr.getJSONObject(i)
                            voices.add(
                                ElevenLabsVoiceProfile(
                                    voiceId = v.optString("voice_id"),
                                    name = v.optString("name", "Voice ${i + 1}"),
                                    category = v.optString("category", "premade")
                                )
                            )
                        }
                    }
                    Result.success(voices.take(25))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun testElevenLabsConnection(settings: ProviderSettings): ProviderDiagnostic =
        withContext(Dispatchers.IO) {
            val t0 = System.currentTimeMillis()
            val apiKey = ApiSecretsResolver.resolveElevenLabsKey(settings)
            if (apiKey.isBlank()) {
                return@withContext ProviderDiagnostic(
                    "ElevenLabs",
                    false,
                    "KEY MISSING",
                    0,
                    "Add ELEVENLABS_API_KEY in AI Studio Secrets or Settings."
                )
            }
            try {
                val req = Request.Builder()
                    .url("https://api.elevenlabs.io/v1/voices")
                    .header("xi-api-key", apiKey)
                    .build()
                client.newCall(req).execute().use { resp ->
                    val lat = System.currentTimeMillis() - t0
                    val body = resp.body?.string().orEmpty()
                    val count = runCatching { JSONObject(body).optJSONArray("voices")?.length() ?: 0 }.getOrDefault(0)
                    val ok = resp.isSuccessful && body.contains("voices")
                    ProviderDiagnostic(
                        "ElevenLabs",
                        ok,
                        if (ok) "CONNECTED" else "HTTP ${resp.code}",
                        lat,
                        if (ok) "$count voices ready" else body.take(140)
                    )
                }
            } catch (e: Exception) {
                ProviderDiagnostic(
                    "ElevenLabs",
                    false,
                    "ERROR",
                    System.currentTimeMillis() - t0,
                    e.localizedMessage ?: "ElevenLabs connection failed"
                )
            }
        }

    // ==================== DEEPGRAM NOVA STT ====================

    @SuppressLint("MissingPermission")
    fun startDeepgramRecording(context: Context): Result<Unit> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return Result.failure(IllegalStateException("Microphone permission required for Deepgram audio capture."))
        }
        if (isRecordingFlag.get()) {
            return Result.success(Unit)
        }
        val sampleRate = 16000
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        return try {
            val recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf * 2
            )
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder.release()
                return Result.failure(IllegalStateException("AudioRecord hardware initialization failed."))
            }
            synchronized(pcmBuffer) { pcmBuffer.reset() }
            audioRecord = recorder
            isRecordingFlag.set(true)
            recordStartedMs = System.currentTimeMillis()
            recorder.startRecording()

            val thread = Thread {
                val chunk = ByteArray(minBuf)
                while (isRecordingFlag.get()) {
                    val read = recorder.read(chunk, 0, chunk.size)
                    if (read > 0) {
                        synchronized(pcmBuffer) {
                            pcmBuffer.write(chunk, 0, read)
                        }
                    }
                }
            }.apply {
                name = "AetherionDeepgramCapture"
                start()
            }
            recordThread = thread
            Result.success(Unit)
        } catch (e: Exception) {
            isRecordingFlag.set(false)
            Result.failure(e)
        }
    }

    suspend fun stopDeepgramAndTranscribe(settings: ProviderSettings): Result<DeepgramTranscriptionResult> =
        withContext(Dispatchers.IO) {
            isRecordingFlag.set(false)
            runCatching { recordThread?.join(600) }
            recordThread = null

            val rec = audioRecord
            audioRecord = null
            if (rec != null) {
                runCatching { rec.stop() }
                runCatching { rec.release() }
            }

            val pcmBytes: ByteArray = synchronized(pcmBuffer) { pcmBuffer.toByteArray() }
            val elapsedSec = ((System.currentTimeMillis() - recordStartedMs).coerceAtLeast(300L)) / 1000.0

            val apiKey = ApiSecretsResolver.resolveDeepgramKey(settings)
            if (apiKey.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("DISCONNECTED: Add DEEPGRAM_API_KEY in AI Studio Secrets or Settings.")
                )
            }
            if (pcmBytes.size < 1600) {
                return@withContext Result.failure(
                    IllegalStateException("Captured audio is too short. Hold/speak clearly for at least 1 second.")
                )
            }

            val wavBytes = wrapPcmAsWav(pcmBytes, sampleRate = 16000, channels = 1, bitsPerSample = 16)
            val model = settings.deepgramModel.trim().ifBlank { "nova-3" }

            val req = Request.Builder()
                .url("https://api.deepgram.com/v1/listen?model=$model&smart_format=true&punctuate=true&language=en")
                .header("Authorization", "Token $apiKey")
                .post(wavBytes.toRequestBody("audio/wav".toMediaType()))
                .build()

            try {
                client.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("Deepgram HTTP ${resp.code}: ${body.take(180)}")
                        )
                    }
                    val root = JSONObject(body)
                    val alt = root.optJSONObject("results")
                        ?.optJSONArray("channels")
                        ?.optJSONObject(0)
                        ?.optJSONArray("alternatives")
                        ?.optJSONObject(0)
                    val transcript = alt?.optString("transcript", "").orEmpty().trim()
                    val confidence = alt?.optDouble("confidence", 0.0) ?: 0.0
                    if (transcript.isBlank()) {
                        return@withContext Result.failure(
                            IllegalStateException("Deepgram returned no speech transcript. Check microphone input.")
                        )
                    }
                    Result.success(
                        DeepgramTranscriptionResult(
                            transcript = transcript,
                            confidence = confidence,
                            modelUsed = model,
                            durationSec = elapsedSec
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun cancelDeepgramCapture() {
        isRecordingFlag.set(false)
        runCatching { recordThread?.join(300) }
        recordThread = null
        val rec = audioRecord
        audioRecord = null
        if (rec != null) {
            runCatching { rec.stop() }
            runCatching { rec.release() }
        }
        synchronized(pcmBuffer) { pcmBuffer.reset() }
    }

    suspend fun testDeepgramConnection(settings: ProviderSettings): ProviderDiagnostic =
        withContext(Dispatchers.IO) {
            val t0 = System.currentTimeMillis()
            val apiKey = ApiSecretsResolver.resolveDeepgramKey(settings)
            if (apiKey.isBlank()) {
                return@withContext ProviderDiagnostic(
                    "Deepgram",
                    false,
                    "KEY MISSING",
                    0,
                    "Add DEEPGRAM_API_KEY in AI Studio Secrets or Settings."
                )
            }
            try {
                val req = Request.Builder()
                    .url("https://api.deepgram.com/v1/projects")
                    .header("Authorization", "Token $apiKey")
                    .build()
                client.newCall(req).execute().use { resp ->
                    val lat = System.currentTimeMillis() - t0
                    val body = resp.body?.string().orEmpty()
                    val count = runCatching { JSONObject(body).optJSONArray("projects")?.length() ?: 0 }.getOrDefault(0)
                    val ok = resp.isSuccessful && body.contains("projects")
                    ProviderDiagnostic(
                        "Deepgram",
                        ok,
                        if (ok) "CONNECTED" else "HTTP ${resp.code}",
                        lat,
                        if (ok) "$count project(s) · ${settings.deepgramModel}" else body.take(140)
                    )
                }
            } catch (e: Exception) {
                ProviderDiagnostic(
                    "Deepgram",
                    false,
                    "ERROR",
                    System.currentTimeMillis() - t0,
                    e.localizedMessage ?: "Deepgram connection failed"
                )
            }
        }

    fun releaseAll() {
        stopPlayback()
        cancelDeepgramCapture()
    }

    private fun wrapPcmAsWav(pcmData: ByteArray, sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
        val totalDataLen = pcmData.size + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(totalDataLen)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1.toShort())
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort(blockAlign.toShort())
            putShort(bitsPerSample.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(pcmData.size)
        }.array()
        return ByteArray(44 + pcmData.size).also { out ->
            System.arraycopy(header, 0, out, 0, 44)
            System.arraycopy(pcmData, 0, out, 44, pcmData.size)
        }
    }

    companion object {
        const val DEFAULT_ELEVENLABS_VOICE_ID = "JBFqnCBsd6RMkjVDRZzb" // George (Institutional Narrator)
        val CURATED_VOICES = listOf(
            ElevenLabsVoiceProfile("JBFqnCBsd6RMkjVDRZzb", "George (Desk Briefings)", "premade"),
            ElevenLabsVoiceProfile("pNInz6obpgDQGcFmaJgB", "Adam (Deep Executive)", "premade"),
            ElevenLabsVoiceProfile("21m00Tcm4TlvDq8ikWAM", "Rachel (Crisp Analyst)", "premade"),
            ElevenLabsVoiceProfile("EXAVITQu4vr4xnSDxMaL", "Sarah (Warm Concise)", "premade")
        )
    }
}
