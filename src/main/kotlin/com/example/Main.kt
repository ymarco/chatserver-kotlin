package com.example

import com.example.hubClients.bots.CounterBot
import com.example.hubClients.bots.EchoBot
import com.example.hubClients.bots.MathBot
import com.example.server.ChatServer


suspend fun main() {
    ChatServer(8080, "0.0.0.0") {
        addBot(EchoBot)
        addBot(MathBot)
        addBot(CounterBot)
    }.run()
}
