package com.example.hubClients.websocketClients

import com.example.sendln
import com.example.server.ChatMessage
import com.example.server.ChatServer
import com.example.server.HubClient
import com.example.server.Username
import com.example.splitSpaces
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


class AuthenticatedClient(
    unauthenticatedClient: UnauthenticatedClient,
    val username: Username
) : WebSocketServerSession by unauthenticatedClient, HubClient {

    override val name: Username get() = username
    override val server: ChatServer = unauthenticatedClient.server


    override val incomingMsgs = MutableSharedFlow<ChatMessage>()

    private suspend fun executeIncomingUserInput() {
        // TODO is this the proper place to create the factory?
        val inputFactory = InputFactory(server.cmdPrefix, this)
        incoming.consumeAsFlow()
            .filterIsInstance<Frame.Text>()
            .map(Frame.Text::readText)
            .map(inputFactory::new)
            .collect(UserInput::execute)
    }

    private suspend fun printIncomingChatMsgs() {
        incomingMsgs
            .filter { it.sender != this.username }
            .collect(::relayMsgToUser)
    }

    private suspend fun relayMsgToUser(msg: ChatMessage) {
        sendln("${msg.sender}: ${msg.content}")
    }

    suspend fun run() = coroutineScope {
        launch {
            executeIncomingUserInput()
        }
        launch {
            printIncomingChatMsgs()
        }
    }

}