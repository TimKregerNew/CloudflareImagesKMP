package com.kmpnetworking

import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*

/**
 * Android-specific HTTP client engine
 * Using OkHttp engine instead of Android engine to support PATCH method
 */
actual fun getHttpClientEngine(): HttpClientEngine {
    return OkHttp.create()
}

/**
 * Get environment variable on Android
 */
actual fun getEnvironmentVariable(name: String): String? {
    return System.getenv(name)
}

/**
 * Get current timestamp on Android
 */
actual fun currentTimeMillis(): Long {
    return System.currentTimeMillis()
}
