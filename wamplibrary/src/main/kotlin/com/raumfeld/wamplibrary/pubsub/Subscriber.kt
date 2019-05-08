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
                  subscribedHandler: SubscribedHandler): SubscriptionHandle {
        val requestIdSubscribed = createSubscription(topic) {
            subscribed -> subscribedHandler.invoke(subscribed)
        }
        subscriptions[requestIdSubscribed] = eventHandler
        return SubscriptionHandle { unsubscribe(requestIdSubscribed) }
    }

    private fun createSubscription(topic: String,
                                   onSubscribed: (Subscribed) -> Unit): Long {
        randomIdGenerator.newRandomId().also { requestId ->
            messageListenersHandler.registerListener(requestId) { message ->
                (message as? Subscribed)?.let {
                    Logger.i("Subscribed " + it.requestId)
                    onSubscribed.invoke(it)
                }
            }
            client.send(Subscribe(requestId, emptyMap(), topic))
            return requestId
        }
    }

    private fun unsubscribe(subscriptionId: Long) {
        subscriptions.remove(subscriptionId)
        unsubscribeFromRouter(subscriptionId)
    }

    private fun unsubscribeFromRouter(subscriptionId: Long) {
        randomIdGenerator.newRandomId().also { requestId ->
            messageListenersHandler.registerListener(requestId) { message ->
                (message as? Unsubscribed)?.let {
                    Logger.i("Unsubscribed " + it.requestId)
                }
            }

            client.send(Unsubscribe(requestId, subscriptionId))
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

class SubscriptionHandle(val unsubscribeCallback: () -> Unit) {
    fun unsubscribe() = unsubscribeCallback()
}