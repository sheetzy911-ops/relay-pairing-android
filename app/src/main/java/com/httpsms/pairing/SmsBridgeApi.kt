package com.httpsms.pairing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

@Serializable
data class BridgeMessage(
    val contact: String = "",
    val content: String = "",
    val sim: String = "SIM1",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("failure_reason") val failureReason: String? = null,
    val id: String = "",
    @SerialName("last_attempted_at") val lastAttemptedAt: String? = null,
    @SerialName("order_timestamp") val orderTimestamp: String = "",
    val owner: String = "",
    @SerialName("received_at") val receivedAt: String? = null,
    val encrypted: Boolean = false,
    @SerialName("request_received_at") val requestReceivedAt: String = "",
    @SerialName("send_time") val sendTime: Long? = null,
    @SerialName("sent_at") val sentAt: String? = null,
    val status: String = "",
    val type: String = "SMS",
    @SerialName("updated_at") val updatedAt: String = "",
    val attachments: List<String>? = null
)

@Serializable
data class BridgeResponse<T>(val data: T? = null, val message: String? = null, val status: String? = null)

@Serializable
data class MessageEventRequest(
    @SerialName("event_name") val eventName: String,
    val reason: String? = null,
    val timestamp: String
)

@Serializable
data class HeartbeatRequest(
    val charging: Boolean,
    @SerialName("phone_numbers") val phoneNumbers: List<String>
)

@Serializable
data class ReceivedAttachment(
    val name: String,
    @SerialName("content_type") val contentType: String,
    val content: String
)

@Serializable
data class ReceivedMessageRequest(
    val sim: String,
    val from: String,
    val to: String,
    val content: String,
    val encrypted: Boolean,
    val timestamp: String,
    val attachments: List<ReceivedAttachment>? = null
)

class SmsBridgeApi(private val relay: RelayApiClient) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    @Throws(IOException::class)
    fun getOutstandingMessage(messageId: String): BridgeMessage? {
        require(messageId.isNotBlank()) { "messageId cannot be blank" }
        val request = relay.authenticatedRequest(
            "/v1/messages/outstanding?message_id=${java.net.URLEncoder.encode(messageId, Charsets.UTF_8.name())}"
        )
        return execute(request) { body ->
            json.decodeFromString<BridgeResponse<BridgeMessage>>(body).data
        }
    }

    @Throws(IOException::class)
    fun sendSentEvent(messageId: String, timestamp: String): Boolean =
        sendEvent(messageId, "SENT", timestamp, null)

    @Throws(IOException::class)
    fun sendDeliveredEvent(messageId: String, timestamp: String): Boolean =
        sendEvent(messageId, "DELIVERED", timestamp, null)

    @Throws(IOException::class)
    fun sendFailedEvent(messageId: String, timestamp: String, reason: String): Boolean =
        sendEvent(messageId, "FAILED", timestamp, reason)

    @Throws(IOException::class)
    fun storeHeartbeat(phoneNumbers: List<String>, charging: Boolean): Boolean {
        val body = json.encodeToString(HeartbeatRequest.serializer(), HeartbeatRequest(charging, phoneNumbers))
        return executeJson(
            relay.authenticatedRequest("/v1/heartbeats").newBuilder()
                .post(body.toRequestBody(mediaType)).build()
        )
    }

    @Throws(IOException::class)
    fun receive(requestPayload: ReceivedMessageRequest): Boolean {
        val body = json.encodeToString(ReceivedMessageRequest.serializer(), requestPayload)
        return executeJson(
            relay.authenticatedRequest("/v1/messages/receive").newBuilder()
                .post(body.toRequestBody(mediaType)).build()
        )
    }

    private fun sendEvent(messageId: String, event: String, timestamp: String, reason: String?): Boolean {
        require(messageId.isNotBlank()) { "messageId cannot be blank" }
        val body = json.encodeToString(
            MessageEventRequest.serializer(), MessageEventRequest(event, reason, timestamp)
        )
        val request = relay.authenticatedRequest("/v1/messages/$messageId/events").newBuilder()
            .post(body.toRequestBody(mediaType)).build()
        return executeJson(request, treatNotFoundAsSuccess = true)
    }

    private fun <T> execute(request: Request, parser: (String) -> T): T? {
        return relay.execute(request) { body -> parser(body) }
    }

    private fun executeJson(request: Request, treatNotFoundAsSuccess: Boolean = false): Boolean {
        return relay.execute(request, treatNotFoundAsSuccess) { true } ?: false
    }
}
