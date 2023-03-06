package com.example.hubClients.websocketClients

import com.example.server.ChatServer

data class Credentials(val username: String, val password: String)


enum class AuthAction(val str: String) {
    Login("l"), Register("r");
}

sealed interface AuthRequestStatus {
    sealed class Passed(val creds: Credentials) : AuthRequestStatus {
        class Login(creds: Credentials) : Passed(creds)
        class Register(creds: Credentials) : Passed(creds)
    }

    sealed class Failed(val reason: String) : AuthRequestStatus {
        object InvalidCredentials : Failed("Invalid credentials")
        object UsernameAlreadyOnline : Failed("Username already online")
        object UsernameAlreadyExists : Failed("Username already exists")
    }
}

// TODO get rid of this alias?
fun ChatServer.check(request: AuthRequest) = request.check(this)

sealed class AuthRequest(val creds: Credentials) {
    /**
     * @param server the server to check against
     */
    abstract fun check(server: ChatServer): AuthRequestStatus

    class Login(creds: Credentials) : AuthRequest(creds) {
        override fun check(server: ChatServer) =
            if (!server.db.match(creds)) {
                AuthRequestStatus.Failed.InvalidCredentials
            } else if (server.hub.userIsOnline(creds.username)) {
                AuthRequestStatus.Failed.UsernameAlreadyOnline
            } else {
                AuthRequestStatus.Passed.Login(creds)
            }
    }

    class Register(creds: Credentials) : AuthRequest(creds) {
        override fun check(server: ChatServer) =
            if (server.db.match(creds)) {
                AuthRequestStatus.Failed.UsernameAlreadyExists
            } else {
                AuthRequestStatus.Passed.Register(creds)
            }
    }

    companion object {
        fun new(action: AuthAction, creds: Credentials): AuthRequest = when (action) {
            AuthAction.Login -> Login(creds)
            AuthAction.Register -> Register(creds)
        }
    }
}

