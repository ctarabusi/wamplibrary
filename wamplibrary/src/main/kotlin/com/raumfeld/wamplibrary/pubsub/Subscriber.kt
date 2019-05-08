package com.raumfeld.wamplibrary.pubsub

import com.raumfeld.wamplibrary.*
import java.util.concurrent.ConcurrentHashMap

internal class Subscriber(
        private val client: Client,
        private val randomIdGenerator: RandomIdGenerator,
        private val messageListenersHandler: MessageListenersHandler
) {
    private val subscriptions = ConcurrentHashMap<Long, EventHandler>()

    fun subscribe(topic: String, eventHandler: EventHandler,
                  subscribedHandler: SubscribedHandler) {
        createSubscription(topic) { subscribed ->
                subscribedHandler.invoke(subscribed)
                subscriptions[subscribed.subscription] = eventHandler
        }
    }

    private fun createSubscription(topic: String,
                                   onSubscribed: (Subscribed) -> Unit) {
        randomIdGenerator.newRandomId().also { requestId ->
            messageListenersHandler.registerListener(requestId) { message ->
                (message as? Subscribed)?.let {
                    Logger.i("Subscribed " + it.requestId)
                    onSubscribed.invoke(it)
                }
            }
            client.send(Subscribe(requestId, emptyMap(), topic))
        }
    }

    fun receiveEvent(eventMessage: Event) {
        subscriptions[eventMessage.subscription]?.invoke(
                eventMessage.arguments,
                eventMessage.argumentsKw
        )
                ?: Logger.w("Got an event (${eventMessage.publication}) for a subscription we don't have (${eventMessage.subscription})")
    }
}

typealias SubscribedHandler = (subscribed: Subscribed) -> Unit
typealias EventHandler = (arguments: List<Any>?, argumentsKw: Map<String, Any>?) -> Unit