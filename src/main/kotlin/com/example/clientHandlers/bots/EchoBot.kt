package com.example.clientHandlers.bots

val EchoBot = newBot("EchoBot") {
    addMethod(
        Bot.Method("echo", listOf(), "Like echo(1)") { caller, args ->
            broadcastMsg("$caller said ${args.joinToString(" ")}")
        }
    )

}