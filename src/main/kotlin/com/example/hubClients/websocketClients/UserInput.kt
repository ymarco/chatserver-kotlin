package com.example.hubClients.websocketClients

import com.example.sendln
import com.example.server.ChatMessage
import com.example.splitSpaces

sealed class UserInput(val owner: AuthenticatedClient) {
    abstract suspend fun execute()
    class Msg(private val str: String, owner: AuthenticatedClient) : UserInput(owner) {
        override suspend fun execute() {
            owner.broadcastMsg(str)
        }
    }

    sealed class Cmd(
        val args: List<String>,
        owner: AuthenticatedClient
    ) : UserInput(owner) {

        class Whisper(args: List<String>, owner: AuthenticatedClient) :
            Cmd(args, owner) {
            companion object {
                const val name = "whisper"
            }

            override suspend fun execute() {
                if (args.count() < 2) {
                    return owner.sendln("Usage: whisper USER MSG...\n")
                }
                val recipient = args.first()
                val msgContent = args.drop(1).joinToString(separator = " ")
                val recipientClient = owner.server.hub.activeConnections[recipient]
                val msg = ChatMessage(sender = owner.username, content = msgContent)
                if (recipientClient != null) {
                    recipientClient.incomingMsgs.emit(msg)
                } else {
                    owner.sendln("User isn't online")
                }
            }
        }

    }

    class UnknownCmd(private val name: String, owner: AuthenticatedClient) :
        UserInput(owner) {
        override suspend fun execute() {
            owner.sendln("Invalid command: $name")

        }
    }
}

class InputFactory(
    private val cmdPrefix: String,
    private val owner: AuthenticatedClient
) {
    private fun isCommand(str: String) = str.startsWith(cmdPrefix)

    private fun splitToCmdAndArgs(str: String): Pair<String, List<String>> =
        with(str.splitSpaces()) {
            val cmd = first().drop(cmdPrefix.length)
            val args = drop(1)
            Pair(cmd, args)
        }

    fun new(str: String): UserInput {
        return if (isCommand(str)) {
            newCmd(str, owner)
        } else {
            UserInput.Msg(str, owner)
        }
    }

    private fun newCmd(str: String, owner: AuthenticatedClient): UserInput {
        val (cmdStr, args) = splitToCmdAndArgs(str)
        return when (cmdStr) {
            UserInput.Cmd.Whisper.name -> UserInput.Cmd.Whisper(args, owner)
            else -> UserInput.UnknownCmd(cmdStr, owner)
        }
    }
}
