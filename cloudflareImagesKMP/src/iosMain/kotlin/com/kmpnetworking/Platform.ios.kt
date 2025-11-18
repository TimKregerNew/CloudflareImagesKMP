package com.kmpnetworking

import io.ktor.client.engine.*
import io.ktor.client.engine.darwin.*
import platform.Foundation.*
import platform.posix.getenv
import kotlinx.cinterop.*

/**
 * iOS-specific HTTP client engine using Darwin (NSURLSession)
 */
actual fun getHttpClientEngine(): HttpClientEngine {
    return Darwin.create()
}

/**
 * Get environment variable on iOS
 */
@OptIn(ExperimentalForeignApi::class)
actual fun getEnvironmentVariable(name: String): String? {
    return getenv(name)?.toKString()
}

/**
 * Get current timestamp on iOS
 */
actual fun currentTimeMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}
