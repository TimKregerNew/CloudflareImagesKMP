import SwiftUI

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            if #available(iOS 16.0, *) {
                CloudflareExampleView()
            } else {
                Text("iOS 16.0 or later required")
            }
        }
    }
}

