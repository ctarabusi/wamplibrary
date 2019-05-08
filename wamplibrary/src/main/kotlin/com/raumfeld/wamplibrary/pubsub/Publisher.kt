package com.raumfeld.wamplibrary.pubsub


import com.raumfeld.wamplibrary.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

internal class Publisher(
        private val client: WampClient,
        private val randomIdGenerator: RandomIdGenerator,
        private val messageListenersHandler: MessageListenersHandler
) {
    fun publish(topic: String, arguments: List<JsonElement>, argumentsKw: WampDict, onPublished: ((Long) -> Unit)?) {
        randomIdGenerator.newRandomId().also { requestId ->
            val optionsMap = if (onPublished != null) mapOf("acknowledge" to JsonPrimitive(true)) else emptyMap()
            client.send(Publish(requestId, optionsMap, topic, arguments, argumentsKw))

            if (onPublished != null) {
                messageListenersHandler.registerListener(requestId) { message ->
                    (message as? Published)?.let {
                        onPublished(it.publication)
                    }
                }
            }
        }
    }
}