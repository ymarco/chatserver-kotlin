package com.example.clientHandlers.websocketClients

import com.example.sendln
import com.example.splitSpaces

sealed class UserInput(open val owner: AuthenticatedClient) {
    abstract suspend fun execute()
    class Msg(private val str: String, override val owner: AuthenticatedClient) :
        UserInput(owner) {
        override suspend fun execute() {
            owner.broadcastMsg(str)
        }
    }

    class Cmd(
        private val cmd: Command,
        private val args: List<String>,
        override val owner: AuthenticatedClient
    ) :
        UserInput(owner) {
        override suspend fun execute() {
            cmd.execute(owner, args).onFailure { owner.sendln("$it") }
        }
    }

    class UnknownCmd(private val name: String, override val owner: AuthenticatedClient) :
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
    private fun splitToCmdAndArgs(str: String): Pair<String, List<String>> {
        val cmdAndArgs = str.splitSpaces()
        val cmd = cmdAndArgs[0].substring(cmdPrefix.length)
        val args = cmdAndArgs.subList(1, cmdAndArgs.count())
        return Pair(cmd, args)
    }

    fun new(str: String): UserInput =
        if (isCommand(str)) {
            val (cmdStr, args) = splitToCmdAndArgs(str)
            val cmd = Command.fromStringWithoutCmdPrefix(cmdStr)
            if (cmd != null) {
                UserInput.Cmd(cmd, args, owner)
            } else
                UserInput.UnknownCmd(str, owner)
        } else UserInput.Msg(str, owner)
}
