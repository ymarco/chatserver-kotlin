package com.example.plugins

import com.example.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.util.*
import javax.jws.soap.SOAPBinding.Use

var id = 0

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocket("/chat") {
            println("Got a connection!")
            val thisConnection = authenticate()
            try {
                launch {
                    thisConnection.executeIncomingUserInput()
                }
                launch {
                    thisConnection.printIncomingChatMsgs()
                }
                Channel<Int>().receive()
            } finally {
                Hub.logOut(thisConnection.username)
            }
        }
    }
}

enum class AuthAction(val str: String) {
    Login("l"),
    Register("r");
}

sealed class AuthRequest(val username: Username, val password: Password) {
    class Login(username: Username, password: Password) : AuthRequest(username, password)
    class Register(username: Username, password: Password) : AuthRequest(username, password)
}

private suspend fun DefaultWebSocketServerSession.authenticate(): Connection =
    retry {
        val request = acceptAuthRequest()
        with(Hub) {
            when (val status = checkAuthRequest(request)) {
                is Hub.AuthRequestStatus.Failed -> {
                    send(status.reason + System.lineSeparator())
                    null
                }
                is Hub.AuthRequestStatus.Passed -> {
                    send("Logged in as ${status.username}\n")
                    logIn(status)
                }
            }
        }

    }

private suspend fun DefaultWebSocketServerSession.acceptAuthRequest(): AuthRequest {
    val action = chooseLoginOrRegister()
    val credentials = acceptUsernameAndPassword()
    return when (action) {
        AuthAction.Login -> AuthRequest.Login(credentials.username, credentials.password)
        AuthAction.Register -> AuthRequest.Register(credentials.username, credentials.password)
    }
}

private suspend fun ReceiveChannel<Frame>.receiveText(): String? =
    (receive() as? Frame.Text)?.readText()?.trim()

private suspend inline fun DefaultWebSocketServerSession.promptForString(
    prompt: String,
    evaluator: ((String) -> Boolean) = { true }
): String? {
    send(prompt + System.lineSeparator())
    val res = incoming.receiveText() ?: return null
    return if (evaluator(res)) res else null
}

suspend inline fun <T> retry(fn: () -> (T?)): T {
    while (true) {
        yield()
        return fn() ?: continue
    }
}

private suspend fun DefaultWebSocketServerSession.chooseLoginOrRegister(): AuthAction = retry {
    when (promptForString("Type ${AuthAction.Login.str} to login, ${AuthAction.Register.str} to register:")) {
        AuthAction.Login.str -> AuthAction.Login
        AuthAction.Register.str -> AuthAction.Register
        else -> null
    }
}

data class Credentials(val username: String, val password: String)

private suspend fun DefaultWebSocketServerSession.acceptUsernameAndPassword(): Credentials {
    val username = retry { promptForString("Username:") { it != "" } }
    val password = retry { promptForString("Password:\n") { it != "" } }
    return Credentials(username, password)
}

@OptIn(ObsoleteCoroutinesApi::class)
private suspend fun Connection.executeIncomingUserInput() {
    for (frame in session.incoming) {
        val receivedText = (frame as? Frame.Text ?: continue).readText().trim()
        Hub.broadcastMsg.send(ChatMessage(sender = username, content = receivedText))
    }
}

@OptIn(ObsoleteCoroutinesApi::class)
private suspend fun Connection.printIncomingChatMsgs() {
    val msgs = Hub.broadcastMsg.openSubscription()
    try {
        for (msg in msgs) {
            if (msg.sender != this.username) {
                session.send("${msg.sender}: ${msg.content}")
            }
        }
    } finally {
        msgs.cancel()
    }

}
