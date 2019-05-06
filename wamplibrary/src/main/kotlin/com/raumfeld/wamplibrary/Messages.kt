package com.raumfeld.wamplibrary

import kotlinx.serialization.*
import kotlinx.serialization.json.*

/*
From: https://wamp-proto.org/_static/gen/wamp_latest.html#protocol-overview
| Cod | Message        |  Pub |  Brk | Subs |  Calr | Dealr | Callee|
|-----|----------------|------|------|------|-------|-------|-------|
|  1  | `HELLO`        | Tx   | Rx   | Tx   | Tx    | Rx    | Tx    |
|  2  | `WELCOME`      | Rx   | Tx   | Rx   | Rx    | Tx    | Rx    |
|  3  | `ABORT`        | Rx   | TxRx | Rx   | Rx    | TxRx  | Rx    |
|  6  | `GOODBYE`      | TxRx | TxRx | TxRx | TxRx  | TxRx  | TxRx  |
|     |                |      |      |      |       |       |       |
|  8  | `ERROR`        | Rx   | Tx   | Rx   | Rx    | TxRx  | TxRx  |
|     |                |      |      |      |       |       |       |
| 16  | `PUBLISH`      | Tx   | Rx   |      |       |       |       |
| 17  | `PUBLISHED`    | Rx   | Tx   |      |       |       |       |
|     |                |      |      |      |       |       |       |
| 32  | `SUBSCRIBE`    |      | Rx   | Tx   |       |       |       |
| 33  | `SUBSCRIBED`   |      | Tx   | Rx   |       |       |       |
| 34  | `UNSUBSCRIBE`  |      | Rx   | Tx   |       |       |       |
| 35  | `UNSUBSCRIBED` |      | Tx   | Rx   |       |       |       |
| 36  | `EVENT`        |      | Tx   | Rx   |       |       |       |
|     |                |      |      |      |       |       |       |
| 48  | `CALL`         |      |      |      | Tx    | Rx    |       |
| 50  | `RESULT`       |      |      |      | Rx    | Tx    |       |
|     |                |      |      |      |       |       |       |
| 64  | `REGISTER`     |      |      |      |       | Rx    | Tx    |
| 65  | `REGISTERED`   |      |      |      |       | Tx    | Rx    |
| 66  | `UNREGISTER`   |      |      |      |       | Rx    | Tx    |
| 67  | `UNREGISTERED` |      |      |      |       | Tx    | Rx    |
| 68  | `INVOCATION`   |      |      |      |       | Tx    | Rx    |
| 70  | `YIELD`        |      |      |      |       | Rx    | Tx    |
 */

typealias WampMessage = List<JsonElement>

typealias WampDict = Map<String, JsonElement>

interface RequestMessage {
    val requestId: Long
}

@Serializable
abstract class Message {
    open fun toJson(): String = ""
}

@Serializable
data class Hello(val realm: String, val details: WampDict) : Message() {
    companion object {
        val TYPE: Number = 1

        fun create(array: List<JsonElement>) = Hello(realm = array[1].content, details = array[2].jsonObject.content)
    }

    override fun toJson(): String {
        val array = jsonArray {
            +TYPE
            +realm
            +JsonObject(details)
        }

        return Json.stringify(JsonArray.serializer(), array)
    }
}

@Serializable
data class Welcome(val session: Long, val details: WampDict) : Message() {
    companion object {
        const val TYPE = 2
    }
}

@Serializable
data class Abort(val details: WampDict, val reason: String) : Message() {
    companion object {
        const val TYPE = 3
    }
}

@Serializable
data class Goodbye(val details: WampDict, val reason: String) : Message() {
    companion object {
        const val TYPE = 6
    }
}

@Serializable
data class Publish(
        override val requestId: Long,
        val options: WampDict?,
        val topic: String,
        val arguments: List<Any?>? = null,
        val argumentsKw: WampDict? = null
) : Message(), RequestMessage {
    companion object {
        const val TYPE = 16
    }
}

@Serializable
data class Published(override val requestId: Long, val publication: Long) : Message(), RequestMessage {
    companion object {
        const val TYPE = 17
    }
}

@Serializable
data class Subscribe(override val requestId: Long, val options: WampDict, val topic: TopicPattern) : Message(), RequestMessage {
    companion object {
        const val TYPE = 32
    }
}

@Serializable
data class Subscribed(override val requestId: Long, val subscription: Long) : Message(), RequestMessage {
    companion object {
        const val TYPE = 33
    }
}

@Serializable
data class Unsubscribe(override val requestId: Long, val subscription: Long) : Message(), RequestMessage {
    companion object {
        const val TYPE = 34
    }
}

@Serializable
data class Unsubscribed(override val requestId: Long) : Message(), RequestMessage {
    companion object {
        const val TYPE = 35
    }
}

@Serializable
data class Event(
        val subscription: Long,
        val publication: Long,
        val details: WampDict,
        val arguments: List<JsonElement>? = null,
        val argumentsKw: WampDict? = null
) : Message() {
    companion object {
        const val TYPE = 36
    }
}



fun fromJsonToMessage(messageJson: String): Message {
    val wampMessage = Json.parse(JsonArray.serializer(), messageJson)
    return wampMessage.createMessage()
}

private fun WampMessage.createMessage() = when (this[0].intOrNull) {
    Hello.TYPE -> Hello.create(this.drop(0))
    Welcome.TYPE -> Welcome(session = this[1].long, details = this[2].jsonObject.content)
    Abort.TYPE -> Abort(details = this[1].jsonObject.content, reason = this[2].content)
    Goodbye.TYPE -> Goodbye(details = this[1].jsonObject.content, reason = this[2].content)
    Publish.TYPE -> Publish(requestId = this[1].long,
            options = this[2].jsonObject.content,
            topic = this[3].content,
            arguments = this.getOrNull(4)?.jsonArray,
            argumentsKw = this.getOrNull(5)?.jsonObject?.content
    )
    // TODO add other messages
    else -> Abort(details = emptyMap(), reason = "darum")
}

//fun main() {
//    val helloMessageJson = "[1, \"somerealm\", {\"roles\": {\"publisher\": {},\"subscriber\": {}}}]"
//    val helloMessage: Message = fromJsonToMessage(helloMessageJson)
//    println(helloMessage)
//
//    println("spitting out " + helloMessage.toJson())
//
//    val messageJson = "[16, 239714735, {}, \"com.myapp.mytopic1\", [], {\"color\": \"orange\", \"sizes\": [23, 42, 7]}]"
//    val message: Message = fromJsonToMessage(messageJson)
//    println(message)
//}
