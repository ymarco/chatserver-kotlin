package com.example.server

import com.example.clientHandlers.websocketClients.Credentials
import io.ktor.util.collections.*

class DB {
    private val users: MutableMap<Username, Password> = ConcurrentMap()
    fun add(creds: Credentials) = users.put(creds.username, creds.password)
    fun match(creds: Credentials) = users[creds.username] == creds.password
}