package com.example.hubClients.bots

import com.example.server.ChatServer

class MathBot(server: ChatServer) : Bot("MathBot", server) {
    init {
        addMethod(
            Method(
                name = "range",
                argsDescription = listOf("n"),
                description = "Print n, n-1, ..., 1"
            ) { caller, args ->
                val n = args.firstOrNull()?.toIntOrNull() ?: return@Method
                if (n != 0) {
                    broadcastMsg("$botPrefix$name list ${n - 1}")
                }
            })

        addMethod(
            Method("factorial", listOf("n"), "Calculate n!") { caller, args ->
                val n = args.firstOrNull()?.toIntOrNull()
                if (n == null) {
                    sendPrivateMsgIfUserIsOnline(caller, "Usage: factorial N")
                    return@Method
                }
                broadcastMsg("$botPrefix$name _factorial $n 1")
            })
        addMethod(
            Method(
                "_factorial", listOf("n", "acc"), "Internal function of factorial"
            ) { _, args ->
                val (n, acc) = args.map(String::toInt)
                if (n == 0) {
                    broadcastMsg("Result is $acc")
                } else {
                    broadcastMsg("$botPrefix$name _factorial ${n - 1} ${acc * n}")
                }
            })
    }

    companion object : BotConstructor {
        override fun new(server: ChatServer) = MathBot(server)
    }
}
