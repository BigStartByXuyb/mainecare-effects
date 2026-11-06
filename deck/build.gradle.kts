import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("maven-publish")
}

android {
    namespace = "com.mainecare.effects.deck"
    compileSdk = 36

    defaultConfig {
        minSdk = 33
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.03.00")
    api(project(":motion"))
    api(composeBom)
    api("androidx.compose.ui:ui")
    api("androidx.compose.ui:ui-graphics")
    api("androidx.compose.foundation:foundation")
    testImplementation("junit:junit:4.13.2")
}

group = "com.mainecare.effects"
version = "0.1.4"

afterEvaluate {
    publishing {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/BigStartByXuyb/mainecare-effects")
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifact(rootProject.file("LICENSE")) {
                    classifier = "license"
                    extension = "txt"
                }
                artifact(rootProject.file("NOTICE")) {
                    classifier = "notice"
                    extension = "txt"
                }
                artifact(rootProject.file("THIRD_PARTY_NOTICES.md")) {
                    classifier = "third-party-notices"
                    extension = "md"
                }
                pom {
                    name.set("MaineCare Effects ${project.name}")
                    description.set("Reusable Android Compose liquid-glass and deck effects for MaineCare.")
                    url.set("https://github.com/BigStartByXuyb/mainecare-effects")
                    licenses {
                        license {
                            name.set("Apache-2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        }
                    }
                    scm {
                        url.set("https://github.com/BigStartByXuyb/mainecare-effects")
                        connection.set("scm:git:https://github.com/BigStartByXuyb/mainecare-effects.git")
                    }
                }
            }
        }
    }

    listOf("releaseSourcesJar", "debugSourcesJar").forEach { taskName ->
        tasks.matching { it.name == taskName }.configureEach {
            (this as org.gradle.jvm.tasks.Jar).from(rootProject.file("LICENSE")) { into("META-INF") }
            (this as org.gradle.jvm.tasks.Jar).from(rootProject.file("NOTICE")) { into("META-INF") }
            (this as org.gradle.jvm.tasks.Jar).from(rootProject.file("THIRD_PARTY_NOTICES.md")) { into("META-INF") }
        }
    }
}
