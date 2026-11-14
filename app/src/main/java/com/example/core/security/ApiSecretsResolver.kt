package com.example.core.security

import com.example.BuildConfig
import com.example.data.model.ProviderSettings

object ApiSecretsResolver {
    private val placeholders = setOf(
        "",
        "null",
        "YOUR_FMP_API_KEY",
        "YOUR_FRED_API_KEY",
        "YOUR_TWELVE_DATA_API_KEY",
        "YOUR_FINNHUB_API_KEY",
        "YOUR_DEEPSEEK_API_KEY",
        "YOUR_ELEVENLABS_API_KEY",
        "YOUR_DEEPGRAM_API_KEY"
    )

    private fun cleanKey(value: String?): String {
        val trimmed = value?.trim().orEmpty()
        return if (trimmed in placeholders || trimmed.startsWith("YOUR_")) "" else trimmed
    }

    fun resolveFmpKey(settings: ProviderSettings): String =
        cleanKey(settings.fmpApiKey).ifBlank { cleanKey(BuildConfig.FMP_API_KEY) }

    fun resolveFredKey(settings: ProviderSettings): String =
        cleanKey(settings.fredApiKey).ifBlank { cleanKey(BuildConfig.FRED_API_KEY) }

    fun resolveTwelveDataKey(settings: ProviderSettings): String =
        cleanKey(settings.twelveDataApiKey).ifBlank { cleanKey(BuildConfig.TWELVE_DATA_API_KEY) }

    fun resolveFinnhubKey(settings: ProviderSettings): String =
        cleanKey(settings.finnhubApiKey).ifBlank { cleanKey(BuildConfig.FINNHUB_API_KEY) }

    fun resolveDeepSeekKey(settings: ProviderSettings): String =
        cleanKey(settings.deepSeekApiKey).ifBlank { cleanKey(BuildConfig.DEEPSEEK_API_KEY) }

    fun resolveElevenLabsKey(settings: ProviderSettings): String =
        cleanKey(settings.elevenLabsApiKey).ifBlank { cleanKey(BuildConfig.ELEVENLABS_API_KEY) }

    fun resolveDeepgramKey(settings: ProviderSettings): String =
        cleanKey(settings.deepgramApiKey).ifBlank { cleanKey(BuildConfig.DEEPGRAM_API_KEY) }

    fun hasBuildConfigFmp(): Boolean = cleanKey(BuildConfig.FMP_API_KEY).isNotBlank()
    fun hasBuildConfigFred(): Boolean = cleanKey(BuildConfig.FRED_API_KEY).isNotBlank()
    fun hasBuildConfigTwelveData(): Boolean = cleanKey(BuildConfig.TWELVE_DATA_API_KEY).isNotBlank()
    fun hasBuildConfigFinnhub(): Boolean = cleanKey(BuildConfig.FINNHUB_API_KEY).isNotBlank()
    fun hasBuildConfigDeepSeek(): Boolean = cleanKey(BuildConfig.DEEPSEEK_API_KEY).isNotBlank()
    fun hasBuildConfigElevenLabs(): Boolean = cleanKey(BuildConfig.ELEVENLABS_API_KEY).isNotBlank()
    fun hasBuildConfigDeepgram(): Boolean = cleanKey(BuildConfig.DEEPGRAM_API_KEY).isNotBlank()
}
