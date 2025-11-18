import SwiftUI
import cloudflareImagesKMP
import PhotosUI

/**
 * Example SwiftUI view demonstrating Cloudflare Images upload
 * 
 * This shows how all the business logic is in the shared KMP library,
 * with only UI and image selection being platform-specific.
 */
@available(iOS 16.0, *)
struct CloudflareExampleView: View {
    @StateObject private var viewModel = CloudflareViewModel()
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 20) {
                    Text("Cloudflare Image Upload")
                        .font(.title)
                    
                    Text("All upload logic is in the shared KMP library!")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    
                    // Image picker
                    PhotosPicker(
                        selection: $viewModel.selectedPhoto,
                        matching: .images
                    ) {
                        Label("Select Image from Photos", systemImage: "photo")
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.blue)
                            .foregroundColor(.white)
                            .cornerRadius(8)
                    }
                    .disabled(viewModel.isUploading)
                    
                    // Selected image preview
                    if let image = viewModel.selectedImage {
                        Image(uiImage: image)
                            .resizable()
                            .scaledToFit()
                            .frame(maxHeight: 200)
                            .cornerRadius(8)
                        
                        // Upload button
                        Button(action: {
                            viewModel.uploadImage()
                        }) {
                            Text("Upload to Cloudflare")
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(viewModel.isUploading ? Color.gray : Color.green)
                                .foregroundColor(.white)
                                .cornerRadius(8)
                        }
                        .disabled(viewModel.isUploading)
                    }
                    
                    // Progress indicator
                    if viewModel.isUploading {
                        VStack {
                            ProgressView()
                            Text("Uploading: \(Int(viewModel.uploadProgress * 100))%")
                                .font(.caption)
                        }
                    }
                    
                    // Error message
                    if let error = viewModel.errorMessage {
                        Text("Error: \(error)")
                            .foregroundColor(.red)
                            .padding()
                            .frame(maxWidth: .infinity)
                            .background(Color.red.opacity(0.1))
                            .cornerRadius(8)
                    }
                    
                    // Success message
                    if let url = viewModel.uploadedImageUrl {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("✅ Upload Successful!")
                                .font(.headline)
                                .foregroundColor(.green)
                            
                            Text("URL: \(url)")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .padding()
                        .frame(maxWidth: .infinity)
                        .background(Color.green.opacity(0.1))
                        .cornerRadius(8)
                    }
                    
                    Divider()
                    
                    // Additional operations
                    Text("Other Operations")
                        .font(.title3)
                    
                    HStack(spacing: 12) {
                        Button("List Images") {
                            viewModel.listImages()
                        }
                        .buttonStyle(.bordered)
                        .disabled(viewModel.isLoadingImages)
                        
                        Button("Get Stats") {
                            viewModel.getStats()
                        }
                        .buttonStyle(.bordered)
                    }
                    
                    if viewModel.isLoadingImages {
                        ProgressView("Loading images...")
                            .padding()
                    }
                    
                    if let info = viewModel.infoMessage {
                        Text(info)
                            .font(.caption)
                            .foregroundColor(.blue)
                            .padding()
                    }
                    
                    // Display Cloudflare images
                    if !viewModel.cloudflareImages.isEmpty {
                        Divider()
                            .padding(.vertical)
                        
                        Text("Your Cloudflare Images")
                            .font(.title3)
                            .padding(.top)
                        
                        ScrollView {
                            LazyVStack(spacing: 12) {
                                ForEach(Array(viewModel.cloudflareImages.enumerated()), id: \.element.id) { index, image in
                                    CloudflareImageRowView(image: image, onDelete: {
                                        viewModel.deleteImage(imageId: image.id)
                                    })
                                }
                            }
                            .padding()
                        }
                        .frame(maxHeight: 400)
                    }
                }
                .padding()
            }
            .navigationTitle("KMP Cloudflare")
            .onChange(of: viewModel.selectedPhoto) { _ in
                viewModel.loadImage()
            }
        }
    }
}

@available(iOS 16.0, *)
class CloudflareViewModel: ObservableObject {
    @Published var selectedPhoto: PhotosPickerItem?
    @Published var selectedImage: UIImage?
    @Published var isUploading = false
    @Published var uploadProgress: Float = 0.0
    @Published var uploadedImageUrl: String?
    @Published var errorMessage: String?
    @Published var infoMessage: String?
    @Published var cloudflareImages: [CloudflareImageUploadResponse] = []
    @Published var isLoadingImages = false
    
    private let uploader: CloudflareImageUploader
    
    init() {
        // Initialize the uploader with credentials from environment variables
        guard let accountId = ProcessInfo.processInfo.environment["CLOUDFLARE_ACCOUNT_ID"],
              let apiToken = ProcessInfo.processInfo.environment["CLOUDFLARE_API_TOKEN"] else {
            fatalError("CLOUDFLARE_ACCOUNT_ID and CLOUDFLARE_API_TOKEN environment variables must be set")
        }
        
        uploader = CloudflareImageUploader.Companion().create(
            accountId: accountId,
            apiToken: apiToken,
            enableLogging: true,
            timeoutMillis: 60000
        )
    }
    
    deinit {
        uploader.close()
    }
    
    func loadImage() {
        Task {
            guard let selectedPhoto = selectedPhoto else { return }
            
            if let data = try? await selectedPhoto.loadTransferable(type: Data.self),
               let image = UIImage(data: data) {
                await MainActor.run {
                    self.selectedImage = image
                    self.uploadedImageUrl = nil
                    self.errorMessage = nil
                }
            }
        }
    }
    
