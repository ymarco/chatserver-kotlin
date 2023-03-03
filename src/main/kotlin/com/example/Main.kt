package com.example

import com.example.clientHandlers.bots.EchoBot
import com.example.clientHandlers.bots.MathBot
import com.example.server.ChatServer


fun main() =
    ChatServer(8080, "0.0.0.0") {
        addBot(EchoBot)
        addBot(MathBot)
    }.run()
