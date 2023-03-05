package com.example.hubClients.websocketClients

import com.example.server.ChatMessage
import io.ktor.server.plugins.*


sealed class Command(val str: String) {
    abstract suspend fun execute(client: AuthenticatedClient, args: List<String>): Result<String>

    companion object {
        object Whisper : Command("whisper") {
            override suspend fun execute(client: AuthenticatedClient, args: List<String>): Result<String> {
                if (args.count() < 2) {
                    return Result.failure(IllegalArgumentException("Usage: whisper USER MSG...\n"))
                }
                val recipient = args[0]
                val msgContent = args.subList(1, args.count()).joinToString(separator = " ")
                val recipientClient = client.server.hub.activeConnections[recipient]
                val msg = ChatMessage(sender = client.username, content = msgContent)
                return if (recipientClient != null) {
                    recipientClient.incomingMsgs.emit(msg)
                    Result.success("Msg send successfully")
                } else Result.failure(NotFoundException("User isn't online"))
            }
        }

        fun fromStringWithoutCmdPrefix(string: String): Command? = when (string) {
            Whisper.str -> Whisper
            else -> null
        }
    }

}