package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AiProviderType
import com.example.data.model.ProviderSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.aetherSettingsDataStore by preferencesDataStore(name = "aetherion_provider_settings")

class SettingsPreferences(private val context: Context) {

    private object Keys {
        val ACTIVE_PROVIDER = stringPreferencesKey("active_provider")
        val DEEPSEEK_KEY = stringPreferencesKey("deepseek_api_key")
        val DEEPSEEK_MODEL = stringPreferencesKey("deepseek_model")
        val OPENAI_KEY = stringPreferencesKey("openai_api_key")
        val OPENAI_MODEL = stringPreferencesKey("openai_model")
        val OPENAI_URL = stringPreferencesKey("openai_base_url")
        val ANTHROPIC_KEY = stringPreferencesKey("anthropic_api_key")
        val ANTHROPIC_MODEL = stringPreferencesKey("anthropic_model")
        val FMP_KEY = stringPreferencesKey("fmp_api_key")
        val FRED_KEY = stringPreferencesKey("fred_api_key")
        val TWELVE_DATA_KEY = stringPreferencesKey("twelve_data_api_key")
        val FINNHUB_KEY = stringPreferencesKey("finnhub_api_key")
        val GEMINI_LIVE_VOICE = stringPreferencesKey("gemini_live_voice")
        val ELEVENLABS_KEY = stringPreferencesKey("elevenlabs_api_key")
        val ELEVENLABS_VOICE_ID = stringPreferencesKey("elevenlabs_voice_id")
        val ELEVENLABS_MODEL_ID = stringPreferencesKey("elevenlabs_model_id")
        val DEEPGRAM_KEY = stringPreferencesKey("deepgram_api_key")
        val DEEPGRAM_MODEL = stringPreferencesKey("deepgram_model")
        val MT5_BRIDGE_URL = stringPreferencesKey("mt5_bridge_url")
        val MT5_BRIDGE_TOKEN = stringPreferencesKey("mt5_bridge_token")
        val DERIV_APP_ID = stringPreferencesKey("deriv_app_id")
    }

    val settingsFlow: Flow<ProviderSettings> = context.aetherSettingsDataStore.data.map { prefs ->
        val providerName = prefs[Keys.ACTIVE_PROVIDER] ?: AiProviderType.FIREBASE_GEMINI_FLASH.name
        val provider = runCatching { AiProviderType.valueOf(providerName) }
            .getOrDefault(AiProviderType.FIREBASE_GEMINI_FLASH)
        ProviderSettings(
            activeProvider = provider,
            deepSeekApiKey = prefs[Keys.DEEPSEEK_KEY] ?: "",
            deepSeekModel = prefs[Keys.DEEPSEEK_MODEL] ?: "deepseek-chat",
            openAiApiKey = prefs[Keys.OPENAI_KEY] ?: "",
            openAiModel = prefs[Keys.OPENAI_MODEL] ?: "gpt-4o",
            openAiBaseUrl = prefs[Keys.OPENAI_URL] ?: "https://api.openai.com/v1",
            anthropicApiKey = prefs[Keys.ANTHROPIC_KEY] ?: "",
            anthropicModel = prefs[Keys.ANTHROPIC_MODEL] ?: "claude-3-7-sonnet-20250219",
            fmpApiKey = prefs[Keys.FMP_KEY] ?: "",
            fredApiKey = prefs[Keys.FRED_KEY] ?: "",
            twelveDataApiKey = prefs[Keys.TWELVE_DATA_KEY] ?: "",
            finnhubApiKey = prefs[Keys.FINNHUB_KEY] ?: "",
            geminiLiveVoice = prefs[Keys.GEMINI_LIVE_VOICE] ?: "FENRIR",
            elevenLabsApiKey = prefs[Keys.ELEVENLABS_KEY] ?: "",
            elevenLabsVoiceId = prefs[Keys.ELEVENLABS_VOICE_ID] ?: "JBFqnCBsd6RMkjVDRZzb",
            elevenLabsModelId = prefs[Keys.ELEVENLABS_MODEL_ID] ?: "eleven_multilingual_v2",
            deepgramApiKey = prefs[Keys.DEEPGRAM_KEY] ?: "",
            deepgramModel = prefs[Keys.DEEPGRAM_MODEL] ?: "nova-3",
            mt5BridgeUrl = prefs[Keys.MT5_BRIDGE_URL] ?: "",
            mt5BridgeToken = prefs[Keys.MT5_BRIDGE_TOKEN] ?: "",
            derivAppId = prefs[Keys.DERIV_APP_ID] ?: ""
        )
    }

