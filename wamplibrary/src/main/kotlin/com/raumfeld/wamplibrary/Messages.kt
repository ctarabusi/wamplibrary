package com.raumfeld.wamplibrary

import android.net.Uri

enum class MessageType {
    HELLO,
    WELCOME,
    ABORT,
    GOODBYE,
    ERROR
}
abstract class Message(val type: MessageType)

data class Hello(val realm: Uri, val details: Map<String, Any>) : Message(MessageType.HELLO)
data class Welcome(val session: Long, val details: Map<String, Any>) : Message(MessageType.WELCOME)
