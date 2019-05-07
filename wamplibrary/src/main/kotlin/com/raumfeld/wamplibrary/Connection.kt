package com.raumfeld.wamplibrary

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch

class Connection(
        private val coroutineScope: CoroutineScope,
        private val incoming: ReceiveChannel<String>,
        private val outgoing: SendChannel<String>
) {

    val log = Logger()

    fun forEachMessage(exceptionHandler: (Throwable) -> Unit, action: suspend (Message) -> Unit) = coroutineScope.launch {
        incoming.consumeEach { message ->
            try {
                processRawMessage(message, action)
            } catch (throwable: Throwable) {
                exceptionHandler(throwable)
            }
        }
    }

    private suspend fun <R> processRawMessage(messageJson: String, action: suspend (Message) -> R): R {
        log.d("Received json: $messageJson")
        val message = fromJsonToMessage(messageJson)
        log.d("Mapped to: $message")

        return action(message)
    }

    suspend fun send(message: Message) = outgoing.send(message.toJson())
}