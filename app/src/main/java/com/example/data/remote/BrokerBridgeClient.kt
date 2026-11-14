package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

data class BrokerHealthReport(
    val bridgeName: String,
    val connected: Boolean,
    val statusLabel: String,
    val latencyMs: Long,
    val accountBalance: Double? = null,
    val accountEquity: Double? = null,
    val openPositionsCount: Int? = null,
    val errorDetails: String? = null
)

class BrokerBridgeClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun pingMt5Bridge(baseUrl: String, authToken: String): BrokerHealthReport =
        withContext(Dispatchers.IO) {
            val cleanUrl = baseUrl.trim().trimEnd('/')
            if (cleanUrl.isBlank()) {
                return@withContext BrokerHealthReport(
                    bridgeName = "MetaTrader 5 EA Bridge",
                    connected = false,
                    statusLabel = "DISCONNECTED · NO ENDPOINT",
                    latencyMs = 0L,
                    errorDetails = "Configure your MT5 EA REST Bridge URL (e.g. https://your-vps:8443) in Integrations Settings."
                )
            }
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                return@withContext BrokerHealthReport(
                    bridgeName = "MetaTrader 5 EA Bridge",
                    connected = false,
                    statusLabel = "INVALID URL",
                    latencyMs = 0L,
                    errorDetails = "Bridge URL must begin with http:// or https://"
                )
            }

            val t0 = System.currentTimeMillis()
            val reqBuilder = Request.Builder().url("$cleanUrl/health")
            if (authToken.isNotBlank()) {
                reqBuilder.header("Authorization", "Bearer ${authToken.trim()}")
            }
            try {
                client.newCall(reqBuilder.build()).execute().use { resp ->
                    val lat = System.currentTimeMillis() - t0
                    val body = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        return@withContext BrokerHealthReport(
                            bridgeName = "MetaTrader 5 EA Bridge",
                            connected = false,
                            statusLabel = "ERROR HTTP ${resp.code}",
                            latencyMs = lat,
                            errorDetails = body.take(200).ifBlank { "Bridge returned HTTP ${resp.code}" }
                        )
                    }
                    val json = runCatching { JSONObject(body) }.getOrNull()
                    BrokerHealthReport(
                        bridgeName = "MetaTrader 5 EA Bridge",
                        connected = true,
                        statusLabel = "LIVE BRIDGE CONNECTED",
                        latencyMs = lat,
                        accountBalance = json?.optDouble("balance")?.takeIf { !it.isNaN() },
                        accountEquity = json?.optDouble("equity")?.takeIf { !it.isNaN() },
                        openPositionsCount = json?.optInt("openPositions")
                    )
                }
            } catch (e: Exception) {
                BrokerHealthReport(
                    bridgeName = "MetaTrader 5 EA Bridge",
                    connected = false,
                    statusLabel = "UNREACHABLE",
                    latencyMs = System.currentTimeMillis() - t0,
                    errorDetails = e.localizedMessage ?: "Connection refused / timeout"
                )
            }
        }

    suspend fun submitOrderToMt5Bridge(
        baseUrl: String,
        authToken: String,
        symbol: String,
        side: String,
        volumeLots: Double,
        stopLoss: Double,
        takeProfit: Double
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanUrl = baseUrl.trim().trimEnd('/')
        if (cleanUrl.isBlank()) {
            return@withContext Result.failure(IllegalStateException("MT5 Bridge URL not configured."))
        }
        val orderId = "AETH_${UUID.randomUUID().toString().take(10)}"
        val payload = JSONObject().apply {
            put("clientOrderId", orderId)
            put("symbol", symbol)
            put("side", side)
            put("volume", volumeLots)
            put("stopLoss", stopLoss)
            put("takeProfit", takeProfit)
            put("timestampMs", System.currentTimeMillis())
        }
        val reqBuilder = Request.Builder()
            .url("$cleanUrl/order")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
        if (authToken.isNotBlank()) {
            reqBuilder.header("Authorization", "Bearer ${authToken.trim()}")
        }
        try {
            client.newCall(reqBuilder.build()).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    Result.failure(IllegalStateException("MT5 Bridge HTTP ${resp.code}: ${body.take(150)}"))
                } else {
                    Result.success("MT5 Ack ($orderId): ${body.take(100)}")
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
