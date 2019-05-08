package com.raumfeld.wamplibrary

import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

interface WampClient {

    fun send(message: Message)

    fun onSessionReady(onSessionReady: (WampSession) -> Unit)
}

class WampClientImpl(val realm: String) : WampClient, WebSocketCallback {

    val wampSession = WampSession(this)

    private var webSocketDelegate: WebSocketDelegate? = null

    private var onSessionReady: ((WampSession) -> Unit)? = null

    override fun onOpen(webSocketDelegate: WebSocketDelegate) {
        this.webSocketDelegate = webSocketDelegate

        joinRealm(realm)
    }

    override fun onSessionReady(onSessionReady: (WampSession) -> Unit) {
        this.onSessionReady = onSessionReady
    }

    override fun onMessage(messageJson: String) {
        Logger.d("Received json: $messageJson")
        val message = fromJsonToMessage(messageJson)
        Logger.d("Mapped to: $message")

        wampSession.notifyListeners(message)

        when (message) {
            //     is Invocation -> callee.invokeProcedure(message)
            is Welcome -> {
                Logger.i("Session established. ID: ${message.session}")
                wampSession.sessionId = message.session

                onSessionReady?.invoke(wampSession)
            }
            is Event   -> wampSession.receiveEvent(message)
        }
    }

    @UnstableDefault
    override fun send(message: Message) = webSocketDelegate?.send(message.toJson()) ?: Unit

    private fun joinRealm(realmUri: String) = send(Hello(
            realmUri, mapOf(
            "roles" to JsonObject(mapOf<String, JsonElement>(
                    "publisher" to JsonObject(emptyMap()),
                    "subscriber" to JsonObject(emptyMap()),
                    "caller" to JsonObject(emptyMap()),
                    "callee" to JsonObject(emptyMap())
            ))
    ))
    )

    override fun onClosing() {
        wampSession.onClosing()
    }
}