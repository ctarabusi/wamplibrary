package com.raumfeld.wamplibrary

interface WebSocketDelegate {
    fun send(message: String)

    fun close()
}