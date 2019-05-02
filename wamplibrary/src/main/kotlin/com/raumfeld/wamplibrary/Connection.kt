package com.raumfeld.wamplibrary

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class Connection(
        private val incoming: ReceiveChannel<String>,
        private val outgoing: SendChannel<String>
) : CoroutineScope by CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {

    fun forEachMessage(exceptionHandler: (Throwable) -> Unit, action: suspend (String) -> Unit) =
        launch {
            incoming.consumeEach { message ->
                try {
                    Log.d("WAMP", "message received: $message")
                    action(message)
                } catch (throwable: Throwable) {
                    exceptionHandler(throwable)
                }
            }
        }

    fun send(message: Message) {

    }
}