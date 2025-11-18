package com.kmpnetworking.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Example data model demonstrating JSON serialization
 * You can use this as a template for your own models
 */
@Serializable
data class Post(
    @SerialName("id")
    val id: Int,
    @SerialName("title")
    val title: String,
    @SerialName("body")
    val body: String,
    @SerialName("userId")
    val userId: Int
)

/**
 * Example request body
 */
@Serializable
data class CreatePostRequest(
    @SerialName("title")
    val title: String,
    @SerialName("body")
    val body: String,
    @SerialName("userId")
    val userId: Int
)

