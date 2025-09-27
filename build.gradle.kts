buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") // Include jitpack here for plugin dependencies if needed
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.13.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.10")
    }
}

allprojects {
    // keep this empty or remove repositories block to avoid conflict with settings.gradle.kts
}
