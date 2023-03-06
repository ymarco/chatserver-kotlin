package com.example.server

import com.example.hubClients.bots.Bot
import com.example.hubClients.websocketClients.AuthenticatedClient
import io.ktor.util.collections.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

typealias Username = String
typealias Password = String

class Hub {
    val activeConnections: MutableMap<Username, HubClient> = ConcurrentMap()

    fun add(client: HubClient) = activeConnections.set(client.name, client)

    suspend fun runBots() = supervisorScope {
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

