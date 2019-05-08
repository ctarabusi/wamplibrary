package com.raumfeld.wamplibrary

import com.raumfeld.wamplibrary.pubsub.EventHandler
import com.raumfeld.wamplibrary.pubsub.Publisher
import com.raumfeld.wamplibrary.pubsub.SubscribedHandler
import com.raumfeld.wamplibrary.pubsub.Subscriber
import kotlinx.serialization.json.JsonElement

class WampSession(val client: WampClient) {

    var sessionId: Long? = null

    private val randomIdGenerator = RandomIdGenerator()

    private val messageListenersHandler = MessageListenersHandler()

    private val publisher = Publisher(client, randomIdGenerator, messageListenersHandler)
    private val subscriber = Subscriber(client, randomIdGenerator, messageListenersHandler)

    fun publish(topic: String,
                arguments: List<JsonElement>,
                argumentsKw: WampDict,
                onPublished: ((Long) -> Unit)?
    ) = publisher.publish(topic, arguments, argumentsKw, onPublished)

    fun subscribe(topicPattern: String,
                  onEventHandler: EventHandler,
                  onSubscribedHandler: SubscribedHandler
    ) = subscriber.subscribe(topicPattern, onEventHandler, onSubscribedHandler)

    fun notifyListeners(message: Message) = messageListenersHandler.notifyListeners(message)

    fun receiveEvent(message: Event) = subscriber.receiveEvent(message)

    fun onClosing() {
        messageListenersHandler.registerListener(randomIdGenerator.newRandomId()) { message ->
            (message as? Goodbye)?.let {
                Logger.i("Router replied goodbye. Reason: ${it.reason}")
            }
        }

        client.send(Goodbye(emptyMap(), reason = WampClose.SYSTEM_SHUTDOWN.content))
    }
}