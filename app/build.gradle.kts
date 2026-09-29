plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// La firma arriva dai secret di GitHub Actions: stessa chiave a ogni versione,
// così gli aggiornamenti si installano sopra senza perdere il widget.
val keystoreFile: String? = System.getenv("KEYSTORE_FILE")

android {
    namespace = "io.github.hidetoshi777.meteomeucci"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.hidetoshi777.meteomeucci"
        minSdk = 26
        targetSdk = 34
        versionCode = (project.findProperty("versionCode") as String?)?.toInt() ?: 1
        versionName = (project.findProperty("versionName") as String?) ?: "1.0"
    }

    signingConfigs {
        create("release") {
            if (keystoreFile != null) {
                storeFile = file(keystoreFile)
                storeType = "pkcs12"
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName(if (keystoreFile != null) "release" else "debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation("androidx.work:work-runtime-ktx:2.9.1")
}