    func uploadImage() {
        guard let image = selectedImage else { return }
        
        Task { @MainActor in
            isUploading = true
            uploadProgress = 0.0
            errorMessage = nil
            uploadedImageUrl = nil
            
            // Create ImageData from UIImage - platform-specific
            guard let imageData = IOSImageData.Companion().fromUIImage(
                image: image,
                compressionQuality: 0.9,
                format: .jpeg,
                fileName: "photo.jpg"
            ) else {
                errorMessage = "Failed to process image"
                isUploading = false
                return
            }
            
            // Upload - ALL business logic in shared library!
            // Must call from MainActor for KMP suspend functions
            do {
                let result = try await uploader.uploadImage(
                    imageData: imageData,
                    id: nil,
                    requireSignedURLs: false,
                    metadata: [
                        "source": "ios_app",
                        "timestamp": String(Int(Date().timeIntervalSince1970))
                    ],
                    onProgress: { [weak self] progress in
                        DispatchQueue.main.async {
                            self?.uploadProgress = progress.floatValue
                        }
                    }
                )
                
                isUploading = false
                
                // Check if success
                if result.isSuccess {
                    if let response = result.getOrNull() {
                        uploadedImageUrl = response.publicUrl
                    }
                } else if result.isError {
                    errorMessage = "Upload failed"
                }
            } catch {
                isUploading = false
                errorMessage = error.localizedDescription
            }
        }
    }
    
    func listImages() {
        Task { @MainActor in
            isLoadingImages = true
            errorMessage = nil
            infoMessage = nil
            
            do {
                let result = try await uploader.listImages(page: 1, perPage: 50)
                
                if result.isSuccess {
                    if let response = result.getOrNull() {
                        cloudflareImages = response.images
                        infoMessage = "Found \(response.totalCount) total images"
                    }
                } else {
                    errorMessage = "Failed to list images"
                    cloudflareImages = []
                }
            } catch {
                errorMessage = error.localizedDescription
                cloudflareImages = []
            }
            
            isLoadingImages = false
        }
    }
    
    func getStats() {
        Task { @MainActor in
            do {
                let result = try await uploader.getUsageStats()
                
                if result.isSuccess {
                    if let stats = result.getOrNull() {
                        infoMessage = "Used: \(stats.count.current) / \(stats.count.allowed)"
                    }
                } else {
                    errorMessage = "Failed to get stats"
                }
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }
    
    func deleteImage(imageId: String) {
        Task { @MainActor in
            do {
                let result = try await uploader.deleteImage(imageId: imageId)
                
                if result.isSuccess {
                    // Remove from local list
                    cloudflareImages.removeAll { $0.id == imageId }
                    infoMessage = "Image deleted successfully"
                } else {
                    errorMessage = "Failed to delete image"
                }
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }
}

@available(iOS 16.0, *)
struct CloudflareImageRowView: View {
    let image: CloudflareImageUploadResponse
    let onDelete: () -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(image.filename.isEmpty ? "Untitled" : image.filename)
                        .font(.headline)
                        .lineLimit(1)
                    
                    Text("ID: \(image.id)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    Text("Uploaded: \(formatDate(image.uploaded))")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                HStack(spacing: 12) {
                    if let publicUrl = image.publicUrl as String?, !publicUrl.isEmpty, let url = URL(string: publicUrl) {
                        Link(destination: url) {
                            Image(systemName: "arrow.up.right.square")
                                .foregroundColor(.blue)
                        }
                    }
                    
                    Button(action: onDelete) {
                        Image(systemName: "trash")
                            .foregroundColor(.red)
                    }
                }
            }
            
            // Image preview
            if let publicUrl = image.publicUrl as String?, !publicUrl.isEmpty, let url = URL(string: publicUrl) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .empty:
                        ProgressView()
                            .frame(height: 100)
                    case .success(let loadedImage):
                        loadedImage
                            .resizable()
                            .scaledToFit()
                            .frame(maxHeight: 200)
                            .cornerRadius(8)
                    case .failure:
                        Image(systemName: "photo")
                            .foregroundColor(.gray)
                            .frame(height: 100)
                    @unknown default:
                        EmptyView()
                    }
                }
            }
            
            // Variants
            if !image.variants.isEmpty {
                Text("Variants: \(image.variants.count)")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            
            // Metadata
            if let metadata = image.metadata, !metadata.isEmpty {
                VStack(alignment: .leading, spacing: 2) {
                    ForEach(Array(metadata.keys.sorted()), id: \.self) { key in
                        if let value = metadata[key] {
                            Text("\(key): \(value)")
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                    }
                }
            }
        }
        .padding()
        .background(Color.gray.opacity(0.1))
        .cornerRadius(10)
    }
    
    private func formatDate(_ dateString: String) -> String {
        let formatter = ISO8601DateFormatter()
        if let date = formatter.date(from: dateString) {
            let displayFormatter = DateFormatter()
            displayFormatter.dateStyle = .short
            displayFormatter.timeStyle = .short
            return displayFormatter.string(from: date)
        }
        return dateString
    }
}

@available(iOS 16.0, *)
struct CloudflareExampleView_Previews: PreviewProvider {
    static var previews: some View {
        CloudflareExampleView()
    }
}



