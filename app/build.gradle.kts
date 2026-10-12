import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

// Load release keystore credentials from keystore.properties, local.properties, or environment variables
val keystorePropertiesFile = rootProject.file("keystore.properties").takeIf { it.exists() }
    ?: project.file("keystore.properties").takeIf { it.exists() }
    ?: rootProject.file("local.properties").takeIf { it.exists() }

val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile != null) {
        load(FileInputStream(keystorePropertiesFile))
    }
}

val releaseStoreFile: String? = keystoreProperties.getProperty("storeFile")
    ?: System.getenv("KEYSTORE_FILE")
    ?: System.getenv("KEYSTORE_PATH")
val releaseStorePassword: String? = keystoreProperties.getProperty("storePassword")
    ?: System.getenv("KEYSTORE_PASSWORD")
val releaseKeyAlias: String? = keystoreProperties.getProperty("keyAlias")
    ?: System.getenv("KEY_ALIAS")
val releaseKeyPassword: String? = keystoreProperties.getProperty("keyPassword")
    ?: System.getenv("KEY_PASSWORD")

android {
    namespace = "com.prasad.daylatch"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.prasad.daylatch"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "2.0.0"
    }

    signingConfigs {
        create("release") {
            if (!releaseStoreFile.isNullOrBlank() &&
                !releaseStorePassword.isNullOrBlank() &&
                !releaseKeyAlias.isNullOrBlank() &&
                !releaseKeyPassword.isNullOrBlank()
            ) {
                val resolvedStoreFile = File(releaseStoreFile).let {
                    if (it.isAbsolute) it
                    else if (rootProject.file(releaseStoreFile).exists()) rootProject.file(releaseStoreFile)
                    else project.file(releaseStoreFile)
                }
                storeFile = resolvedStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null && releaseSigning.storeFile?.exists() == true) {
                signingConfig = releaseSigning
            }
        }
    }

    buildFeatures { compose = true; buildConfig = true }
    lint { checkReleaseBuilds = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.8" }

    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
