package com.example.server

import kotlinx.coroutines.flow.MutableSharedFlow

interface HubClient {
    val incomingMsgs: MutableSharedFlow<ChatMessage>
    val name: Username
    val server: ChatServer
    suspend fun broadcastMsg(content: String) =
        server.hub.broadcastMsg(ChatMessage(sender = this.name, content = content))
}