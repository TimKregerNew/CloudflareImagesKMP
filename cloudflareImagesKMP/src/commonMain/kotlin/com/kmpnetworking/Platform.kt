package com.kmpnetworking

/**
 * Get environment variable (platform-specific)
 */
expect fun getEnvironmentVariable(name: String): String?

/**
 * Get current timestamp in milliseconds
 */
expect fun currentTimeMillis(): Long

