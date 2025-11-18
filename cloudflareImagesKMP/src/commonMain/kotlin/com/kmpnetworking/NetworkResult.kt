package com.kmpnetworking

/**
 * Sealed class representing the result of a network operation
 */
sealed class NetworkResult<out T> {
    /**
     * Success result containing the data
     */
    data class Success<T>(val data: T) : NetworkResult<T>()
    
    /**
     * Error result containing error information
     */
    data class Error(
        val message: String,
        val exception: Exception? = null
    ) : NetworkResult<Nothing>()
    
    /**
     * Check if the result is successful
     */
    val isSuccess: Boolean
        get() = this is Success
    
    /**
     * Check if the result is an error
     */
    val isError: Boolean
        get() = this is Error
    
    /**
     * Get data if success, null otherwise
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }
    
    /**
     * Get data if success, default value otherwise
     */
    fun getOrDefault(defaultValue: @UnsafeVariance T): @UnsafeVariance T = when (this) {
        is Success -> data
        is Error -> defaultValue
    }
    
    /**
     * Transform the success data
     */
    inline fun <R> map(transform: (T) -> R): NetworkResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(message, exception)
    }
    
    /**
     * Execute action on success
     */
    inline fun onSuccess(action: (T) -> Unit): NetworkResult<T> {
        if (this is Success) action(data)
        return this
    }
    
    /**
     * Execute action on error
     */
    inline fun onError(action: (String, Exception?) -> Unit): NetworkResult<T> {
        if (this is Error) action(message, exception)
        return this
    }
}

