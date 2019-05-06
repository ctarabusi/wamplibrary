package com.raumfeld.wamplibrary

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class MessageListenersHandler(val coroutineScope: CoroutineScope) {

    private val requestIdListeners =
            ConcurrentHashMap<RequestListenerKey, CompletableDeferred<Message>>()

    fun notifyListeners(message: Message) {
        if (message is RequestMessage)
            requestIdListeners.remove(RequestListenerKey(message.requestId))?.complete(message)
    }

    fun <T : Message> registerListener(requestId: Long): Deferred<T> {
        val messageListener = registerToMessageListenerMap(requestIdListeners, RequestListenerKey(requestId))
        return coroutineScope.async {
            messageListener.await() as T
        }.apply {
            invokeOnCompletion {
                requestIdListeners.remove(RequestListenerKey(requestId))
            }
        }
    }

    fun <T : Message> registerListenerWithErrorHandler(requestId: Long) =
            CompletableDeferred<T>().also {
                applyListenersToDeferred(it, requestId)
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

    private fun <T> registerToMessageListenerMap(
            listenerMap: MutableMap<T, CompletableDeferred<Message>>,
            index: T
    ): CompletableDeferred<Message> {
        if (listenerMap.containsKey(index)) throw IllegalArgumentException("Already listening for $index")
        val completableDeferredMessage = CompletableDeferred<Message>()
        listenerMap[index] = completableDeferredMessage
        return completableDeferredMessage
    }
}

data class RequestListenerKey(val requestId: Long)
