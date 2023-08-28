package com.example.hubClients.bots

import com.example.server.ChatMessage
import com.example.server.ChatServer
import com.example.server.HubClient
import com.example.server.Username
import com.example.splitSpaces
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

const val botPrefix = "!"
fun isBotCommand(string: String): Boolean = string.startsWith(botPrefix)

typealias BotName = String

typealias MethodName = String

interface BotConstructor {
    fun new(server: ChatServer): Bot
}

abstract class Bot(override val name: BotName, override val server: ChatServer) :
    HubClient {

    data class Method(
        val name: String,
        val argsDescription: List<String>,
        val description: String,
        val call: suspend Bot.(caller: Username, args: List<String>) -> Unit
    ) {
        fun describe(): String {
            val describeArgs =
                argsDescription.joinToString(" ", transform = String::uppercase)
            return """
                   $name $describeArgs
                       $description
                    """
                .trimIndent()

        }
    }

    data class MethodCallAttempt(
        val caller: Username,
        val callee: MethodName,
        val args: List<String>
    )
    // TODO
    data class MethodEnv(
        val bot: Bot,
        val method: Method,
        val callAttempt: MethodCallAttempt
    ) {
        suspend fun printUsage() =
            bot.sendPrivateMsgIfUserIsOnline(callAttempt.caller,
                with(method){"Usage: $name ${argsDescription.joinToString(" ")}"})

    }

    final override val incomingMsgs = MutableSharedFlow<ChatMessage>()

    val incomingCmds: Flow<MethodCallAttempt>

    private val methods = LinkedHashMap<String, Method>()

    init {
        addHelpMethod()
        incomingCmds = incomingMsgs
            .filter { isBotCommand(it.content) }
            .filter { isDirectedAtUs(it.content) }
            .map(::parseToMethodCallAttempt)
            // without this flowOn, bots get deadlocked upon sending messages since the
            // SharedFlow buffer is 0 and the coroutine they try to receive on is the same
            // one they're emitting on
            .flowOn(Dispatchers.Default)
    }

    private fun parseToMethodCallAttempt(it: ChatMessage): MethodCallAttempt {
        val split = it.content.splitSpaces()
        val methodName = split[1]
        val args = split.subList(2, split.count())
        return MethodCallAttempt(it.sender, methodName, args)
    }

    private fun isDirectedAtUs(msgContent: String): Boolean {
        val withoutPrefix = msgContent.substring(botPrefix.length)
        val split = withoutPrefix.splitSpaces()
        val botName = split[0]
        return botName == this.name
    }

    private fun addHelpMethod() {
        addMethod(Method("help", listOf(), "Print this help string") { _, _ ->
            sendMsg(
                "My commands are\n" +
                        methods
                            .values
                            .joinToString("\n", transform = Method::describe)
            )
        })
    }


    suspend operator fun Method.invoke(username: Username, args: List<String>) =
        call(username, args)

    private suspend fun sendMsg(string: String) =
        server.hub.broadcastMsg(ChatMessage(name, string))


    fun addMethod(method: Method) = methods.set(method.name, method)

    internal suspend fun sendPrivateMsgIfUserIsOnline(
        recipient: Username,
        content: String
    ) = server.hub.activeConnections[recipient]?.incomingMsgs?.emit(
        ChatMessage(this.name, content)
    )


    suspend fun MethodCallAttempt.callOrRelayError() {
        val method = methods[this.callee]
        if (method != null)
            method.invoke(caller, args)
        else
            sendPrivateMsgIfUserIsOnline(
                caller,
                "error: Method not found"
            )
    }

    open suspend fun run() {
        incomingCmds.collect {
            it.callOrRelayError()
        }
    }
}

fun newBot(name: String, initFn: Bot.() -> Unit) = object : BotConstructor {
    override fun new(server: ChatServer) = object : Bot(name, server) {
        init {
            initFn()
        }
    }
}

