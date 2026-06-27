package com.amishsxt.drawcast.core

enum class Environment {
    DEV,
    PROD
}

object Config {
    // Switch this to Environment.PROD before releasing
    val environment: Environment = Environment.DEV

    val isLogEnabled: Boolean = environment == Environment.DEV
}
