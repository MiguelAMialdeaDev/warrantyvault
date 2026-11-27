plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Clave pública de licencias de Play (Play Console > Monetizar > Configuración de
// monetización > Licencias). No es secreta, pero va en ~/.gradle/gradle.properties junto
// al resto de datos de publicación. Sin ella, PurchaseVerifier rechaza todas las compras.
val playLicenseKey = (findProperty("WARRANTYVAULT_PLAY_LICENSE_KEY") as String?).orEmpty().trim()

val signingProps: Map<String, String> = listOf(
    "WARRANTYVAULT_KEYSTORE_PATH",
    "WARRANTYVAULT_KEYSTORE_PASSWORD",
    "WARRANTYVAULT_KEY_ALIAS",
    "WARRANTYVAULT_KEY_PASSWORD"
).mapNotNull { key -> (findProperty(key) as String?)?.takeIf { it.isNotBlank() }?.let { key to it } }.toMap()

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
        versionCode = 4
        versionName = "1.0.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "PLAY_LICENSE_KEY", "\"$playLicenseKey\"")
    }

    signingConfigs {
        // Los cuatro datos salen de ~/.gradle/gradle.properties y nada más. Antes, si
        // faltaba alguno, se caía en silencio a ../keystore/release.jks (una clave vieja
        // que no es la de subida) y a contraseñas vacías. Si falta algo, el build de
        // publicación falla con un mensaje claro (ver el final del fichero).
        create("release") {
            signingProps["WARRANTYVAULT_KEYSTORE_PATH"]?.let { storeFile = file(it) }
            storePassword = signingProps["WARRANTYVAULT_KEYSTORE_PASSWORD"]
            keyAlias = signingProps["WARRANTYVAULT_KEY_ALIAS"]
            keyPassword = signingProps["WARRANTYVAULT_KEY_PASSWORD"]
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
    implementation("com.google.android.play:review-ktx:2.0.2")

    // Tests
    testImplementation("junit:junit:4.13.2")
    // org.json real: el de android.jar en los tests de JVM solo son stubs que lanzan.
    testImplementation("org.json:json:20240303")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
}

// A release build without the Play license key would reject every purchase and take premium
// away from paying users, so packaging a release fails fast with a clear message instead.
// Only tasks that package, sign or publish a release need the secrets: unit tests, lint and
// debug builds work from a clean clone.
val releasePackagingTask = Regex("^(assemble|bundle|package|sign|publish)\\w*Release\\w*$")
gradle.taskGraph.whenReady {
    val buildsRelease = allTasks.any { it.project == project && releasePackagingTask.matches(it.name) }
    if (!buildsRelease) return@whenReady
    if (playLicenseKey.isEmpty()) {
        throw GradleException(
            "Missing WARRANTYVAULT_PLAY_LICENSE_KEY in ~/.gradle/gradle.properties " +
                "(Play Console > Monetize > Monetization setup > Licensing)."
        )
    }
    val missingSigning = listOf(
        "WARRANTYVAULT_KEYSTORE_PATH",
        "WARRANTYVAULT_KEYSTORE_PASSWORD",
        "WARRANTYVAULT_KEY_ALIAS",
        "WARRANTYVAULT_KEY_PASSWORD"
    ).filterNot { it in signingProps }
    if (missingSigning.isNotEmpty()) {
        throw GradleException("Missing in ~/.gradle/gradle.properties: " + missingSigning.joinToString())
    }
    val keystore = file(signingProps.getValue("WARRANTYVAULT_KEYSTORE_PATH"))
    if (!keystore.isFile) throw GradleException("Upload keystore not found: $keystore")
}