    suspend fun updateSettings(transform: (ProviderSettings) -> ProviderSettings) {
        context.aetherSettingsDataStore.edit { prefs ->
            val currentProviderName = prefs[Keys.ACTIVE_PROVIDER] ?: AiProviderType.FIREBASE_GEMINI_FLASH.name
            val current = ProviderSettings(
                activeProvider = runCatching { AiProviderType.valueOf(currentProviderName) }
                    .getOrDefault(AiProviderType.FIREBASE_GEMINI_FLASH),
                deepSeekApiKey = prefs[Keys.DEEPSEEK_KEY] ?: "",
                deepSeekModel = prefs[Keys.DEEPSEEK_MODEL] ?: "deepseek-chat",
                openAiApiKey = prefs[Keys.OPENAI_KEY] ?: "",
                openAiModel = prefs[Keys.OPENAI_MODEL] ?: "gpt-4o",
                openAiBaseUrl = prefs[Keys.OPENAI_URL] ?: "https://api.openai.com/v1",
                anthropicApiKey = prefs[Keys.ANTHROPIC_KEY] ?: "",
                anthropicModel = prefs[Keys.ANTHROPIC_MODEL] ?: "claude-3-7-sonnet-20250219",
                fmpApiKey = prefs[Keys.FMP_KEY] ?: "",
                fredApiKey = prefs[Keys.FRED_KEY] ?: "",
                twelveDataApiKey = prefs[Keys.TWELVE_DATA_KEY] ?: "",
                finnhubApiKey = prefs[Keys.FINNHUB_KEY] ?: "",
                geminiLiveVoice = prefs[Keys.GEMINI_LIVE_VOICE] ?: "FENRIR",
                elevenLabsApiKey = prefs[Keys.ELEVENLABS_KEY] ?: "",
                elevenLabsVoiceId = prefs[Keys.ELEVENLABS_VOICE_ID] ?: "JBFqnCBsd6RMkjVDRZzb",
                elevenLabsModelId = prefs[Keys.ELEVENLABS_MODEL_ID] ?: "eleven_multilingual_v2",
                deepgramApiKey = prefs[Keys.DEEPGRAM_KEY] ?: "",
                deepgramModel = prefs[Keys.DEEPGRAM_MODEL] ?: "nova-3",
                mt5BridgeUrl = prefs[Keys.MT5_BRIDGE_URL] ?: "",
                mt5BridgeToken = prefs[Keys.MT5_BRIDGE_TOKEN] ?: "",
                derivAppId = prefs[Keys.DERIV_APP_ID] ?: ""
            )
            val updated = transform(current)
            prefs[Keys.ACTIVE_PROVIDER] = updated.activeProvider.name
            prefs[Keys.DEEPSEEK_KEY] = updated.deepSeekApiKey
            prefs[Keys.DEEPSEEK_MODEL] = updated.deepSeekModel
            prefs[Keys.OPENAI_KEY] = updated.openAiApiKey
            prefs[Keys.OPENAI_MODEL] = updated.openAiModel
            prefs[Keys.OPENAI_URL] = updated.openAiBaseUrl
            prefs[Keys.ANTHROPIC_KEY] = updated.anthropicApiKey
            prefs[Keys.ANTHROPIC_MODEL] = updated.anthropicModel
            prefs[Keys.FMP_KEY] = updated.fmpApiKey
            prefs[Keys.FRED_KEY] = updated.fredApiKey
            prefs[Keys.TWELVE_DATA_KEY] = updated.twelveDataApiKey
            prefs[Keys.FINNHUB_KEY] = updated.finnhubApiKey
            prefs[Keys.GEMINI_LIVE_VOICE] = updated.geminiLiveVoice
            prefs[Keys.ELEVENLABS_KEY] = updated.elevenLabsApiKey
            prefs[Keys.ELEVENLABS_VOICE_ID] = updated.elevenLabsVoiceId
            prefs[Keys.ELEVENLABS_MODEL_ID] = updated.elevenLabsModelId
            prefs[Keys.DEEPGRAM_KEY] = updated.deepgramApiKey
            prefs[Keys.DEEPGRAM_MODEL] = updated.deepgramModel
            prefs[Keys.MT5_BRIDGE_URL] = updated.mt5BridgeUrl
            prefs[Keys.MT5_BRIDGE_TOKEN] = updated.mt5BridgeToken
            prefs[Keys.DERIV_APP_ID] = updated.derivAppId
        }
    }
}
