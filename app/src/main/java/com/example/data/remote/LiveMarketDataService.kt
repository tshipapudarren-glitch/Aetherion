package com.example.data.remote

import android.util.Log
import com.example.core.security.ApiSecretsResolver
import com.example.data.model.CandleBar
import com.example.data.model.EconomicCalendarEvent
import com.example.data.model.MacroIndicator
import com.example.data.model.MarketConditionsSnapshot
import com.example.data.model.MarketInstrument
import com.example.data.model.MarketQuote
import com.example.data.model.MarketUniverse
import com.example.data.model.NewsHeadline
import com.example.data.model.ProviderDiagnostic
import com.example.data.model.ProviderSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

class LiveMarketDataService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun fetchAllUniverseQuotes(
        settings: ProviderSettings = ProviderSettings()
    ): List<MarketQuote> = coroutineScope {
        MarketUniverse.instruments.map { instrument ->
            async(Dispatchers.IO) {
                fetchInstrumentQuote(instrument, settings = settings)
            }
        }.awaitAll()
    }

    suspend fun fetchInstrumentQuote(
        instrument: MarketInstrument,
        interval: String = "1h",
        range: String = "5d",
        settings: ProviderSettings = ProviderSettings()
    ): MarketQuote = withContext(Dispatchers.IO) {
        if (instrument.isConfigurableBridgeOnly || instrument.yahooTicker.isBlank()) {
            return@withContext MarketQuote(
                instrument = instrument,
                price = 0.0,
                previousClose = 0.0,
                changePct = 0.0,
                high24h = 0.0,
                low24h = 0.0,
                realizedVolPct = 0.0,
                candles = emptyList(),
                sourceName = "Deriv / MT5 Synthetic Bridge",
                latencyMs = 0L,
                fetchedAtMs = System.currentTimeMillis(),
                isLive = false,
                errorMessage = "DISCONNECTED: Configure Deriv App ID or MT5 Bridge endpoint in Settings."
            )
        }

        // 1. Institutional Keyed Sources if configured (Twelve Data -> FMP -> Finnhub)
        val tdKey = ApiSecretsResolver.resolveTwelveDataKey(settings)
        if (tdKey.isNotBlank() && instrument.twelveDataSymbol.isNotBlank()) {
            fetchTwelveDataQuote(instrument, tdKey)?.let { return@withContext it }
        }

        val fmpKey = ApiSecretsResolver.resolveFmpKey(settings)
        if (fmpKey.isNotBlank() && instrument.fmpSymbol.isNotBlank()) {
            fetchFmpQuote(instrument, fmpKey)?.let { return@withContext it }
        }

        // 2. Primary Zero-Key Live Feed: Yahoo Finance v8 Chart API
        fetchYahooChartQuote(instrument, interval, range)?.let { return@withContext it }

        // 3. Finnhub Spot Quote fallback if key configured
        val fhKey = ApiSecretsResolver.resolveFinnhubKey(settings)
        if (fhKey.isNotBlank() && instrument.finnhubSymbol.isNotBlank()) {
            fetchFinnhubSpotQuote(instrument, fhKey)?.let { return@withContext it }
        }

        // 4. Direct Crypto / ECB FX public fallbacks
        fallbackOrError(
            instrument = instrument,
            primaryError = "All configured market endpoints failed or returned empty data",
            primaryLatency = 0L
        )
    }

    private fun fetchTwelveDataQuote(instrument: MarketInstrument, apiKey: String): MarketQuote? {
        return try {
            val t0 = System.currentTimeMillis()
            val encoded = URLEncoder.encode(instrument.twelveDataSymbol, "UTF-8")
            val url = "https://api.twelvedata.com/time_series?symbol=$encoded&interval=1h&outputsize=90&apikey=$apiKey"
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                val lat = System.currentTimeMillis() - t0
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful || body.isBlank()) return null
                val json = JSONObject(body)
                if (json.optString("status") == "error") return null
                val values = json.optJSONArray("values") ?: return null
                if (values.length() == 0) return null

                val candles = mutableListOf<CandleBar>()
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                for (i in (values.length() - 1) downTo 0) {
                    val row = values.getJSONObject(i)
                    val c = row.optString("close").toDoubleOrNull() ?: continue
                    val o = row.optString("open").toDoubleOrNull() ?: c
                    val h = row.optString("high").toDoubleOrNull() ?: c
                    val l = row.optString("low").toDoubleOrNull() ?: c
                    val v = row.optString("volume").toDoubleOrNull() ?: 0.0
                    val ts = runCatching { sdf.parse(row.optString("datetime"))?.time }.getOrNull()
                        ?: (System.currentTimeMillis() - i * 3600_000L)
                    candles.add(CandleBar(ts, o, h, l, c, v))
                }
                if (candles.isEmpty()) return null
                val last = candles.last().close
                val prev = candles.getOrNull(candles.size - 24)?.close ?: candles.first().open
                val change = if (prev > 0.0) ((last - prev) / prev) * 100.0 else 0.0
                MarketQuote(
                    instrument = instrument,
                    price = last,
                    previousClose = prev,
                    changePct = change,
                    high24h = candles.takeLast(24).maxOf { it.high },
                    low24h = candles.takeLast(24).minOf { it.low },
                    realizedVolPct = computeAnnualizedVolatilityPct(candles),
                    candles = candles,
                    sourceName = "Twelve Data Live API",
                    latencyMs = lat,
                    fetchedAtMs = System.currentTimeMillis(),
                    isLive = true
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Twelve Data failed for ${instrument.symbol}: ${e.message}")
            null
        }
    }

    private fun fetchFmpQuote(instrument: MarketInstrument, apiKey: String): MarketQuote? {
        return try {
            val t0 = System.currentTimeMillis()
            val encoded = URLEncoder.encode(instrument.fmpSymbol, "UTF-8")
            val url = "https://financialmodelingprep.com/api/v3/historical-chart/1hour/$encoded?apikey=$apiKey"
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                val lat = System.currentTimeMillis() - t0
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful || body.isBlank() || body.trimStart().startsWith("{")) return null
                val arr = JSONArray(body)
                if (arr.length() == 0) return null

                val candles = mutableListOf<CandleBar>()
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("America/New_York")
                }
                val limit = minOf(arr.length(), 90)
                for (i in (limit - 1) downTo 0) {
                    val row = arr.getJSONObject(i)
                    val c = row.optDouble("close", Double.NaN)
                    if (c.isNaN() || c <= 0.0) continue
                    val o = row.optDouble("open", c).takeIf { !it.isNaN() } ?: c
                    val h = row.optDouble("high", c).takeIf { !it.isNaN() } ?: c
                    val l = row.optDouble("low", c).takeIf { !it.isNaN() } ?: c
                    val v = row.optDouble("volume", 0.0).takeIf { !it.isNaN() } ?: 0.0
                    val ts = runCatching { sdf.parse(row.optString("date"))?.time }.getOrNull()
                        ?: (System.currentTimeMillis() - i * 3600_000L)
                    candles.add(CandleBar(ts, o, h, l, c, v))
                }
                if (candles.isEmpty()) return null
                val last = candles.last().close
                val prev = candles.getOrNull(candles.size - 24)?.close ?: candles.first().open
                val change = if (prev > 0.0) ((last - prev) / prev) * 100.0 else 0.0
                MarketQuote(
                    instrument = instrument,
                    price = last,
                    previousClose = prev,
                    changePct = change,
                    high24h = candles.takeLast(24).maxOf { it.high },
                    low24h = candles.takeLast(24).minOf { it.low },
                    realizedVolPct = computeAnnualizedVolatilityPct(candles),
                    candles = candles,
                    sourceName = "FMP Live API",
                    latencyMs = lat,
                    fetchedAtMs = System.currentTimeMillis(),
                    isLive = true
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "FMP failed for ${instrument.symbol}: ${e.message}")
            null
        }
    }

    private fun fetchFinnhubSpotQuote(instrument: MarketInstrument, apiKey: String): MarketQuote? {
        return try {
            val t0 = System.currentTimeMillis()
            val encoded = URLEncoder.encode(instrument.finnhubSymbol, "UTF-8")
            val url = "https://finnhub.io/api/v1/quote?symbol=$encoded&token=$apiKey"
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                val lat = System.currentTimeMillis() - t0
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful || body.isBlank()) return null
                val json = JSONObject(body)
                val c = json.optDouble("c", 0.0)
                if (c <= 0.0) return null
                val pc = json.optDouble("pc", c).takeIf { it > 0.0 } ?: c
                val h = json.optDouble("h", c).takeIf { it > 0.0 } ?: c
                val l = json.optDouble("l", c).takeIf { it > 0.0 } ?: c
                val dp = json.optDouble("dp", ((c - pc) / pc) * 100.0)
                MarketQuote(
                    instrument = instrument,
                    price = c,
                    previousClose = pc,
                    changePct = dp,
                    high24h = h,
                    low24h = l,
                    realizedVolPct = 12.0,
                    candles = emptyList(),
                    sourceName = "Finnhub Live API",
                    latencyMs = lat,
                    fetchedAtMs = System.currentTimeMillis(),
                    isLive = true
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Finnhub spot failed for ${instrument.symbol}: ${e.message}")
            null
        }
    }

    private fun fetchYahooChartQuote(
        instrument: MarketInstrument,
        interval: String,
        range: String
    ): MarketQuote? {
        val startMs = System.currentTimeMillis()
        val encoded = URLEncoder.encode(instrument.yahooTicker, "UTF-8")
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$encoded?interval=$interval&range=$range"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Android; AETHERION/1.0)")
            .header("Accept", "application/json")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startMs
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful || body.isBlank()) return null

                val root = JSONObject(body)
                val chart = root.optJSONObject("chart")
                if (chart?.optJSONObject("error") != null) return null

                val results = chart?.optJSONArray("result")
                if (results == null || results.length() == 0) return null

                val first = results.getJSONObject(0)
                val meta = first.optJSONObject("meta")
                val regularPrice = meta?.optDouble("regularMarketPrice", Double.NaN) ?: Double.NaN
                val prevClose = meta?.optDouble("chartPreviousClose", Double.NaN)
                    ?.takeIf { !it.isNaN() && it > 0 }
                    ?: meta?.optDouble("previousClose", Double.NaN)
                    ?: Double.NaN

                val timestamps = first.optJSONArray("timestamp")
                val quoteObj = first.optJSONObject("indicators")
                    ?.optJSONArray("quote")
                    ?.optJSONObject(0)

                val opens = quoteObj?.optJSONArray("open")
                val highs = quoteObj?.optJSONArray("high")
                val lows = quoteObj?.optJSONArray("low")
                val closes = quoteObj?.optJSONArray("close")
                val volumes = quoteObj?.optJSONArray("volume")

                val candles = mutableListOf<CandleBar>()
                if (timestamps != null && closes != null) {
                    for (i in 0 until timestamps.length()) {
                        val c = closes.optDouble(i, Double.NaN)
                        if (c.isNaN() || c <= 0.0) continue
                        val o = opens?.optDouble(i, c)?.takeIf { !it.isNaN() && it > 0 } ?: c
                        val h = highs?.optDouble(i, c)?.takeIf { !it.isNaN() && it > 0 } ?: c
                        val l = lows?.optDouble(i, c)?.takeIf { !it.isNaN() && it > 0 } ?: c
                        val v = volumes?.optDouble(i, 0.0)?.takeIf { !it.isNaN() } ?: 0.0
                        candles.add(
                            CandleBar(
                                timestampMs = timestamps.getLong(i) * 1000L,
                                open = o,
                                high = h,
                                low = l,
                                close = c,
                                volume = v
                            )
                        )
                    }
                }

                val effectivePrice = if (!regularPrice.isNaN() && regularPrice > 0.0) {
                    regularPrice
                } else {
                    candles.lastOrNull()?.close ?: 0.0
                }
                if (effectivePrice <= 0.0) return null

                val effectivePrev = if (!prevClose.isNaN() && prevClose > 0.0) {
                    prevClose
                } else {
                    candles.firstOrNull()?.open ?: effectivePrice
                }
                val changePct = if (effectivePrev > 0.0) {
                    ((effectivePrice - effectivePrev) / effectivePrev) * 100.0
                } else 0.0

                MarketQuote(
                    instrument = instrument,
                    price = effectivePrice,
                    previousClose = effectivePrev,
                    changePct = changePct,
                    high24h = candles.takeLast(24).maxOfOrNull { it.high } ?: effectivePrice,
                    low24h = candles.takeLast(24).minOfOrNull { it.low } ?: effectivePrice,
                    realizedVolPct = computeAnnualizedVolatilityPct(candles),
                    candles = candles,
                    sourceName = "Yahoo Finance Live API",
                    latencyMs = latency,
                    fetchedAtMs = System.currentTimeMillis(),
                    isLive = true
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Yahoo Finance fetch failed for ${instrument.symbol}: ${e.message}")
            null
        }
    }

    private fun fallbackOrError(
        instrument: MarketInstrument,
        primaryError: String,
        primaryLatency: Long
    ): MarketQuote {
        val krakenPair = when (instrument.symbol) {
            "BTCUSD" -> "XXBTZUSD"
            "ETHUSD" -> "XETHZUSD"
            "SOLUSD" -> "SOLUSD"
            else -> null
        }
        if (krakenPair != null) {
            try {
                val t0 = System.currentTimeMillis()
                val req = Request.Builder()
                    .url("https://api.kraken.com/0/public/OHLC?pair=$krakenPair&interval=60")
                    .build()
                client.newCall(req).execute().use { resp ->
                    val lat = System.currentTimeMillis() - t0
                    val body = resp.body?.string().orEmpty()
                    if (resp.isSuccessful && body.isNotBlank()) {
                        val json = JSONObject(body)
                        val result = json.optJSONObject("result")
                        val arr = result?.optJSONArray(krakenPair)
                            ?: result?.keys()?.asSequence()?.firstOrNull { it != "last" }?.let { result.optJSONArray(it) }
                        if (arr != null && arr.length() > 0) {
                            val candles = mutableListOf<CandleBar>()
                            val start = (arr.length() - 120).coerceAtLeast(0)
                            for (i in start until arr.length()) {
                                val bar = arr.getJSONArray(i)
                                candles.add(
                                    CandleBar(
                                        timestampMs = bar.getLong(0) * 1000L,
                                        open = bar.getString(1).toDouble(),
                                        high = bar.getString(2).toDouble(),
                                        low = bar.getString(3).toDouble(),
                                        close = bar.getString(4).toDouble(),
                                        volume = bar.getString(6).toDouble()
                                    )
                                )
                            }
                            val last = candles.last().close
                            val prev = candles.getOrNull(candles.size - 24)?.close ?: candles.first().open
                            val change = if (prev > 0) ((last - prev) / prev) * 100.0 else 0.0
                            return MarketQuote(
                                instrument = instrument,
                                price = last,
                                previousClose = prev,
                                changePct = change,
                                high24h = candles.takeLast(24).maxOf { it.high },
                                low24h = candles.takeLast(24).minOf { it.low },
                                realizedVolPct = computeAnnualizedVolatilityPct(candles),
                                candles = candles,
                                sourceName = "Kraken Public REST API",
                                latencyMs = lat,
                                fetchedAtMs = System.currentTimeMillis(),
                                isLive = true
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Kraken fallback failed for ${instrument.symbol}: ${e.message}")
            }
        }

        if (instrument.symbol in listOf("EURUSD", "GBPUSD", "USDJPY", "AUDUSD", "USDZAR")) {
            try {
                val t0 = System.currentTimeMillis()
                val req = Request.Builder()
                    .url("https://api.frankfurter.app/latest?from=USD&to=EUR,GBP,JPY,AUD,ZAR")
                    .build()
                client.newCall(req).execute().use { resp ->
                    val lat = System.currentTimeMillis() - t0
                    val body = resp.body?.string().orEmpty()
                    if (resp.isSuccessful && body.isNotBlank()) {
                        val rates = JSONObject(body).optJSONObject("rates")
                        if (rates != null) {
                            val price = when (instrument.symbol) {
                                "EURUSD" -> 1.0 / rates.getDouble("EUR")
                                "GBPUSD" -> 1.0 / rates.getDouble("GBP")
                                "USDJPY" -> rates.getDouble("JPY")
                                "AUDUSD" -> 1.0 / rates.getDouble("AUD")
                                "USDZAR" -> rates.getDouble("ZAR")
                                else -> 0.0
                            }
                            if (price > 0.0) {
                                return MarketQuote(
                                    instrument = instrument,
                                    price = price,
                                    previousClose = price,
                                    changePct = 0.0,
                                    high24h = price,
                                    low24h = price,
                                    realizedVolPct = 7.5,
                                    candles = emptyList(),
                                    sourceName = "Frankfurter ECB Live FX",
                                    latencyMs = lat,
                                    fetchedAtMs = System.currentTimeMillis(),
                                    isLive = true
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Frankfurter fallback failed: ${e.message}")
            }
        }

        return MarketQuote(
            instrument = instrument,
            price = 0.0,
            previousClose = 0.0,
            changePct = 0.0,
            high24h = 0.0,
            low24h = 0.0,
            realizedVolPct = 0.0,
            candles = emptyList(),
            sourceName = "Disconnected",
            latencyMs = primaryLatency,
            fetchedAtMs = System.currentTimeMillis(),
            isLive = false,
            errorMessage = primaryError
        )
    }

    suspend fun fetchLiveMarketConditions(
        quotes: List<MarketQuote>,
        settings: ProviderSettings
    ): MarketConditionsSnapshot = withContext(Dispatchers.IO) {
        val fredKey = ApiSecretsResolver.resolveFredKey(settings)
        val fmpKey = ApiSecretsResolver.resolveFmpKey(settings)
        val finnhubKey = ApiSecretsResolver.resolveFinnhubKey(settings)

        val fredSeriesSpecs = listOf(
            "VIXCLS" to "CBOE VIX Volatility Index",
            "DGS10" to "US 10Y Treasury Yield (%)",
            "T10Y2Y" to "10Y - 2Y Yield Curve Spread",
            "DFF" to "Effective Fed Funds Rate (%)",
            "CPIAUCSL" to "US CPI Consumer Price Index",
            "UNRATE" to "US Unemployment Rate (%)"
        )

        val indicators = mutableListOf<MacroIndicator>()
        var fredError: String? = null

        if (fredKey.isNotBlank()) {
            val fetched = coroutineScope {
                fredSeriesSpecs.map { (id, title) ->
                    async(Dispatchers.IO) {
                        fetchFredSeries(id, title, fredKey)
                    }
                }.awaitAll()
            }
            indicators.addAll(fetched)
            if (fetched.none { it.isLive }) {
                fredError = "FRED API returned no valid observations; check FRED_API_KEY."
            }
        } else {
            // Live Zero-Key Macro Proxies (^VIX & ^TNX from Yahoo Finance) + Honest KEY REQUIRED slots for pure FRED series
            val vixVal = fetchSingleYahooSpot("^VIX")
            val us10yQuote = quotes.find { it.instrument.symbol == "US10Y" && it.isLive }
            val tnxSpot: Pair<Double, Double>? = if (us10yQuote != null) {
                us10yQuote.price to us10yQuote.changePct
            } else {
                fetchSingleYahooSpot("^TNX")
            }

            indicators.add(
                MacroIndicator(
                    seriesId = "VIXCLS / ^VIX",
                    title = "CBOE VIX Volatility Index",
                    valueText = vixVal?.let { String.format(Locale.US, "%.2f", it.first) } ?: "OFFLINE",
                    numericValue = vixVal?.first,
                    deltaText = vixVal?.let { String.format(Locale.US, "%+.2f%%", it.second) } ?: "-",
                    observationDate = "Live Spot",
                    sourceName = if (vixVal != null) "Yahoo Finance ^VIX Proxy" else "Unavailable",
                    isLive = vixVal != null
                )
            )
            indicators.add(
                MacroIndicator(
                    seriesId = "DGS10 / ^TNX",
                    title = "US 10Y Treasury Yield (%)",
                    valueText = tnxSpot?.let { String.format(Locale.US, "%.3f%%", it.first) } ?: "OFFLINE",
                    numericValue = tnxSpot?.first,
                    deltaText = tnxSpot?.let { String.format(Locale.US, "%+.2f%%", it.second) } ?: "-",
                    observationDate = "Live Spot",
                    sourceName = if (tnxSpot != null) "Yahoo Finance ^TNX Proxy" else "Unavailable",
                    isLive = tnxSpot != null
                )
            )
            listOf("T10Y2Y" to "10Y - 2Y Yield Curve Spread", "DFF" to "Effective Fed Funds Rate (%)", "CPIAUCSL" to "US CPI Index", "UNRATE" to "US Unemployment Rate (%)").forEach { (id, title) ->
                indicators.add(
                    MacroIndicator(
                        seriesId = id,
                        title = title,
                        valueText = "FRED KEY REQ",
                        numericValue = null,
                        deltaText = "Configure FRED_API_KEY",
                        observationDate = "-",
                        sourceName = "FRED Disconnected",
                        isLive = false
                    )
                )
            }
        }

        // Economic Calendar from FMP or Finnhub when keys exist
        val calendarEvents = mutableListOf<EconomicCalendarEvent>()
        if (fmpKey.isNotBlank()) {
            calendarEvents.addAll(fetchFmpEconomicCalendar(fmpKey))
        } else if (finnhubKey.isNotBlank()) {
            calendarEvents.addAll(fetchFinnhubEconomicCalendar(finnhubKey))
        }

        val liveQuotes = quotes.filter { it.isLive }
        val advancers = liveQuotes.count { it.changePct > 0.0 }
        val decliners = liveQuotes.count { it.changePct < 0.0 }
        val avgVol = if (liveQuotes.isNotEmpty()) liveQuotes.map { it.realizedVolPct }.average() else 0.0

        val vix = indicators.find { it.seriesId.startsWith("VIXCLS") }?.numericValue
        val us10y = indicators.find { it.seriesId.startsWith("DGS10") }?.numericValue
        val spread10y2y = indicators.find { it.seriesId == "T10Y2Y" }?.numericValue
        val fedFunds = indicators.find { it.seriesId == "DFF" }?.numericValue

        val volRegime = when {
            vix != null && vix >= 30.0 -> "CRISIS VOLATILITY (VIX >= 30)"
            vix != null && vix >= 22.0 -> "ELEVATED STRESS (VIX 22-30)"
            vix != null && vix >= 15.0 -> "BALANCED EXPANSION (VIX 15-22)"
            vix != null -> "LOW-VOL COMPRESSION (VIX < 15)"
            avgVol >= 25.0 -> "ELEVATED REALIZED VOL"
            avgVol > 0.0 -> "NORMAL REALIZED VOL"
            else -> "AWAITING FEEDS"
        }

        val curveStatus = when {
            spread10y2y == null -> "REQUIRES FRED T10Y2Y"
            spread10y2y < 0.0 -> "INVERTED (${String.format(Locale.US, "%+.2f%%", spread10y2y)})"
            spread10y2y < 0.25 -> "FLAT (${String.format(Locale.US, "%+.2f%%", spread10y2y)})"
            else -> "STEEPENING (${String.format(Locale.US, "%+.2f%%", spread10y2y)})"
        }

        val breadthRatio = if (liveQuotes.isNotEmpty()) advancers.toDouble() / liveQuotes.size else 0.5
        val vixPenalty = ((vix ?: 18.0) - 12.0).coerceIn(0.0, 30.0) * 1.3
        val riskScore = (40.0 + breadthRatio * 45.0 - vixPenalty + (if ((spread10y2y ?: 0.2) < 0) -8.0 else 4.0))
            .toInt().coerceIn(5, 98)

        val overallRegime = when {
            vix != null && vix >= 28.0 -> "RISK-OFF TAIL DEFENCE"
            spread10y2y != null && spread10y2y < -0.1 && breadthRatio < 0.45 -> "RECESSIONARY DEFENSIVE"
            riskScore >= 64 -> "RISK-ON LIQUIDITY EXPANSION"
            riskScore <= 38 -> "CONTRACTIONARY VOLATILITY"
            else -> "ROTATIONAL EQUILIBRIUM"
        }

        MarketConditionsSnapshot(
            overallRegime = overallRegime,
            volatilityRegime = volRegime,
            yieldCurveStatus = curveStatus,
            activeTradingSessions = computeActiveSessionsUtc(),
            advancersCount = advancers,
            declinersCount = decliners,
            averageRealizedVolPct = avgVol,
            riskAppetiteScore = riskScore,
            vixLevel = vix,
            us10yYield = us10y,
            yieldSpread10y2y = spread10y2y,
            fedFundsRate = fedFunds,
            indicators = indicators,
            economicEvents = calendarEvents,
            updatedAtMs = System.currentTimeMillis(),
            errorSummary = fredError
        )
    }

    private fun fetchFredSeries(seriesId: String, title: String, apiKey: String): MacroIndicator {
        val url = "https://api.stlouisfed.org/fred/series/observations?series_id=$seriesId&api_key=$apiKey&file_type=json&sort_order=desc&limit=5"
        return try {
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful || body.isBlank()) {
                    return MacroIndicator(seriesId, title, "HTTP ${resp.code}", null, "Error", "-", "FRED API", false)
                }
                val obs = JSONObject(body).optJSONArray("observations")
                val valid = mutableListOf<Pair<String, Double>>()
                if (obs != null) {
                    for (i in 0 until obs.length()) {
                        val item = obs.getJSONObject(i)
                        val v = item.optString("value").toDoubleOrNull()
                        val d = item.optString("date")
                        if (v != null) valid.add(d to v)
                    }
                }
                if (valid.isEmpty()) {
                    return MacroIndicator(seriesId, title, "NO DATA", null, "-", "-", "FRED API", false)
                }
                val latest = valid.first()
                val prev = valid.getOrNull(1)
                val delta = if (prev != null) String.format(Locale.US, "%+.3f", latest.second - prev.second) else "Latest"
                MacroIndicator(
                    seriesId = seriesId,
                    title = title,
                    valueText = String.format(Locale.US, "%.2f", latest.second),
                    numericValue = latest.second,
                    deltaText = delta,
                    observationDate = latest.first,
                    sourceName = "FRED St. Louis Fed API",
                    isLive = true
                )
            }
        } catch (e: Exception) {
            MacroIndicator(seriesId, title, "ERROR", null, e.localizedMessage ?: "Failed", "-", "FRED API", false)
        }
    }

    private fun fetchSingleYahooSpot(ticker: String): Pair<Double, Double>? {
        return try {
            val encoded = URLEncoder.encode(ticker, "UTF-8")
            val req = Request.Builder()
                .url("https://query1.finance.yahoo.com/v8/finance/chart/$encoded?interval=1d&range=5d")
                .header("User-Agent", "Mozilla/5.0 (Android; AETHERION/1.0)")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val meta = JSONObject(resp.body?.string().orEmpty())
                    .optJSONObject("chart")
                    ?.optJSONArray("result")
                    ?.optJSONObject(0)
                    ?.optJSONObject("meta") ?: return null
                val p = meta.optDouble("regularMarketPrice", Double.NaN)
                val prev = meta.optDouble("chartPreviousClose", p)
                if (p.isNaN() || p <= 0.0) null else p to (if (prev > 0) ((p - prev) / prev) * 100.0 else 0.0)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchFmpEconomicCalendar(apiKey: String): List<EconomicCalendarEvent> {
        return try {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            val from = fmt.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 3)
            val to = fmt.format(cal.time)
            val url = "https://financialmodelingprep.com/api/v3/economic_calendar?from=$from&to=$to&apikey=$apiKey"
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful || !body.trimStart().startsWith("[")) return emptyList()
                val arr = JSONArray(body)
                val list = mutableListOf<EconomicCalendarEvent>()
                for (i in 0 until minOf(arr.length(), 10)) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        EconomicCalendarEvent(
                            eventName = o.optString("event", "Economic Release"),
                            country = o.optString("country", "US"),
                            impact = o.optString("impact", "Medium"),
                            timeUtc = o.optString("date", ""),
                            actual = o.optString("actual", "-"),
                            estimate = o.optString("estimate", "-"),
                            previous = o.optString("previous", "-"),
                            sourceName = "FMP Calendar"
                        )
                    )
                }
                list
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun fetchFinnhubEconomicCalendar(apiKey: String): List<EconomicCalendarEvent> {
        return try {
            val req = Request.Builder()
                .url("https://finnhub.io/api/v1/calendar/economic?token=$apiKey")
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful || body.isBlank()) return emptyList()
                val arr = JSONObject(body).optJSONArray("economicCalendar") ?: return emptyList()
                val list = mutableListOf<EconomicCalendarEvent>()
                for (i in 0 until minOf(arr.length(), 10)) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        EconomicCalendarEvent(
                            eventName = o.optString("event", "Macro Event"),
                            country = o.optString("country", "US"),
                            impact = o.optString("impact", "medium"),
                            timeUtc = o.optString("time", ""),
                            actual = o.optString("actual", "-"),
                            estimate = o.optString("estimate", "-"),
                            previous = o.optString("prev", "-"),
                            sourceName = "Finnhub Calendar"
                        )
                    )
                }
                list
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchLiveMacroHeadlines(
        settings: ProviderSettings = ProviderSettings()
    ): Result<List<NewsHeadline>> = withContext(Dispatchers.IO) {
        val merged = mutableListOf<NewsHeadline>()
        val fhKey = ApiSecretsResolver.resolveFinnhubKey(settings)
        if (fhKey.isNotBlank()) {
            merged.addAll(fetchFinnhubNews(fhKey))
        }
        val fmpKey = ApiSecretsResolver.resolveFmpKey(settings)
        if (fmpKey.isNotBlank()) {
            merged.addAll(fetchFmpNews(fmpKey))
        }

        val url = "https://feeds.finance.yahoo.com/rss/2.0/headline?s=GC=F,BTC-USD,EURUSD=X,^GSPC&region=US&lang=en-US"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Android; AETHERION/1.0)")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                val xml = response.body?.string().orEmpty()
                if (response.isSuccessful && xml.isNotBlank()) {
                    merged.addAll(parseRss(xml))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Yahoo RSS failed: ${e.message}")
        }

        if (merged.isEmpty()) {
            Result.failure(IllegalStateException("No headlines retrieved from Finnhub, FMP, or Yahoo Finance RSS."))
        } else {
            Result.success(merged.distinctBy { it.title }.take(20))
        }
    }

    private fun fetchFinnhubNews(apiKey: String): List<NewsHeadline> {
        return try {
            val req = Request.Builder()
                .url("https://finnhub.io/api/v1/news?category=general&token=$apiKey")
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful || !body.trimStart().startsWith("[")) return emptyList()
                val arr = JSONArray(body)
                val out = mutableListOf<NewsHeadline>()
                for (i in 0 until minOf(arr.length(), 8)) {
                    val obj = arr.getJSONObject(i)
                    val headline = obj.optString("headline")
                    if (headline.isNotBlank()) {
                        out.add(
                            NewsHeadline(
                                title = headline,
                                source = "Finnhub · ${obj.optString("source", "Wire")}",
                                publishedAt = "Live API",
                                link = obj.optString("url", "")
                            )
                        )
                    }
                }
                out
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun fetchFmpNews(apiKey: String): List<NewsHeadline> {
        return try {
            val req = Request.Builder()
                .url("https://financialmodelingprep.com/api/v3/fmp/articles?page=0&size=8&apikey=$apiKey")
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful || body.isBlank()) return emptyList()
                val arr = JSONObject(body).optJSONArray("content") ?: return emptyList()
                val out = mutableListOf<NewsHeadline>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val title = obj.optString("title")
                    if (title.isNotBlank()) {
                        out.add(
                            NewsHeadline(
                                title = title,
                                source = "FMP Research Wire",
                                publishedAt = obj.optString("date", "Live API"),
                                link = obj.optString("link", "")
                            )
                        )
                    }
                }
                out
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun testDataProviderConnection(
        providerId: String,
        settings: ProviderSettings
    ): ProviderDiagnostic = withContext(Dispatchers.IO) {
        val t0 = System.currentTimeMillis()
        try {
            when (providerId) {
                "FMP" -> {
                    val key = ApiSecretsResolver.resolveFmpKey(settings)
                    if (key.isBlank()) return@withContext ProviderDiagnostic("FMP", false, "KEY MISSING", 0, "Add FMP_API_KEY in Secrets or Integrations Settings.")
                    val req = Request.Builder().url("https://financialmodelingprep.com/api/v3/quote/AAPL?apikey=$key").build()
                    client.newCall(req).execute().use { resp ->
                        val lat = System.currentTimeMillis() - t0
                        val body = resp.body?.string().orEmpty()
                        val ok = resp.isSuccessful && body.trimStart().startsWith("[") && !body.contains("Error Message")
                        ProviderDiagnostic("FMP", ok, if (ok) "CONNECTED" else "HTTP ${resp.code}", lat, body.take(140))
                    }
                }
                "FRED" -> {
                    val key = ApiSecretsResolver.resolveFredKey(settings)
                    if (key.isBlank()) return@withContext ProviderDiagnostic("FRED", false, "KEY MISSING", 0, "Add FRED_API_KEY in Secrets or Integrations Settings.")
                    val req = Request.Builder().url("https://api.stlouisfed.org/fred/series/observations?series_id=DFF&api_key=$key&file_type=json&limit=1&sort_order=desc").build()
                    client.newCall(req).execute().use { resp ->
                        val lat = System.currentTimeMillis() - t0
                        val body = resp.body?.string().orEmpty()
                        val ok = resp.isSuccessful && body.contains("observations")
                        ProviderDiagnostic("FRED", ok, if (ok) "CONNECTED" else "HTTP ${resp.code}", lat, body.take(140))
                    }
                }
                "TWELVE_DATA" -> {
                    val key = ApiSecretsResolver.resolveTwelveDataKey(settings)
                    if (key.isBlank()) return@withContext ProviderDiagnostic("Twelve Data", false, "KEY MISSING", 0, "Add TWELVE_DATA_API_KEY in Secrets or Integrations Settings.")
                    val req = Request.Builder().url("https://api.twelvedata.com/price?symbol=EUR/USD&apikey=$key").build()
                    client.newCall(req).execute().use { resp ->
                        val lat = System.currentTimeMillis() - t0
                        val body = resp.body?.string().orEmpty()
                        val ok = resp.isSuccessful && body.contains("price") && !body.contains("error")
                        ProviderDiagnostic("Twelve Data", ok, if (ok) "CONNECTED" else "HTTP ${resp.code}", lat, body.take(140))
                    }
                }
                "FINNHUB" -> {
                    val key = ApiSecretsResolver.resolveFinnhubKey(settings)
                    if (key.isBlank()) return@withContext ProviderDiagnostic("Finnhub", false, "KEY MISSING", 0, "Add FINNHUB_API_KEY in Secrets or Integrations Settings.")
                    val req = Request.Builder().url("https://finnhub.io/api/v1/quote?symbol=AAPL&token=$key").build()
                    client.newCall(req).execute().use { resp ->
                        val lat = System.currentTimeMillis() - t0
                        val body = resp.body?.string().orEmpty()
                        val ok = resp.isSuccessful && body.contains("\"c\"")
                        ProviderDiagnostic("Finnhub", ok, if (ok) "CONNECTED" else "HTTP ${resp.code}", lat, body.take(140))
                    }
                }
                "DEEPSEEK" -> {
                    val key = ApiSecretsResolver.resolveDeepSeekKey(settings)
                    if (key.isBlank()) return@withContext ProviderDiagnostic("DeepSeek", false, "KEY MISSING", 0, "Add DEEPSEEK_API_KEY in Secrets or Integrations Settings.")
                    val payload = JSONObject().apply {
                        put("model", settings.deepSeekModel.ifBlank { "deepseek-chat" })
                        put("messages", JSONArray().put(JSONObject().apply {
                            put("role", "user")
                            put("content", "Reply OK")
                        }))
                        put("max_tokens", 10)
                    }
                    val req = Request.Builder()
                        .url("https://api.deepseek.com/chat/completions")
                        .header("Authorization", "Bearer $key")
                        .post(payload.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                    client.newCall(req).execute().use { resp ->
                        val lat = System.currentTimeMillis() - t0
                        val body = resp.body?.string().orEmpty()
                        val ok = resp.isSuccessful && body.contains("choices")
                        ProviderDiagnostic("DeepSeek", ok, if (ok) "CONNECTED" else "HTTP ${resp.code}", lat, body.take(140))
                    }
                }
                else -> ProviderDiagnostic(providerId, false, "UNKNOWN", 0, "Unsupported provider ID")
            }
        } catch (e: Exception) {
            ProviderDiagnostic(providerId, false, "ERROR", System.currentTimeMillis() - t0, e.localizedMessage ?: "Connection failed")
        }
    }

    private fun parseRss(xml: String): List<NewsHeadline> {
        val headlines = mutableListOf<NewsHeadline>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var insideItem = false
        var title = ""
        var link = ""
        var pubDate = ""
        var currentTag = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag == "item") {
                        insideItem = true
                        title = ""
                        link = ""
                        pubDate = ""
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideItem) {
                        when (currentTag) {
                            "title" -> title += parser.text
                            "link" -> link += parser.text
                            "pubDate" -> pubDate += parser.text
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "item" && insideItem) {
                        if (title.isNotBlank()) {
                            headlines.add(
                                NewsHeadline(
                                    title = title.trim(),
                                    source = "Yahoo Finance RSS",
                                    publishedAt = pubDate.trim().ifBlank { "Live Feed" },
                                    link = link.trim()
                                )
                            )
                        }
                        insideItem = false
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }
        return headlines
    }

    companion object {
        private const val TAG = "LiveMarketDataService"

        fun computeActiveSessionsUtc(): String {
            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            val hour = utcCal.get(Calendar.HOUR_OF_DAY)
            val sessions = mutableListOf<String>()
            if (hour in 21..23 || hour in 0..5) sessions.add("Sydney")
            if (hour in 0..8) sessions.add("Tokyo")
            if (hour in 7..15) sessions.add("London")
            if (hour in 12..20) sessions.add("New York")
            val active = if (sessions.isEmpty()) "After-Hours Crypto/FX" else sessions.joinToString(" + ")
            val overlap = if ("London" in sessions && "New York" in sessions) " (Peak Liquidity Overlap)" else ""
            return "$active · ${String.format(Locale.US, "%02d:%02d UTC", hour, utcCal.get(Calendar.MINUTE))}$overlap"
        }

        fun computeAnnualizedVolatilityPct(candles: List<CandleBar>): Double {
            if (candles.size < 4) return 0.0
            val returns = mutableListOf<Double>()
            for (i in 1 until candles.size) {
                val prev = candles[i - 1].close
                val curr = candles[i].close
                if (prev > 0.0 && curr > 0.0) {
                    returns.add(ln(curr / prev))
                }
            }
            if (returns.size < 2) return 0.0
            val mean = returns.average()
            val variance = returns.sumOf { (it - mean).pow(2) } / (returns.size - 1)
            val hourlyStd = sqrt(variance)
            return (hourlyStd * sqrt(252.0 * 24.0) * 100.0).coerceIn(0.0, 400.0)
        }
    }
}
