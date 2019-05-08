package com.raumfeld.wamplibrary

interface WebSocketCallback {
    fun onOpen(webSocketDelegate: WebSocketDelegate)

    fun onMessage(messageJson: String)

    fun onClosing()
}