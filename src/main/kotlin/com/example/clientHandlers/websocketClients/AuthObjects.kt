package com.example.clientHandlers.websocketClients
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

sealed class AuthRequest(val creds: Credentials) {
    class Login(creds: Credentials) : AuthRequest(creds)
    class Register(creds: Credentials) : AuthRequest(creds)

    companion object {
        fun new(action: AuthAction, creds: Credentials): AuthRequest = when (action) {
            AuthAction.Login -> Login(creds)
            AuthAction.Register -> Register(creds)
        }
    }
}

