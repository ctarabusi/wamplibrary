package com.raumfeld.wamplibrary

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException

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

    fun <R> onNextMessage(action: suspend (Message) -> R): Deferred<R> =
            coroutineScope.async {
                processRawMessage(incoming.receive(), action)
            }

    suspend inline fun <reified T : Message, R> withNextMessage(crossinline action: suspend (message: T) -> R) =
            onNextMessage {
                when (it) {
                    is T -> action(it)
                    else -> throw IllegalArgumentException() //UnexpectedMessageException(T::class, it::class)
                }
            }
}