package com.raumfeld.wamplibrary

import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

interface Client {
    fun register(procedure: Uri, handler: CallHandler): RegistrationHandle

    fun call(
            procedure: Uri,
            arguments: List<Any?>? = null,
            argumentsKw: Dict? = null
    ): DeferredCallResult

    fun disconnect(closeReason: Uri = WampClose.SYSTEM_SHUTDOWN.uri): Uri

    fun publish(
            topic: Uri,
            arguments: List<Any?>? = null,
            argumentsKw: Dict? = null,
            onPublished: ((Long) -> Unit)? = null
    )

    fun subscribe(topicPattern: UriPattern, eventHandler: EventHandler): SubscriptionHandle
}

class ClientImpl(
        val coroutineScope: CoroutineScope,
        incoming: ReceiveChannel<String>,
        outgoing: SendChannel<String>,
        realm: Uri
) : Client {
    private val log = Logger()

    private val connection = Connection(incoming, outgoing)

    private var sessionId: Long? = null

    private val randomIdGenerator = RandomIdGenerator()

    private val messageListenersHandler = MessageListenersHandler()

    private val caller = Caller(connection, randomIdGenerator, messageListenersHandler)
    private val callee = Callee(connection, randomIdGenerator, messageListenersHandler)

    private val publisher = Publisher(connection, randomIdGenerator, messageListenersHandler)
    private val subscriber = Subscriber(connection, randomIdGenerator, messageListenersHandler)

    init {

        joinRealm(realm)

        coroutineScope.launch {
            connection.forEachMessage(exceptionHandler()) {
                try {
                    handleMessage(it)
                } catch (nonFatalError: WampErrorException) {
                    exceptionCatcher.catchException(nonFatalError)
                }
            }.invokeOnCompletion { fatalException ->
                fatalException?.run { printStackTrace() }
            }
        }
    }

    private fun exceptionHandler(): (Throwable) -> Unit = { throwable ->
        when (throwable) {
            is WampErrorException -> exceptionCatcher.catchException(throwable)
            else -> throw throwable
        }
    }

    private fun handleMessage(message: Message) {
        messageListenersHandler.notifyListeners(message)

        when (message) {
            is Invocation -> callee.invokeProcedure(message)
            is Event -> subscriber.receiveEvent(message)

            is Error -> exceptionCatcher.catchException(message.toWampErrorException())
        }
    }

    private fun joinRealm(realmUri: Uri) = coroutineScope.launch {
        connection.send(
                Hello(
                        realmUri, mapOf(
                        "roles" to mapOf<String, Any?>(
                                "publisher" to emptyMap<String, Any?>(),
                                "subscriber" to emptyMap<String, Any?>(),
                                "caller" to emptyMap<String, Any?>(),
                                "callee" to emptyMap<String, Any?>()
                        )
                )
                )
        )
        connection.withNextMessage { message: Welcome ->
            log.i("Session established. ID: ${message.session}")
            sessionId = message.session
        }.join()
    }

    override fun register(procedure: Uri, handler: CallHandler) =
            callee.register(procedure, handler)

    override fun call(
            procedure: Uri,
            arguments: List<Any?>?,
            argumentsKw: Dict?
    ) = caller.call(procedure, arguments, argumentsKw)

    override fun disconnect(closeReason: Uri) = coroutineScope.launch {
        val messageListener = messageListenersHandler.registerListener<Goodbye>()

        connection.send(Goodbye(emptyMap(), closeReason))

        messageListener.await().let { message ->
            Log.i("Router replied goodbye. Reason: ${message.reason}")
            message.reason
        }
    }

    override fun publish(
            topic: Uri,
            arguments: List<Any?>?,
            argumentsKw: Dict?,
            onPublished: ((Long) -> Unit)?
    ) = publisher.publish(topic, arguments, argumentsKw, onPublished)

    override fun subscribe(
            topicPattern: UriPattern,
            eventHandler: EventHandler
    ) = subscriber.subscribe(topicPattern, eventHandler)
}