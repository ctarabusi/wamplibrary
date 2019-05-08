package com.raumfeld.wamplibrary

import com.raumfeld.wamplibrary.pubsub.*
import com.raumfeld.wamplibrary.pubsub.Publisher
import com.raumfeld.wamplibrary.pubsub.Subscriber
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

interface Client {

    fun publish(
            topic: String,
            arguments: List<JsonElement>,
            argumentsKw: WampDict,
            onPublished: ((Long) -> Unit)? = null
    )

    fun subscribe(topicPattern: String, onEventHandler: EventHandler, onSubscribedHandler: SubscribedHandler)

    fun send(message: Message)
}

class ClientImpl(val realm: String) : Client, WebSocketCallback {

    private var sessionId: Long? = null

    private val randomIdGenerator = RandomIdGenerator()

    private val messageListenersHandler = MessageListenersHandler()

    private val publisher = Publisher(this, randomIdGenerator, messageListenersHandler)
    private val subscriber = Subscriber(this, randomIdGenerator, messageListenersHandler)

    private var webSocketDelegate: WebSocketDelegate? = null

    override fun onOpen(webSocketDelegate: WebSocketDelegate) {
        this.webSocketDelegate = webSocketDelegate

        joinRealm(realm)
    }

    override fun onMessage(messageJson: String) {
        Logger.d("Received json: $messageJson")
        val message = fromJsonToMessage(messageJson)
        Logger.d("Mapped to: $message")

        messageListenersHandler.notifyListeners(message)

        when (message) {
            //     is Invocation -> callee.invokeProcedure(message)
            is Welcome -> {
                Logger.i("Session established. ID: ${message.session}")
                sessionId = message.session
            }
            is Event   -> subscriber.receiveEvent(message)
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
        messageListenersHandler.registerListener(randomIdGenerator.newRandomId()) { message ->
            (message as? Goodbye)?.let {
                Logger.i("Router replied goodbye. Reason: ${it.reason}")
            }
        }

        send(Goodbye(emptyMap(), reason = WampClose.SYSTEM_SHUTDOWN.uri))
    }

    override fun publish(
            topic: String,
            arguments: List<JsonElement>,
            argumentsKw: WampDict,
            onPublished: ((Long) -> Unit)?
    ) = publisher.publish(topic, arguments, argumentsKw, onPublished)

    override fun subscribe(
            topicPattern: String,
            onEventHandler: EventHandler,
            onSubscribedHandler: SubscribedHandler
    ) = subscriber.subscribe(topicPattern, onEventHandler, onSubscribedHandler)
}