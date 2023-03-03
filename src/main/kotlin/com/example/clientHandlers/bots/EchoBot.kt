package com.example.clientHandlers.bots

import com.example.server.ChatServer

class EchoBot(server: ChatServer) : Bot("EchoBot", server) {
    init {
        addMethod(
            Method("echo", listOf(), "Like echo(1)") { caller, args ->
                broadcastMsg("$caller said ${args.joinToString(" ")}")
            }
        )
    }

    companion object : BotConstructor{
        override fun new(server: ChatServer) = EchoBot(server)
    }
}