package com.example.hubClients.websocketClients

import com.example.hubClients.websocketClients.AuthAction.Login
import com.example.hubClients.websocketClients.AuthAction.Register
import com.example.retryUntilNotNull
import com.example.sendln
import com.example.server.ChatServer
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ReceiveChannel


class UnauthenticatedClient(session: WebSocketServerSession, val server: ChatServer) :
    WebSocketServerSession by session {

    private suspend fun authenticate(): AuthenticatedClient =
        retryUntilNotNull {
            val request = acceptAuthRequest()

            with(server) {
                when (val status = check(request)) {
                    is AuthRequestStatus.Failed -> {
                        sendln(status.reason)
                        null
                    }

                    is AuthRequestStatus.Passed -> {
                        sendln("Logged in as ${status.creds.username}")
                        logIn(status)
                    }

                }
            }
        }

    private suspend fun acceptAuthRequest(): AuthRequest {
        val action = chooseLoginOrRegister()
        val credentials = acceptUsernameAndPassword()
        return AuthRequest.new(action, credentials)
    }

    private suspend fun ReceiveChannel<Frame>.receiveText(): String? =
        (receive() as? Frame.Text)?.readText()?.trim()

    private suspend inline fun promptForString(
        prompt: String,
        evaluator: ((String) -> Boolean) = { true }
    ): String? {
        sendln(prompt)
        val res = incoming.receiveText() ?: return null
        return if (evaluator(res)) res else null
    }

    private suspend fun chooseLoginOrRegister(): AuthAction = retryUntilNotNull {
        when (promptForString("Type ${Login.str} to login, ${Register.str} to register:")) {
            Login.str -> Login
            Register.str -> Register
            else -> null
        }
    }


    private suspend fun acceptUsernameAndPassword(): Credentials {
        val username = retryUntilNotNull { promptForString("Username:") { it != "" } }
        val password = retryUntilNotNull { promptForString("Password:") { it != "" } }
        return Credentials(username, password)
    }

    suspend fun runSession() {
        val client = authenticate()
        try {
            client.run()
        } finally {
            with(server) { client.logOut() }
        }
    }

}
