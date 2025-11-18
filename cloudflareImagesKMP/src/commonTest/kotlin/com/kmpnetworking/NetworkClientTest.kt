package com.kmpnetworking

import com.kmpnetworking.models.Post
import kotlin.test.Test
import kotlin.test.assertTrue

class NetworkClientTest {
    
    @Test
    fun testNetworkClientCreation() {
        val client = NetworkClient.create(
            baseUrl = "https://jsonplaceholder.typicode.com",
            enableLogging = true
        )
        
        // Client should be created successfully
        assertTrue(true, "NetworkClient created successfully")
        
        client.close()
    }
    
    @Test
    fun testNetworkResultSuccess() {
        val result: NetworkResult<String> = NetworkResult.Success("test data")
        
        assertTrue(result.isSuccess, "Result should be success")
        assertTrue(result.getOrNull() == "test data", "Should return data")
    }
    
    @Test
    fun testNetworkResultError() {
        val result: NetworkResult<String> = NetworkResult.Error("Error message")
        
        assertTrue(result.isError, "Result should be error")
        assertTrue(result.getOrNull() == null, "Should return null on error")
    }
    
    @Test
    fun testNetworkResultMap() {
        val result: NetworkResult<Int> = NetworkResult.Success(5)
        val mapped = result.map { it * 2 }
        
        assertTrue(mapped.getOrNull() == 10, "Mapped value should be 10")
    }
}

