package com.raumfeld.wamplibrary.pubsub


import com.raumfeld.wamplibrary.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

internal class Publisher(
        private val connection: Connection,
        private val randomIdGenerator: RandomIdGenerator,
        private val messageListenersHandler: MessageListenersHandler
) {
    suspend fun publish(topic: String, arguments: List<JsonElement>, argumentsKw: WampDict, onPublished: (suspend (Long) -> Unit)?) {
        randomIdGenerator.newRandomId().also { requestId ->
            val optionsMap = if (onPublished != null) mapOf("acknowledge" to JsonPrimitive(true)) else emptyMap()
            connection.send(Publish(requestId, optionsMap, topic, arguments, argumentsKw))

            if (onPublished != null) {
                val published = messageListenersHandler.registerListener<Published>(requestId).await()
                onPublished(published.publication)
            }
        }
    }
}