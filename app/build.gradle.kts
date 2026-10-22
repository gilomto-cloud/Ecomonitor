import java.util.Properties
import java.io.FileInputStream
import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

base {
    archivesName.set("EcoMonitor")
}

val rootPropertiesFile = rootProject.file("version.properties")
val versionProperties = Properties()
if (rootPropertiesFile.exists()) {
    versionProperties.load(FileInputStream(rootPropertiesFile))
}

val appVersionCode = System.getenv("VERSION_CODE")?.toIntOrNull()
    ?: versionProperties.getProperty("VERSION_CODE")?.toIntOrNull()
    ?: 10203
val appVersionName = System.getenv("VERSION_NAME")
    ?: versionProperties.getProperty("VERSION_NAME")
    ?: "1.2.3"

val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties()
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

val releaseStoreFile = System.getenv("ECO_STORE_FILE")
    ?: localProperties.getProperty("ECO_STORE_FILE")
    ?: localProperties.getProperty("RELEASE_STORE_FILE")
    ?: "app/keystore.jks"
val releaseStorePassword = System.getenv("ECO_STORE_PASSWORD")
    ?: localProperties.getProperty("ECO_STORE_PASSWORD")
    ?: localProperties.getProperty("RELEASE_STORE_PASSWORD")
    ?: "android123"
val releaseKeyAlias = System.getenv("ECO_KEY_ALIAS")
    ?: localProperties.getProperty("ECO_KEY_ALIAS")
    ?: localProperties.getProperty("RELEASE_KEY_ALIAS")
    ?: "ecomonitor"
val releaseKeyPassword = System.getenv("ECO_KEY_PASSWORD")
    ?: localProperties.getProperty("ECO_KEY_PASSWORD")
    ?: localProperties.getProperty("RELEASE_KEY_PASSWORD")
    ?: "android123"

android {
    namespace = "com.river2.monitor"
    compileSdk = 37

    signingConfigs {
        create("release") {
            storeFile = rootProject.file(releaseStoreFile)
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    }

    defaultConfig {
        applicationId = "com.river2.monitor"
        minSdk = 26
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
    }
    testOptions {
        unitTests.all {
            it.useJUnit()
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi"
        )
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.paho.mqtt)
    implementation(libs.androidx.security.crypto)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
}

val generateKeystoreTask = tasks.register("generateKeystore") {
    group = "signing"
    description = "Generates release keystore app/keystore.jks if missing or empty"
    val keystoreFile = layout.projectDirectory.file("keystore.jks")
    outputs.file(keystoreFile)
    doLast {
        val file = keystoreFile.asFile
        if (!file.exists() || file.length() == 0L) {
            file.parentFile.mkdirs()
            val javaHomeSys = System.getProperty("java.home")
            val javaHomeEnv = System.getenv("JAVA_HOME")
            val keytoolPath = sequenceOf(javaHomeSys, javaHomeEnv)
                .filterNotNull()
                .flatMap { sequenceOf(File(it, "bin/keytool.exe"), File(it, "bin/keytool")) }
                .firstOrNull { it.exists() }
                ?: File("keytool")

            val process = ProcessBuilder(
                keytoolPath.absolutePath,
                "-genkeypair", "-v",
                "-keystore", file.absolutePath,
                "-storetype", "JKS",
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-validity", "10000",
                "-alias", "ecomonitor",
                "-storepass", "android123",
                "-keypass", "android123",
                "-dname", "CN=EcoMonitor, OU=Development, O=River2, C=US"
            ).inheritIO().start()
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw RuntimeException("keytool failed with exit code $exitCode")
            }
            println("Generated keystore at ${file.absolutePath}")
        } else {
            println("Keystore already exists at ${file.absolutePath}")
        }
    }
}

if (System.getenv("ECO_STORE_FILE").isNullOrBlank()) {
    tasks.named("preBuild") {
        dependsOn(generateKeystoreTask)
    }
}
