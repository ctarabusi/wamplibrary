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

abstract class Message {
    open fun toJson(): String = ""
}

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

data class Welcome(val session: Long, val details: WampDict) : Message() {
    companion object {
        val TYPE: Number = 2
    }
}

data class Abort(val details: WampDict, val reason: String) : Message() {
    companion object {
        val TYPE: Number = 3
    }

    override fun toJson(): String {
        val array = jsonArray {
            +TYPE
            +JsonObject(details)
            +reason
        }
        return Json.stringify(JsonArray.serializer(), array)
    }
}

data class Goodbye(val details: WampDict, val reason: String) : Message() {
    companion object {
        val TYPE: Number = 6
    }

    override fun toJson(): String {
        val array = jsonArray {
            +TYPE
            +JsonObject(details)
            +reason
        }
        return Json.stringify(JsonArray.serializer(), array)
    }
}

data class Publish(
        override val requestId: Long,
        val options: WampDict,
        val topic: String,
        val arguments: List<JsonElement>,
        val argumentsKw: WampDict
) : Message(), RequestMessage {
    companion object {
        val TYPE: Number = 16
    }

    override fun toJson(): String {
        val array = jsonArray {
            +TYPE
            +(requestId as Number)
            +JsonObject(options)
            +topic
            +JsonArray(arguments)
            +JsonObject(argumentsKw)
        }
        return Json.stringify(JsonArray.serializer(), array)
    }
}

data class Published(override val requestId: Long, val publication: Long) : Message(), RequestMessage {
    companion object {
        val TYPE: Number = 17
    }

    override fun toJson(): String {
        val array = jsonArray {
            +TYPE
            +(requestId as Number)
            +(publication as Number)
        }

        return Json.stringify(JsonArray.serializer(), array)
    }
}

data class Subscribe(override val requestId: Long, val options: WampDict, val topic: TopicPattern) : Message(), RequestMessage {
    companion object {
        val TYPE: Number = 32
    }

    override fun toJson(): String {
        val array = jsonArray {
            +TYPE
            +(requestId as Number)
            +JsonObject(options)
            +topic
        }

        return Json.stringify(JsonArray.serializer(), array)
    }
}

data class Subscribed(override val requestId: Long, val subscription: Long) : Message(), RequestMessage {
    companion object {
        val TYPE: Number = 33
    }

    override fun toJson(): String {
        val array = jsonArray {
            +TYPE
            +(requestId as Number)
            +(subscription as Number)
        }

        return Json.stringify(JsonArray.serializer(), array)
    }
}

data class Unsubscribe(override val requestId: Long, val subscription: Long) : Message(), RequestMessage {
    companion object {
        val TYPE: Number = 34
    }
}

data class Unsubscribed(override val requestId: Long) : Message(), RequestMessage {
    companion object {
        val TYPE: Number = 35
    }
}

data class Event(
        val subscription: Long,
        val publication: Long,
        val details: WampDict,
        val arguments: List<JsonElement>,
        val argumentsKw: WampDict
) : Message() {
    companion object {
        const val TYPE = 36
    }

    override fun toJson(): String {
        val array = jsonArray {
            +TYPE
            +(subscription as Number)
            +(publication as Number)
            +JsonObject(details)
            +JsonArray(arguments)
            +JsonObject(argumentsKw)
        }

        return Json.stringify(JsonArray.serializer(), array)
    }
}

fun fromJsonToMessage(messageJson: String): Message {
    val wampMessage = Json.parse(JsonArray.serializer(), messageJson)
    return wampMessage.createMessage()
}

private fun WampMessage.createMessage() = when (this[0].intOrNull) {
    Hello.TYPE -> Hello.create(this.drop(0))
    Welcome.TYPE -> Welcome(session = this[1].content.toLong(), details = this[2].jsonObject.content)
    Abort.TYPE -> Abort(details = this[1].jsonObject.content, reason = this[2].content)
    Goodbye.TYPE -> Goodbye(details = this[1].jsonObject.content, reason = this[2].content)
    Publish.TYPE -> Publish(requestId = this[1].content.toLong(),
            options = this[2].jsonObject.content,
            topic = this[3].content,
            arguments = this.getOrNull(4)?.jsonArray ?: emptyList(),
            argumentsKw = this.getOrNull(5)?.jsonObject?.content ?: emptyMap()
    )
    Event.TYPE -> Event(subscription = this[1].content.toLong(), publication = this[2].content.toLong(),
            details = this.getOrNull(3)?.jsonObject?.content ?: emptyMap(),
            arguments = this.getOrNull(4)?.jsonArray ?: emptyList(),
            argumentsKw = this.getOrNull(5)?.jsonObject?.content ?: emptyMap())

    Subscribed.TYPE -> Subscribed(requestId = this[1].content.toLong(), subscription = this[2].content.toLong())
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
