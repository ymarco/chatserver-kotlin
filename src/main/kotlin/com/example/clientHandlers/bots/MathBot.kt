package com.example.clientHandlers.bots

val MathBot = newBot("MathBot") {
    addMethod(Bot.Method("list", listOf("n"), "Print n, n-1, ..., 1") { _, args ->
        val n = args[0].toInt()
        if (n != 0) {
            broadcastMsg("$botPrefix$name list ${n - 1}")
        }
    })

    addMethod(Bot.Method("factorial", listOf("n"), "Calculate n!") { _, args ->
        val n = args[0].toInt()
        broadcastMsg("$botPrefix$name _factorial $n 1")
    })
    addMethod(
        Bot.Method(
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
