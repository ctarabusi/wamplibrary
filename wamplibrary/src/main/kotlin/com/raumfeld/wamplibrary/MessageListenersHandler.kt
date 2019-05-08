package com.raumfeld.wamplibrary

import java.util.concurrent.ConcurrentHashMap

class MessageListenersHandler {

    private val requestIdListeners = ConcurrentHashMap<Long, (Message) -> Unit>()

    fun notifyListeners(message: Message) {
        if (message is RequestMessage) requestIdListeners.remove(message.requestId)?.invoke(message)
    }

    fun registerListener(requestId: Long, action: (Message) -> Unit) {
        if (requestIdListeners.containsKey(requestId)) throw IllegalArgumentException("Already listening for $requestId")
        requestIdListeners[requestId] = action
    }
}