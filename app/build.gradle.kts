
plugins {
    id("com.android.application")
    id("kotlin-android")
    id("stringfog")
    id("top.niunaijun.blackobfuscator")
}

android {
    compileSdk = 32

    defaultConfig {
        applicationId = "Xuoos.GenshinImpact.Proxy"
        minSdk = 28
        targetSdk = 32
        versionCode = 3
        versionName = "1.0.4"
        ndk {
              abiFilters += listOf("armeabi-v7a","arm64-v8a", "x86", "x86_64")
        }
    }



    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro", "proguard-log.pro"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.majorVersion
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("libs")
        }
    }

    packagingOptions {
        resources {
            excludes += "/META-INF/**"
            excludes += "/kotlin/**"
            excludes += "/*.txt"
            excludes += "/*.bin"
        }
    }

    applicationVariants.all {
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "GI-Proxy-Xuoos-$name.apk"
        }
    }
}

BlackObfuscator {
    isEnabled = false
    //正常
    //depth = 3
    //极端
    depth = 6
    setObfClass("Xuoos", "kotlin", "com", "org")
    //setBlackClass("GenshinProxy.Xuoos.StringFog")
}

stringfog {
    implementation = "Xuoos.GenshinImpact.Proxy.StringFogImpl"
    enable = true
    //fogPackages = arrayOf("com.github", "org", "Kotlin", "GenshinProxy")
    //kg = com.github.megatronking.stringfog.plugin.kg.RandomKeyGenerator()
    // bytes ** base64
    mode = com.github.megatronking.stringfog.plugin.StringFogMode.bytes
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("lib*.so")))) 
    api("com.github.megatronking.stringfog:xor:4.0.1")
    compileOnly("de.robv.android.xposed:api:82")
    compileOnly("de.robv.android.xposed:api:82:sources")
    implementation("com.github.kyuubiran:EzXHelper:0.9.2")
}
