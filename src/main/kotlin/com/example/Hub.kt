package com.example

import com.example.plugins.AuthRequest
import io.ktor.network.sockets.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BroadcastChannel
import java.util.Collections.synchronizedMap
import java.util.concurrent.atomic.*

typealias Username = String
typealias Password = String
data class ChatMessage(val sender: Username, val content: String)
data class Connection(val session: DefaultWebSocketServerSession, val username: Username)

object Hub {
    private val connections: MutableMap<Username, Connection> = synchronizedMap(HashMap())
    private val userDB: MutableMap<Username, Password> = synchronizedMap(HashMap())

    @OptIn(ObsoleteCoroutinesApi::class)
    val broadcastMsg = BroadcastChannel<ChatMessage>(128)

    sealed interface AuthRequestStatus {
        class Passed(val username: Username, val password: Password) : AuthRequestStatus
        sealed class Failed(val reason: String) : AuthRequestStatus {
            object InvalidCredentials : Failed("Invalid credentials")
            object UsernameAlreadyOnline : Failed("Username already online")
            object UsernameAlreadyExists : Failed("Username already exists")
        }
    }

    fun checkAuthRequest(request: AuthRequest): AuthRequestStatus {
        when (request) {
            is AuthRequest.Login -> {
                if (userDB[request.username] != request.password) {
                    return AuthRequestStatus.Failed.InvalidCredentials
                }
                if (request.username in connections) {
                    return AuthRequestStatus.Failed.UsernameAlreadyOnline
                }
            }

            is AuthRequest.Register -> {
                if (request.username in userDB) {
                    return AuthRequestStatus.Failed.UsernameAlreadyExists
                }
            }
        }
        return AuthRequestStatus.Passed(request.username, request.password)
    }

    fun DefaultWebSocketServerSession.logIn(request: AuthRequestStatus.Passed): Connection  {
        val thisConnection = Connection(this, request.username)
        call.application.environment.log.info("Logged in: ${request.username}")
        connections[request.username] = thisConnection
        userDB[request.username] = request.password
        return thisConnection
    }
    fun Connection.logOut(username: String) {
        session.call.application.environment.log.info("Logged out: $username")
        connections.remove(username)
    }
}

