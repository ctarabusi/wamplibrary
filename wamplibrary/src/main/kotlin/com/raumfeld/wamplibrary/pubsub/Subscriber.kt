package com.raumfeld.wamplibrary.pubsub

import com.raumfeld.wamplibrary.*
import java.util.concurrent.ConcurrentHashMap

internal class Subscriber(
        private val connection: Connection,
        private val randomIdGenerator: RandomIdGenerator,
        private val messageListenersHandler: MessageListenersHandler
) {
    private val subscriptions = ConcurrentHashMap<Long, EventHandler>()

    suspend fun subscribe(topic: String, eventHandler: EventHandler): SubscriptionHandle {
        val subscribed = createSubscription(topic)
        subscriptions[subscribed.subscription] = eventHandler
        return SubscriptionHandle { unsubscribe(subscribed.subscription) }
    }

    private suspend fun createSubscription(topic: String): Subscribed {
        randomIdGenerator.newRandomId().also { requestId ->
            val messageListener = messageListenersHandler.registerListenerWithErrorHandler<Subscribed>(requestId)

            connection.send(Subscribe(requestId, emptyMap(), topic))

            return messageListener.await()
        }
    }

    private suspend fun unsubscribe(subscriptionId: Long) {
        subscriptions.remove(subscriptionId)
        unsubscribeFromRouter(subscriptionId)
    }

    private suspend fun unsubscribeFromRouter(subscriptionId: Long) {
        randomIdGenerator.newRandomId().also { requestId ->
            val messageListener = messageListenersHandler.registerListenerWithErrorHandler<Unsubscribed>(requestId)

            connection.send(Unsubscribe(requestId, subscriptionId))
            messageListener.await()
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

typealias EventHandler = (arguments: List<Any>?, argumentsKw: Map<String, Any>?) -> Unit

class SubscriptionHandle(val unsubscribeCallback: suspend () -> Unit) {
    suspend fun unsubscribe() = unsubscribeCallback()
}