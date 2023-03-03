package com.example.clientHandlers.bots

import com.example.server.ChatServer

class CounterBot(server: ChatServer) : Bot("CounterBot", server) {
    private val counter = 0
    init {
        addMethod(Method("add", listOf("n"), "add N to current counter") { caller, args ->
            counter+=args[0].toIntOrNull() ?:return@Method server.hub
        })
    }

    companion object : BotConstructor {
        override fun new(server: ChatServer) = CounterBot(server)
    }
}