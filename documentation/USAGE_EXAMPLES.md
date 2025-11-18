# KMP Networking Library - Usage Examples

This document provides comprehensive examples of using the KMP Networking Library.

## Table of Contents

1. [Basic Setup](#basic-setup)
2. [GET Requests](#get-requests)
3. [POST Requests](#post-requests)
4. [PUT Requests](#put-requests)
5. [DELETE Requests](#delete-requests)
6. [Error Handling](#error-handling)
7. [Custom Headers](#custom-headers)
8. [Android Example](#android-example)
9. [iOS Example](#ios-example)

## Basic Setup

### Creating a Client

```kotlin
import com.kmpnetworking.NetworkClient

// Simple client
val client = NetworkClient.create()

// Configured client
val client = NetworkClient.create(
    baseUrl = "https://jsonplaceholder.typicode.com",
    enableLogging = true,
    timeoutMillis = 30000
)
```

## GET Requests

### Simple GET

```kotlin
import com.kmpnetworking.models.Post

suspend fun fetchPosts() {
    val result: NetworkResult<List<Post>> = client.get(
        path = "/posts"
    )
    
    when (result) {
        is NetworkResult.Success -> {
            println("Fetched ${result.data.size} posts")
        }
        is NetworkResult.Error -> {
            println("Error: ${result.message}")
        }
    }
}
```

### GET with Query Parameters

```kotlin
suspend fun fetchPostsByUserId(userId: Int) {
    val result: NetworkResult<List<Post>> = client.get(
        path = "/posts",
        parameters = mapOf("userId" to userId.toString())
    )
    
    result.onSuccess { posts ->
        println("User has ${posts.size} posts")
    }
}
```

### GET with Custom Headers

```kotlin
suspend fun fetchWithAuth() {
    val result: NetworkResult<List<Post>> = client.get(
        path = "/posts",
        headers = mapOf(
            "Authorization" to "Bearer YOUR_TOKEN_HERE",
            "Custom-Header" to "CustomValue"
        )
    )
}
```

### GET Raw String Response

```kotlin
suspend fun fetchRawHtml() {
    val result: NetworkResult<String> = client.getRaw(
        path = "/some/endpoint"
    )
    
    result.onSuccess { html ->
        println("HTML Length: ${html.length}")
    }
}
```

## POST Requests

### Simple POST

```kotlin
import com.kmpnetworking.models.CreatePostRequest

suspend fun createPost() {
    val newPost = CreatePostRequest(
        title = "My New Post",
        body = "This is the content of my post",
        userId = 1
    )
    
    val result: NetworkResult<Post> = client.post(
        path = "/posts",
        body = newPost
    )
    
    result.onSuccess { createdPost ->
        println("Created post with ID: ${createdPost.id}")
    }
}
```

### POST with Custom Headers

```kotlin
suspend fun createPostWithAuth() {
    val newPost = CreatePostRequest(
        title = "Authenticated Post",
        body = "This post was created with authentication",
        userId = 1
    )
    
    val result: NetworkResult<Post> = client.post(
        path = "/posts",
        body = newPost,
        headers = mapOf(
            "Authorization" to "Bearer YOUR_TOKEN",
            "X-Custom-Header" to "Value"
        )
    )
}
```

## PUT Requests

### Update Resource

```kotlin
suspend fun updatePost(postId: Int) {
    val updatedPost = Post(
        id = postId,
        title = "Updated Title",
        body = "Updated body content",
        userId = 1
    )
    
    val result: NetworkResult<Post> = client.put(
        path = "/posts/$postId",
        body = updatedPost
    )
    
    result.onSuccess { post ->
        println("Updated post: ${post.title}")
    }
}
```

## DELETE Requests

### Delete Resource

```kotlin
suspend fun deletePost(postId: Int) {
    val result: NetworkResult<Unit> = client.delete(
        path = "/posts/$postId"
    )
    
    result
        .onSuccess { println("Post deleted successfully") }
        .onError { message, _ -> println("Failed to delete: $message") }
}
```

## Error Handling

### Pattern 1: When Expression

```kotlin
suspend fun fetchWithErrorHandling() {
    val result = client.get<List<Post>>("/posts")
    
    when (result) {
        is NetworkResult.Success -> {
            // Handle success
            processData(result.data)
        }
        is NetworkResult.Error -> {
            // Handle error
            logError(result.message)
            showErrorToUser(result.message)
        }
    }
}
```

### Pattern 2: Callbacks

```kotlin
suspend fun fetchWithCallbacks() {
    client.get<List<Post>>("/posts")
        .onSuccess { posts ->
            println("Success: ${posts.size} posts")
        }
        .onError { message, exception ->
            println("Error: $message")
            exception?.printStackTrace()
        }
}
```

### Pattern 3: getOrNull

```kotlin
suspend fun fetchOrNull() {
    val posts = client.get<List<Post>>("/posts").getOrNull()
    
    if (posts != null) {
        println("Got ${posts.size} posts")
    } else {
        println("Request failed")
    }
}
```

### Pattern 4: getOrDefault

```kotlin
suspend fun fetchOrDefault() {
    val posts = client.get<List<Post>>("/posts")
        .getOrDefault(emptyList())
    
    println("Posts count: ${posts.size}")
}
```

### Pattern 5: Transform with map

```kotlin
suspend fun fetchAndTransform() {
    val titles: NetworkResult<List<String>> = client
        .get<List<Post>>("/posts")
        .map { posts -> posts.map { it.title } }
    
    titles.onSuccess { titleList ->
        titleList.forEach { println(it) }
    }
}
```

## Custom Headers

### Authentication Token

```kotlin
class AuthenticatedClient(token: String) {
    private val client = NetworkClient.create(
        baseUrl = "https://api.example.com"
    )
    
    private val authHeaders = mapOf(
        "Authorization" to "Bearer $token"
    )
    
    suspend fun fetchUserData(): NetworkResult<User> {
        return client.get(
            path = "/user/me",
            headers = authHeaders
        )
    }
    
    suspend fun updateUserData(user: User): NetworkResult<User> {
        return client.put(
            path = "/user/me",
            body = user,
            headers = authHeaders
        )
    }
}
```

## Android Example

### ViewModel with Coroutines

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmpnetworking.NetworkClient
import com.kmpnetworking.NetworkResult
import com.kmpnetworking.models.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PostsViewModel : ViewModel() {
    private val client = NetworkClient.create(
        baseUrl = "https://jsonplaceholder.typicode.com",
        enableLogging = BuildConfig.DEBUG
    )
    
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    fun loadPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = client.get<List<Post>>("/posts")
            
            result
                .onSuccess { posts ->
                    _posts.value = posts
                }
                .onError { message, _ ->
                    _error.value = message
                }
            
            _isLoading.value = false
        }
    }
    
    fun createPost(title: String, body: String) {
        viewModelScope.launch {
            val newPost = CreatePostRequest(
                title = title,
                body = body,
                userId = 1
            )
            
            val result = client.post<CreatePostRequest, Post>(
                path = "/posts",
                body = newPost
            )
            
            result.onSuccess { createdPost ->
                _posts.value = _posts.value + createdPost
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        client.close()
    }
}
```

### Compose UI

```kotlin
@Composable
fun PostsScreen(viewModel: PostsViewModel = viewModel()) {
    val posts by viewModel.posts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadPosts()
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator()
            }
            error != null -> {
                Text("Error: $error", color = Color.Red)
            }
            else -> {
                LazyColumn {
                    items(posts) { post ->
                        PostItem(post)
                    }
                }
            }
        }
    }
}
```

## iOS Example

### SwiftUI ViewModel

```swift
import SwiftUI
import shared
import Combine

class PostsViewModel: ObservableObject {
    @Published var posts: [Post] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let networkClient: NetworkClient
    
    init() {
        networkClient = NetworkClient.Companion().create(
            baseUrl: "https://jsonplaceholder.typicode.com",
            enableLogging: true,
            timeoutMillis: 30000
        )
    }
    
    deinit {
        networkClient.close()
    }
    
    func loadPosts() {
        isLoading = true
        errorMessage = nil
        
        networkClient.get(
            path: "/posts",
            headers: [:],
            parameters: [:]
        ) { [weak self] result in
            DispatchQueue.main.async {
                self?.isLoading = false
                
                if let success = result as? NetworkResult.Success<NSArray> {
                    if let postsArray = success.data as? [Post] {
                        self?.posts = postsArray
                    }
                } else if let error = result as? NetworkResult.Error {
                    self?.errorMessage = error.message
                }
            }
        }
    }
    
    func createPost(title: String, body: String) {
        let newPost = CreatePostRequest(
            title: title,
            body: body,
            userId: 1
        )
        
        networkClient.post(
            path: "/posts",
            body: newPost,
            headers: [:]
        ) { [weak self] result in
            DispatchQueue.main.async {
                if let success = result as? NetworkResult.Success<Post> {
                    if let createdPost = success.data as? Post {
                        self?.posts.append(createdPost)
                    }
                }
            }
        }
    }
}
```

### SwiftUI View

```swift
struct PostsView: View {
    @StateObject private var viewModel = PostsViewModel()
    
    var body: some View {
        NavigationView {
            Group {
                if viewModel.isLoading {
                    ProgressView()
                } else if let error = viewModel.errorMessage {
                    Text("Error: \(error)")
                        .foregroundColor(.red)
                } else {
                    List(viewModel.posts, id: \.id) { post in
                        VStack(alignment: .leading) {
                            Text(post.title)
                                .font(.headline)
                            Text(post.body)
                                .font(.body)
                                .foregroundColor(.secondary)
                        }
                    }
                }
            }
            .navigationTitle("Posts")
            .onAppear {
                viewModel.loadPosts()
            }
        }
    }
}
```

## Best Practices

### 1. Always Close the Client

```kotlin
// In a ViewModel or similar
override fun onCleared() {
    super.onCleared()
    client.close()
}
```

### 2. Use Dependency Injection

```kotlin
class MyRepository(
    private val client: NetworkClient
) {
    suspend fun fetchData(): NetworkResult<Data> {
        return client.get("/data")
    }
}

// Inject the client
val client = NetworkClient.create(baseUrl = "...")
val repository = MyRepository(client)
```

### 3. Handle Loading States

```kotlin
sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

fun loadData() {
    _uiState.value = UiState.Loading
    
    viewModelScope.launch {
        val result = client.get<Data>("/data")
        _uiState.value = when (result) {
            is NetworkResult.Success -> UiState.Success(result.data)
            is NetworkResult.Error -> UiState.Error(result.message)
        }
    }
}
```

### 4. Retry Logic

```kotlin
suspend fun <T> retryRequest(
    times: Int = 3,
    delay: Long = 1000,
    block: suspend () -> NetworkResult<T>
): NetworkResult<T> {
    repeat(times) { attempt ->
        val result = block()
        if (result.isSuccess) return result
        
        if (attempt < times - 1) {
            delay(delay * (attempt + 1))
        }
    }
    return block()
}

// Usage
val result = retryRequest {
    client.get<Data>("/data")
}
```

### 5. Request Caching

```kotlin
class CachedRepository(private val client: NetworkClient) {
    private val cache = mutableMapOf<String, Pair<Long, Any>>()
    private val cacheTimeout = 5 * 60 * 1000L // 5 minutes
    
    suspend inline fun <reified T> getCached(
        key: String,
        path: String
    ): NetworkResult<T> {
        val cached = cache[key]
        if (cached != null && System.currentTimeMillis() - cached.first < cacheTimeout) {
            @Suppress("UNCHECKED_CAST")
            return NetworkResult.Success(cached.second as T)
        }
        
        val result = client.get<T>(path)
        if (result is NetworkResult.Success) {
            cache[key] = Pair(System.currentTimeMillis(), result.data)
        }
        return result
    }
}
```

## Testing Examples

### Mock Network Client

```kotlin
class MockNetworkClient : NetworkClient {
    var mockResponse: NetworkResult<*>? = null
    
    override suspend fun <T> get(
        path: String,
        headers: Map<String, String>,
        parameters: Map<String, String>
    ): NetworkResult<T> {
        @Suppress("UNCHECKED_CAST")
        return mockResponse as NetworkResult<T>
    }
    
    // Implement other methods similarly...
}

// Usage in tests
@Test
fun testSuccessfulFetch() = runTest {
    val mockClient = MockNetworkClient()
    mockClient.mockResponse = NetworkResult.Success(listOf(mockPost))
    
    val repository = MyRepository(mockClient)
    val result = repository.fetchPosts()
    
    assertTrue(result.isSuccess)
}
```

---

For more examples and updates, check the [README.md](README.md) file.

