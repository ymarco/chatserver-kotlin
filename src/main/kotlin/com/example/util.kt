package com.example

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.yield

fun String.splitSpaces() = split(" ", "\t", "\n", "\r")

suspend inline fun <T> retryUntilNotNull(fn: () -> (T?)): T {
    // doesn't use recursion to stay inline-able
    while (true) {
        yield()
        return fn() ?: continue
    }
}

suspend fun WebSocketServerSession.sendln(str: String) =
    send(str + System.lineSeparator())
