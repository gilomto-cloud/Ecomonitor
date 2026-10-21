import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

plugins {
    id("com.android.application") version "9.4.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
}

val bumpTypeProvider = providers.gradleProperty("bump").orElse("patch")

tasks.register("bumpVersion") {
    group = "versioning"
    description = "Bumps the app version code and display name in version.properties"

    doLast {
        val versionFile = File(rootDir, "version.properties")
        val props = Properties()
        if (versionFile.exists()) {
            FileInputStream(versionFile).use { props.load(it) }
        }

        val currentCode = props.getProperty("VERSION_CODE")?.toIntOrNull() ?: 10301
        val currentName = props.getProperty("VERSION_NAME") ?: "1.3.1"
        val bumpType = bumpTypeProvider.get()
        val parts = currentName.split(".").map { it.toIntOrNull() ?: 0 }

        val major = parts.getOrElse(0) { 0 }
        val minor = parts.getOrElse(1) { 0 }
        val patch = parts.getOrElse(2) { 0 }

        val nextName = when (bumpType.lowercase()) {
            "major" -> "${major + 1}.0.0"
            "minor" -> "$major.${minor + 1}.0"
            else -> "$major.$minor.${patch + 1}"
        }

        val nextParts = nextName.split(".").map { it.toIntOrNull() ?: 0 }
        val nextCode = nextParts[0] * 10000 + nextParts[1] * 100 + nextParts[2]

        props.setProperty("VERSION_CODE", nextCode.toString())
        props.setProperty("VERSION_NAME", nextName)
        FileOutputStream(versionFile).use { props.store(it, "Auto-generated version bump") }

        println("Bumped version to $nextName ($nextCode)")
    }
}
