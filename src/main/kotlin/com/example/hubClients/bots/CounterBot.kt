package com.example.hubClients.bots

import com.example.server.ChatServer
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CounterBot(server: ChatServer) : Bot("CounterBot", server) {

    private val sum = MutableStateFlow(0)

    init {
        addMethod(Method("add", listOf("n"), "add N to current counter") { caller, args ->
            val diff = args.firstOrNull()?.toIntOrNull()
            if (diff == null) {
                sendPrivateMsgIfUserIsOnline(caller, "N must be an integer")
                return@Method
            }
            sum.update { sum -> sum + diff }
        })
    }

    @OptIn(FlowPreview::class)
    override suspend fun run() = coroutineScope {
        launch {
            incomingCmds.collect { it.callOrRelayError() }
        }
        launch {
            sum
                .debounce(timeoutMillis = 2000)
                .collect {
                    broadcastMsg("sum is $it")
                }
        }
        Unit
    }

    companion object : BotConstructor {
        override fun new(server: ChatServer) = CounterBot(server)
    }
}