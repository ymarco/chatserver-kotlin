package com.example.server

import com.example.clientHandlers.bots.Bot
import com.example.clientHandlers.bots.BotConstructor
import com.example.clientHandlers.bots.newBot
import com.example.clientHandlers.websocketClients.AuthRequest
import com.example.clientHandlers.websocketClients.AuthRequest.Login
import com.example.clientHandlers.websocketClients.AuthRequest.Register
import com.example.clientHandlers.websocketClients.AuthRequestStatus
import com.example.clientHandlers.websocketClients.AuthRequestStatus.Failed
import com.example.clientHandlers.websocketClients.AuthRequestStatus.Passed
import com.example.clientHandlers.websocketClients.AuthenticatedClient
import com.example.clientHandlers.websocketClients.UnauthenticatedClient
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.runBlocking
import java.time.Duration
import kotlin.concurrent.thread

class ChatServer(
    port: Int,
    host: String,
    val hub: Hub = Hub(),
    private val db: DB = DB(),
    val cmdPrefix: String = "/",
    configFn: ChatServer.() -> Unit,
) {
    private val ktorServer: ApplicationEngine

    init {
        ktorServer = embeddedServer(
            Netty,
            port = port,
            host = host,
        ) {
            install(WebSockets) {
                pingPeriod = Duration.ofSeconds(15)
                timeout = Duration.ofSeconds(15)
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }

            routing {
                webSocket(path = "/chat") {
                    UnauthenticatedClient(this, this@ChatServer).runSession()
                }
            }

        }
        configFn()
    }

    fun checkAuthRequest(request: AuthRequest): AuthRequestStatus =
        when (request) {
            is Login -> checkLoginRequest(request)
            is Register -> checkRegisterRequest(request)
        }

    private fun checkRegisterRequest(request: Register) =
        if (db.match(request.creds))
            Failed.UsernameAlreadyExists
        else
            Passed.Register(request.creds)


    private fun checkLoginRequest(request: Login) =
        if (!db.match(request.creds))
            Failed.InvalidCredentials
        else if (hub.userIsOnline(request.creds.username))
            Failed.UsernameAlreadyOnline
        else
            Passed.Login(request.creds)


    fun UnauthenticatedClient.logIn(request: Passed): AuthenticatedClient {
        with(request) {
            val client = AuthenticatedClient(this@logIn, creds.username)
            call.application.environment.log.info("Logged in: ${creds.username}")
            hub.add(client)
            if (request is Passed.Register) {
                db.add(creds)
            }
            return client
        }
    }


    fun addBot(botConstructor: BotConstructor) = hub.addBot(botConstructor.new(this))


    fun addNewBot(name: String, initFn: Bot.() -> Unit) =
        addBot(newBot(name, initFn))

    fun run() {
        thread(start = true, isDaemon = true, name = "bots") {
            runBlocking { hub.runBots() }
        }
        ktorServer.start(wait = true)
    }
}
