// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://api.xposed.info")
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.2.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.10")
        classpath("com.github.CodingGay:BlackObfuscator-ASPlugin:3.7")
        classpath("com.github.megatronking.stringfog:gradle-plugin:4.0.1")
        classpath("com.github.megatronking.stringfog:xor:4.0.1")
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

tasks.register("Delete", Delete::class) {
    delete(rootProject.buildDir)
}
