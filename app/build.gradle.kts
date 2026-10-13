plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.miguelcode.warrantyvault"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.miguelcode.warrantyvault"
        minSdk = 26
        // API 35 es el mínimo que Google Play acepta para publicar. Targetear 35 activa
        // el edge-to-edge obligatorio de Android 15: la app ya llama a enableEdgeToEdge(),
        // pero hay que revisar que ninguna pantalla quede bajo las barras del sistema.
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Prioridad: gradle.properties (global o local) > variables de entorno > fallback.
            // En ~/.gradle/gradle.properties las pones una vez y funcionan tanto en CLI
            // como en Android Studio sin tocar nada más.
            val keystorePathProp = findProperty("WARRANTYVAULT_KEYSTORE_PATH") as String?
                ?: System.getenv("KEYSTORE_PATH")
                ?: "../keystore/release.jks"
            storeFile = file(keystorePathProp)
            storePassword = (findProperty("WARRANTYVAULT_KEYSTORE_PASSWORD") as String?)
                ?: System.getenv("KEYSTORE_PASSWORD")
                ?: ""
            keyAlias = (findProperty("WARRANTYVAULT_KEY_ALIAS") as String?)
                ?: System.getenv("KEY_ALIAS")
                ?: ""
            keyPassword = (findProperty("WARRANTYVAULT_KEY_PASSWORD") as String?)
                ?: System.getenv("KEY_PASSWORD")
                ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    bundle {
        language { enableSplit = true }
        density { enableSplit = true }
        abi { enableSplit = true }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.11" }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    // Modules
    implementation(project(":core:domain"))
    implementation(project(":core:database"))
    implementation(project(":core:ui"))
    implementation(project(":feature:home"))
    implementation(project(":feature:detail"))
    implementation(project(":feature:addedit"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:premium"))
    implementation(project(":feature:onboarding"))

    // Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.04.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Koin
    implementation("io.insert-koin:koin-android:3.5.6")
    implementation("io.insert-koin:koin-androidx-compose:3.5.6")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Billing. Desde el 31-ago-2026 Google Play exige la 8 o superior para publicar.
    // Artefacto Java y no -ktx: no usamos sus extensiones y el -ktx 8 viene compilado
    // con una versión de Kotlin más nueva que la del proyecto.
    implementation("com.android.billingclient:billing:8.0.0")

    // Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
}
