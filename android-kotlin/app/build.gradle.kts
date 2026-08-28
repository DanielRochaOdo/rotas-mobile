import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

val rootEnvProperties = Properties().apply {
    val envFile = rootProject.file("../.env")
    if (envFile.exists()) {
        envFile.readLines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach
            val separatorIndex = line.indexOf('=')
            if (separatorIndex <= 0) return@forEach

            val key = line.substring(0, separatorIndex).trim()
            var value = line.substring(separatorIndex + 1).trim()
            if ((value.startsWith("\"") && value.endsWith("\"")) ||
                (value.startsWith("'") && value.endsWith("'"))
            ) {
                value = value.substring(1, value.length - 1)
            }
            if (key.isNotBlank()) setProperty(key, value)
        }
    }
}

fun readConfigValue(key: String): String {
    val envValue = System.getenv(key)
    if (!envValue.isNullOrBlank()) return envValue

    val propertyValue = localProperties.getProperty(key)
    if (!propertyValue.isNullOrBlank()) return propertyValue

    return rootEnvProperties.getProperty(key).orEmpty()
}

fun escapeBuildConfig(value: String): String = value
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

val supabaseUrl = readConfigValue("SUPABASE_URL")
val supabaseAnonKey = readConfigValue("SUPABASE_ANON_KEY")
val appVersionName = (project.findProperty("ODONTOART_APP_VERSION_NAME") as? String)
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?: "1.0.0"
val appVersionCode = (project.findProperty("ODONTOART_APP_VERSION_CODE") as? String)
    ?.trim()
    ?.toIntOrNull()
    ?: 1

android {
    namespace = "com.odontoart.rotas"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.odontoart.rotas"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
        buildConfigField("String", "SUPABASE_URL", "\"${escapeBuildConfig(supabaseUrl)}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${escapeBuildConfig(supabaseAnonKey)}\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file("../keystore/release.jks")
            storePassword = System.getenv("ODONTOART_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("ODONTOART_KEY_ALIAS") ?: "odontoart"
            keyPassword = System.getenv("ODONTOART_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }

        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

val releaseVersionedFileName = run {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
    "odontoart-rotas-v${appVersionName}-b${appVersionCode}-${timestamp}"
}

tasks.register<Copy>("exportVersionedReleaseApk") {
    dependsOn("assembleRelease")
    from(layout.buildDirectory.file("outputs/apk/release/app-release.apk"))
    into(layout.buildDirectory.dir("outputs/apk/release-versioned"))
    rename { "${releaseVersionedFileName}.apk" }
}

tasks.register<Copy>("exportVersionedReleaseBundle") {
    dependsOn("bundleRelease")
    from(layout.buildDirectory.file("outputs/bundle/release/app-release.aab"))
    into(layout.buildDirectory.dir("outputs/bundle/release-versioned"))
    rename { "${releaseVersionedFileName}.aab" }
}

tasks.register("assembleVersionedRelease") {
    group = "build"
    description = "Gera o release APK e exporta uma copia com nome versionado."
    dependsOn("assembleRelease", "exportVersionedReleaseApk")
}

tasks.register("bundleVersionedRelease") {
    group = "build"
    description = "Gera o release AAB (padrao Google Play) e exporta uma copia com nome versionado."
    dependsOn("bundleRelease", "exportVersionedReleaseBundle")
}

tasks.register("releasePlayReady") {
    group = "build"
    description = "Gera APK versionado (teste) e AAB versionado (Google Play)."
    dependsOn("assembleVersionedRelease", "bundleVersionedRelease")
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
