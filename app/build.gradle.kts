// ============================================================================
// app/build.gradle.kts — Módulo de la aplicación (Launcher Leanback)
// ============================================================================
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.tuusuario.customlauncher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.tuusuario.customlauncher"
        // minSdk 22 = Android 5.1 → cubre Fire TV Stick de 2.ª generación (Fire OS 5) en adelante
        minSdk = 22
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Interfaz de TV (filas, tarjetas, navegación con D-pad)
    implementation("androidx.leanback:leanback:1.0.0")
    // FragmentActivity + registerForActivityResult
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.core:core-ktx:1.13.1")
    // Sincronización periódica en segundo plano con Kino TV
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    // Carga de logotipos de canales (assets o URL)
    implementation("com.github.bumptech.glide:glide:4.16.0")
}
