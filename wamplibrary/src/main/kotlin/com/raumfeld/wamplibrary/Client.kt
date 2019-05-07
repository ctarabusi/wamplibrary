package com.raumfeld.wamplibrary

import com.raumfeld.wamplibrary.pubsub.EventHandler
import com.raumfeld.wamplibrary.pubsub.Publisher
import com.raumfeld.wamplibrary.pubsub.Subscriber
import com.raumfeld.wamplibrary.pubsub.SubscriptionHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

interface Client {
//    suspend fun register(procedure: Uri, handler: CallHandler): RegistrationHandle
//
//    suspend fun call(
//            procedure: Uri,
//            arguments: List<Any?>? = null,
//            argumentsKw: WampDict? = null
//    ): DeferredCallResult

    suspend fun disconnect(closeReason: String = WampClose.SYSTEM_SHUTDOWN.uri): Job

    suspend fun publish(
            topic: String,
            arguments: List<JsonElement>,
            argumentsKw: WampDict,
            onPublished: (suspend (Long) -> Unit)? = null
    )

    suspend fun subscribe(topicPattern: TopicPattern, eventHandler: EventHandler): SubscriptionHandle
}

class ClientImpl(
        val coroutineScope: CoroutineScope,
        incoming: ReceiveChannel<String>,
        outgoing: SendChannel<String>,
        realm: String
) : Client {
    private val log = Logger()

    private val connection = Connection(coroutineScope, incoming, outgoing)

    private var sessionId: Long? = null

    private val randomIdGenerator = RandomIdGenerator()

    private val messageListenersHandler = MessageListenersHandler(coroutineScope)

//    private val caller = Caller(connection, randomIdGenerator, messageListenersHandler)
//    private val callee = Callee(connection, randomIdGenerator, messageListenersHandler)

    private val publisher = Publisher(connection, randomIdGenerator, messageListenersHandler)
    private val subscriber = Subscriber(connection, randomIdGenerator, messageListenersHandler)

    init {

        joinRealm(realm)

        coroutineScope.launch {
            connection.forEachMessage(exceptionHandler()) {
             //   try {
                    handleMessage(it)
//                } catch (nonFatalError: WampErrorException) {
//                    exceptionCatcher.catchException(nonFatalError)
//                }
            }.invokeOnCompletion { fatalException ->
                fatalException?.run { printStackTrace() }
            }
        }
    }

    private fun exceptionHandler(): (Throwable) -> Unit = { throwable ->
        when (throwable) {
          //  is WampErrorException -> exceptionCatcher.catchException(throwable)
            else -> throw throwable
        }
    }

    private fun handleMessage(message: Message) {
        messageListenersHandler.notifyListeners(message)

        when (message) {
       //     is Invocation -> callee.invokeProcedure(message)
            is Welcome -> {
                log.i("Session established. ID: ${message.session}")
                sessionId = message.session
            }
            is Event -> subscriber.receiveEvent(message)

         //   is Error -> exceptionCatcher.catchException(message.toWampErrorException())
        }
    }

    private fun joinRealm(realmUri: String) = coroutineScope.launch {
        connection.send(
                Hello(
                        realmUri, mapOf(
                        "roles" to JsonObject(mapOf<String, JsonElement>(
                                "publisher" to JsonObject(emptyMap()),
                                "subscriber" to JsonObject(emptyMap()),
                                "caller" to JsonObject(emptyMap()),
                                "callee" to JsonObject(emptyMap())
                        ))
                )
                )
        )
    }

//    override suspend fun register(procedure: Uri, handler: CallHandler) = callee.register(procedure, handler)

//    override suspend fun call(
//            procedure: Uri,
//            arguments: List<Any?>?,
//            argumentsKw: WampDict?
//    ) = caller.call(procedure, arguments, argumentsKw)

    override suspend fun disconnect(closeReason: String) = coroutineScope.launch {
        val messageListener = messageListenersHandler.registerListener<Goodbye>(randomIdGenerator.newRandomId())

        connection.send(Goodbye(emptyMap(), closeReason))

        messageListener.await().let { message ->
            log.i("Router replied goodbye. Reason: ${message.reason}")
            message.reason
        }
    }

    override suspend fun publish(
            topic: String,
            arguments: List<JsonElement>,
            argumentsKw: WampDict,
            onPublished: (suspend (Long) -> Unit)?
    ) = publisher.publish(topic, arguments, argumentsKw, onPublished)

    override suspend  fun subscribe(
            topicPattern: TopicPattern,
            eventHandler: EventHandler
    ) = subscriber.subscribe(topicPattern, eventHandler)
}