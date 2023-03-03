package com.example.clientHandlers.bots

import com.example.server.ChatServer

class MathBot(server: ChatServer) : Bot("MathBot", server) {
    init {
        addMethod(Method("list", listOf("n"), "Print n, n-1, ..., 1") { _, args ->
            val n = args[0].toInt()
            if (n != 0) {
                broadcastMsg("$botPrefix$name list ${n - 1}")
            }
        })

        addMethod(Method("factorial", listOf("n"), "Calculate n!") { _, args ->
            val n = args[0].toInt()
            broadcastMsg("$botPrefix$name _factorial $n 1")
        })
        addMethod(
            Method(
                "_factorial",
                listOf("n", "acc"),
                "Internal function of factorial"
            ) { _, args ->
                val (n, acc) = args.map(String::toInt)
                if (n == 0) {
                    broadcastMsg("Result is $acc")
                } else {
                    broadcastMsg("$botPrefix$name _factorial ${n - 1} ${acc * n}")
                }
            })
    }
    companion object: BotConstructor {
        override fun new(server: ChatServer) = MathBot(server)
    }
}