package com.example

import com.example.hubClients.bots.*
import com.example.server.ChatServer


suspend fun main() {
    ChatServer(8080, "0.0.0.0") {
//        a(this, "echo")
//        {
//            addMethod(
//                Bot.Method("echo", listOf(), "Like echo(1)") { caller, args ->
//                    broadcastMsg("$caller said ${args.joinToString(" ")}")
//                }
//            )
//        }
        addBot(EchoBot)
        addBot(MathBot)
        addBot(CounterBot)
    }.run()
}

//fun a(server: ChatServer, name: String, initFn: Bot.() -> Unit): Bot {
//    server.hub.add(
//        newBot(name) {
//            initFn()
//        }.new(server)
//    )
//}