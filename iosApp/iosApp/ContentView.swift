import SwiftUI
import cloudflareImagesKMP

struct ContentView: View {
    @StateObject private var viewModel = NetworkViewModel()
    
    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                Text("KMP Networking Demo")
                    .font(.largeTitle)
                    .padding()
                
                Button("Fetch Posts") {
                    viewModel.fetchPosts()
                }
                .padding()
                .background(Color.blue)
                .foregroundColor(.white)
                .cornerRadius(8)
                
                if viewModel.isLoading {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle())
                } else if let errorMessage = viewModel.errorMessage {
                    Text("Error: \(errorMessage)")
                        .foregroundColor(.red)
                        .padding()
                } else if !viewModel.posts.isEmpty {
                    List(viewModel.posts, id: \.id) { post in
                        PostRowView(post: post)
                    }
                } else {
                    Text("Click the button to fetch posts from the API")
                        .foregroundColor(.gray)
                        .padding()
                }
            }
            .navigationTitle("KMP Networking")
        }
    }
}

struct PostRowView: View {
    let post: Post
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(post.title)
                .font(.headline)
            Text(post.body)
                .font(.body)
                .foregroundColor(.secondary)
            Text("User ID: \(post.userId) | Post ID: \(post.id)")
                .font(.caption)
                .foregroundColor(.gray)
        }
        .padding(.vertical, 4)
    }
}

class NetworkViewModel: ObservableObject {
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
    
    func fetchPosts() {
        Task { @MainActor in
            isLoading = true
            errorMessage = nil
            
            do {
                let result = try await networkClient.get(
                    path: "/posts",
                    headers: [:],
                    parameters: [:]
                )
                
                isLoading = false
                
                if result.isSuccess {
                    if let postsArray = result.getOrNull() as? [Post] {
                        posts = Array(postsArray.prefix(10))
                    }
                } else {
                    errorMessage = "Failed to fetch posts"
                }
            } catch {
                isLoading = false
                errorMessage = error.localizedDescription
            }
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}

