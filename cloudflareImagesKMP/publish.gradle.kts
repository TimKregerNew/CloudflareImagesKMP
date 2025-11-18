// Maven publishing configuration for JFrog Artifactory

apply(plugin = "maven-publish")

// Version information
val libraryVersion = project.findProperty("library.version")?.toString() ?: "1.0.0"
val libraryGroupId = "com.kmpnetworking"
val libraryArtifactId = "cloudflareImagesKMP"

// JFrog Artifactory configuration
val artifactoryUrl = project.findProperty("artifactory.url")?.toString() 
    ?: System.getenv("ARTIFACTORY_URL")
    ?: "https://your-company.jfrog.io/artifactory/libs-release-local"

val artifactoryUsername = project.findProperty("artifactory.username")?.toString()
    ?: System.getenv("ARTIFACTORY_USERNAME")
    ?: ""

val artifactoryPassword = project.findProperty("artifactory.password")?.toString()
    ?: System.getenv("ARTIFACTORY_PASSWORD")
    ?: ""

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("release") {
            groupId = libraryGroupId
            artifactId = libraryArtifactId
            version = libraryVersion
            
            // For Android AAR
            afterEvaluate {
                from(components["release"])
            }
            
            pom {
                name.set("KMP Networking Library")
                description.set("Cross-platform networking library for Android and iOS using Kotlin Multiplatform")
                url.set("https://github.com/yourusername/kmp-networking")
                
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                
                developers {
                    developer {
                        id.set("yourid")
                        name.set("Your Name")
                        email.set("your.email@example.com")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/yourusername/kmp-networking.git")
                    developerConnection.set("scm:git:ssh://github.com/yourusername/kmp-networking.git")
                    url.set("https://github.com/yourusername/kmp-networking")
                }
            }
        }
    }
    
    repositories {
        maven {
            name = "Artifactory"
            url = uri(artifactoryUrl)
            credentials {
                username = artifactoryUsername
                password = artifactoryPassword
            }
        }
    }
}


