package com.example.server

import com.example.hubClients.websocketClients.AuthenticatedClient
import com.example.hubClients.bots.Bot
import io.ktor.util.collections.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

typealias Username = String
typealias Password = String

data class ChatMessage(val sender: Username, val content: String)

interface HubClient {
    val incomingMsgs: MutableSharedFlow<ChatMessage>
    val name: Username
    val server: ChatServer
    suspend fun broadcastMsg(content: String) =
        server.hub.broadcastMsg(ChatMessage(sender = this.name, content = content))
}

class Hub {
    val activeConnections: MutableMap<Username, HubClient> = ConcurrentMap()

    fun add(client: HubClient) = activeConnections.set(client.name, client)

    suspend fun runBots() = coroutineScope {
        activeConnections
            .values
            .filterIsInstance<Bot>()
            .forEach { bot ->
                launch { bot.run() }
            }
    }

    fun userIsOnline(username: Username) = username in activeConnections


    fun AuthenticatedClient.logOut() {
        call.application.environment.log.info("Logged out: $username")
        activeConnections.remove(username)
    }

    suspend fun broadcastMsg(msg: ChatMessage) =
        activeConnections.forEach { (_, client) -> client.incomingMsgs.emit(msg) }

}

