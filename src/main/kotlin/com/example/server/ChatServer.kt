package com.example.server

import com.example.hubClients.bots.Bot
import com.example.hubClients.bots.BotConstructor
import com.example.hubClients.bots.newBot
import com.example.hubClients.websocketClients.AuthRequestStatus.Passed
import com.example.hubClients.websocketClients.AuthenticatedClient
import com.example.hubClients.websocketClients.UnauthenticatedClient
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.Duration

/**
 * Every once in a while Kotlin gets confused and says we aren't covering all
 * the cases in a when() expression even though we are. Keep this in case it
 * happens again.
 */
data class UnreachableCodeException(val str: String? = null) : Exception(str)

class ChatServer(
    port: Int,
    host: String,
    val hub: Hub = Hub(),
    val db: DB = DB(),
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
                    UnauthenticatedClient(this, this@ChatServer)
                        .runSession()
                }
            }

        }
        configFn()
    }

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
    fun AuthenticatedClient.logOut() {
        with(hub){
            logOut()
        }
    }


    fun addBot(botConstructor: BotConstructor) = hub.add(botConstructor.new(this))

    fun addNewBot(name: String, initFn: Bot.() -> Unit) =
        addBot(newBot(name, initFn))

    suspend fun run() = coroutineScope {
        launch { hub.runBots() }
        launch(Dispatchers.Default) { ktorServer.start(wait = true) }
    }
}
