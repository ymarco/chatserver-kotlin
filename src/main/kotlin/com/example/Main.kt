package com.example

import com.example.hubClients.bots.CounterBot
import com.example.hubClients.bots.EchoBot
import com.example.hubClients.bots.MathBot
import com.example.server.ChatServer
import io.ktor.server.engine.*
import kotlinx.coroutines.runBlocking


fun main() {
    return runBlocking {
        ChatServer(8080, "0.0.0.0") {
            addBot(EchoBot)
            addBot(MathBot)
            addBot(CounterBot)
        }.run()
    }
}
