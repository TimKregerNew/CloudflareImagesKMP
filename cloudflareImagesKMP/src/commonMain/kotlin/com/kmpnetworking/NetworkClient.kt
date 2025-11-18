package com.kmpnetworking

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Main networking client for making HTTP requests across platforms.
 * Supports GET, POST, PUT, DELETE operations with automatic JSON serialization.
 */
class NetworkClient(
    private val baseUrl: String = "",
    private val enableLogging: Boolean = true,
    private val timeoutMillis: Long = 30000
) {
    
    @PublishedApi
    internal val httpClient = HttpClient(getHttpClientEngine()) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        
        if (enableLogging) {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
            }
        }
        
        install(HttpTimeout) {
            requestTimeoutMillis = timeoutMillis
            connectTimeoutMillis = timeoutMillis
            socketTimeoutMillis = timeoutMillis
        }
        
        defaultRequest {
            if (baseUrl.isNotEmpty()) {
                url(baseUrl)
            }
        }
    }
    
    /**
     * Perform a GET request
     */
    suspend inline fun <reified T> get(
        path: String,
        headers: Map<String, String> = emptyMap(),
        parameters: Map<String, String> = emptyMap()
    ): NetworkResult<T> {
        return safeRequest {
            httpClient.get(path) {
                headers.forEach { (key, value) ->
                    header(key, value)
                }
                parameters.forEach { (key, value) ->
                    parameter(key, value)
                }
            }.body()
        }
    }
    
    /**
     * Perform a POST request
     */
    suspend inline fun <reified T, reified R> post(
        path: String,
        body: T,
        headers: Map<String, String> = emptyMap()
    ): NetworkResult<R> {
        return safeRequest {
            httpClient.post(path) {
                contentType(ContentType.Application.Json)
                headers.forEach { (key, value) ->
                    header(key, value)
                }
                setBody(body)
            }.body()
        }
    }
    
    /**
     * Perform a PUT request
     */
    suspend inline fun <reified T, reified R> put(
        path: String,
        body: T,
        headers: Map<String, String> = emptyMap()
    ): NetworkResult<R> {
        return safeRequest {
            httpClient.put(path) {
                contentType(ContentType.Application.Json)
                headers.forEach { (key, value) ->
                    header(key, value)
                }
                setBody(body)
            }.body()
        }
    }
    
    /**
     * Perform a DELETE request
     */
    suspend inline fun <reified T> delete(
        path: String,
        headers: Map<String, String> = emptyMap()
    ): NetworkResult<T> {
        return safeRequest {
            httpClient.delete(path) {
                headers.forEach { (key, value) ->
                    header(key, value)
                }
            }.body()
        }
    }
    
    /**
     * Get raw response as String
     */
    suspend fun getRaw(
        path: String,
        headers: Map<String, String> = emptyMap()
    ): NetworkResult<String> {
        return safeRequest {
            httpClient.get(path) {
                headers.forEach { (key, value) ->
                    header(key, value)
                }
            }.bodyAsText()
        }
    }
    
    /**
     * Close the client and release resources
     */
    fun close() {
        httpClient.close()
    }
    
    companion object {
        /**
         * Create a default NetworkClient instance
         */
        fun create(
            baseUrl: String = "",
            enableLogging: Boolean = true,
            timeoutMillis: Long = 30000
        ): NetworkClient {
            return NetworkClient(baseUrl, enableLogging, timeoutMillis)
        }
    }
}

/**
 * Safely execute a network request and wrap the result
 */
@PublishedApi
internal suspend inline fun <T> safeRequest(
    crossinline request: suspend () -> T
): NetworkResult<T> {
    return try {
        NetworkResult.Success(request())
    } catch (e: Exception) {
        NetworkResult.Error(
            message = e.message ?: "Unknown error occurred",
            exception = e
        )
    }
}

/**
 * Platform-specific HTTP client engine
 * Will be implemented in androidMain and iosMain
 */
expect fun getHttpClientEngine(): io.ktor.client.engine.HttpClientEngine

