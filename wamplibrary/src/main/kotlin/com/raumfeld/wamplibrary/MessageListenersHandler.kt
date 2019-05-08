package com.raumfeld.wamplibrary

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class MessageListenersHandler(val coroutineScope: CoroutineScope) {

    private val requestIdListeners = ConcurrentHashMap<Long, CompletableDeferred<Message>>()

    fun notifyListeners(message: Message) {
        if (message is RequestMessage) requestIdListeners.remove(message.requestId)?.complete(message)
    }

    fun <T : Message> registerListenerWithErrorHandler(requestId: Long) =
            CompletableDeferred<T>().also {
                applyListenersToDeferred(it, requestId)
            }

    private fun <T : Message> registerListener(requestId: Long): Deferred<T> {
        val messageListener = registerToMessageListenerMap(requestId)
        return coroutineScope.async {
            messageListener.await() as T
        }.apply {
            invokeOnCompletion {
                requestIdListeners.remove(requestId)
            }
        }
    }

    private fun <T : Message> applyListenersToDeferred(
            completableDeferredMessage: CompletableDeferred<T>,
            requestId: Long
    ) {
        val deferredMessage = registerListener<T>(requestId)
        coroutineScope.launch {
            val message = deferredMessage.await()
            completableDeferredMessage.complete(message)
        }
    }

    private fun registerToMessageListenerMap(requestId: Long): CompletableDeferred<Message> {
        if (requestIdListeners.containsKey(requestId)) throw IllegalArgumentException("Already listening for $requestId")
        val completableDeferredMessage = CompletableDeferred<Message>()
        requestIdListeners[requestId] = completableDeferredMessage
        return completableDeferredMessage
    }
}