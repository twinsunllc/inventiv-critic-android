plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("maven-publish")
    id("signing")
}

android {
    namespace = "io.inventiv.critic"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("proguard-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        targetSdk = 35
    }

    lint {
        targetSdk = 35
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup:seismic:1.0.3")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    implementation("com.google.android.material:material:1.12.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}

// build a jar with source files
tasks.register<Jar>("sourcesJar") {
    from(android.sourceSets["main"].java.srcDirs)
    archiveClassifier.set("sources")
}

tasks.register<Javadoc>("javadocTask") {
    isFailOnError = false
    source = android.sourceSets["main"].java.getSourceFiles()
    classpath += project.files(android.bootClasspath)
}

// build a jar with javadoc
tasks.register<Jar>("javadocJar") {
    dependsOn("javadocTask")
    archiveClassifier.set("javadoc")
    from(tasks.named<Javadoc>("javadocTask").get().destinationDir)
}

artifacts {
    archives(tasks.named("sourcesJar"))
    archives(tasks.named("javadocJar"))
}

// ---------------------------------------------------------------------------
// Maven publishing (Maven Central via Sonatype OSSRH)
// ---------------------------------------------------------------------------

val libVersion: String = project.findProperty("VERSION_NAME") as String? ?: "2.0.0"
val libGroup: String = project.findProperty("GROUP") as String? ?: "io.inventiv.critic"
val libArtifactId: String = project.findProperty("POM_ARTIFACT_ID") as String? ?: "inventiv-critic-android"

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = libGroup
                artifactId = libArtifactId
                version = libVersion

                from(components["release"])

                artifact(tasks["sourcesJar"])
                artifact(tasks["javadocJar"])

                pom {
                    name.set("Inventiv Critic Android Library")
                    description.set("Android library for building integrations with Inventiv Critic.")
                    url.set("https://github.com/twinsunllc/inventiv-critic-android")
                    inceptionYear.set("2018")

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }

                    developers {
                        developer {
                            id.set("inventiv")
                            name.set("Inventiv")
                            url.set("https://inventiv.io/")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/twinsunllc/inventiv-critic-android.git")
                        developerConnection.set("scm:git:ssh://github.com/twinsunllc/inventiv-critic-android.git")
                        url.set("https://github.com/twinsunllc/inventiv-critic-android")
                    }
                }
            }
        }

        repositories {
            maven {
                name = "MavenCentral"
                val isSnapshot = libVersion.endsWith("SNAPSHOT")
                url = uri(
                    if (isSnapshot)
                        "https://s01.oss.sonatype.org/content/repositories/snapshots/"
                    else
                        "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                )
                credentials {
                    username = System.getenv("OSSRH_USERNAME")
                        ?: project.findProperty("ossrhUsername") as String?
                    password = System.getenv("OSSRH_PASSWORD")
                        ?: project.findProperty("ossrhPassword") as String?
                }
            }
        }
    }

    signing {
        val signingKey: String? = System.getenv("GPG_SIGNING_KEY")
        val signingPassword: String? = System.getenv("GPG_SIGNING_PASSWORD")
        if (signingKey != null && signingPassword != null) {
            useInMemoryPgpKeys(signingKey, signingPassword)
            sign(publishing.publications["release"])
        }
    }
}
