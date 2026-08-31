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

fun readConfigValue(vararg keys: String): String {
    keys.forEach { key ->
        val envValue = System.getenv(key)
        if (!envValue.isNullOrBlank()) return envValue

        val propertyValue = localProperties.getProperty(key)
        if (!propertyValue.isNullOrBlank()) return propertyValue

        val envFileValue = rootEnvProperties.getProperty(key)
        if (!envFileValue.isNullOrBlank()) return envFileValue
    }
    return ""
}

fun escapeBuildConfig(value: String): String = value
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

fun quotedBuildConfig(value: String): String = "\"${escapeBuildConfig(value)}\""

// Configuracao cliente. Service-role, CRON_SECRET e segredos de sincronizacao
// nunca sao incluidos no APK.
val supabaseUrl = readConfigValue("VITE_SUPABASE_URL", "SUPABASE_URL", "PRIMARY_SUPABASE_URL")
val supabaseAnonKey = readConfigValue("VITE_SUPABASE_ANON_KEY", "SUPABASE_ANON_KEY")
val dashboardUrl = readConfigValue("VITE_DASHBOARD_URL", "DASHBOARD_URL")
    .trim()
    .ifBlank { supabaseUrl }
val dashboardAnonKey = readConfigValue("VITE_DASHBOARD_ANON_KEY", "DASHBOARD_ANON_KEY")
    .trim()
    .ifBlank { supabaseAnonKey }
val cepApiUrl = readConfigValue("VITE_CEP_API_URL", "CEP_API_URL")
val nominatimProxyUrl = readConfigValue("VITE_NOMINATIM_PROXY_URL", "NOMINATIM_PROXY_URL")
val odontoartProxyUrl = readConfigValue("VITE_ODONTOART_PROXY_URL", "ODONTOART_PROXY_URL")
val odontoartClientToken = readConfigValue("VITE_ODONTOART_TOKEN")

val configuredWebRepoPath = readConfigValue("ODONTOART_WEB_REPO_PATH").trim()
val webReferenceCandidates = buildList {
    if (configuredWebRepoPath.isNotBlank()) add(file(configuredWebRepoPath))
    add(rootProject.file("../web-reference"))
    add(rootProject.file("../../Odontoart-rotas"))
}
val webReferenceDir = webReferenceCandidates.firstOrNull { it.resolve("package.json").isFile }
    ?: webReferenceCandidates.first()
val webRuntimeDir = layout.buildDirectory.dir("web-runtime")
val generatedWebEnvFile = layout.buildDirectory.file("generated/web-runtime/.env.local")
val npmExecutable = if (System.getProperty("os.name").lowercase().contains("windows")) "npm.cmd" else "npm"

val appVersionName = (project.findProperty("ODONTOART_APP_VERSION_NAME") as? String)
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?: "1.0.0"
val appVersionCode = (project.findProperty("ODONTOART_APP_VERSION_CODE") as? String)
    ?.trim()
    ?.toIntOrNull()
    ?: 1

val defaultUpdateBaseUrl = "https://odontoart.com/rotas/updates"
val updateBaseUrl = readConfigValue("ROTAS_UPDATE_BASE_URL")
    .trim()
    .ifBlank { defaultUpdateBaseUrl }
    .removeSuffix("/")
val updateMetadataUrl = readConfigValue("ROTAS_UPDATE_METADATA_URL")
    .trim()
    .ifBlank { "$updateBaseUrl/android-update.json" }
val directApkFileName = "odontoart-rotas-direct-v${appVersionName}-b${appVersionCode}.apk"
val directApkUrl = "$updateBaseUrl/$directApkFileName"

android {
    namespace = "com.odontoart.rotas"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.odontoart.rotas"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
        buildConfigField("String", "SUPABASE_URL", quotedBuildConfig(supabaseUrl))
        buildConfigField("String", "SUPABASE_ANON_KEY", quotedBuildConfig(supabaseAnonKey))
        buildConfigField("String", "DASHBOARD_URL", quotedBuildConfig(dashboardUrl))
        buildConfigField("String", "DASHBOARD_ANON_KEY", quotedBuildConfig(dashboardAnonKey))
        buildConfigField("String", "CEP_API_URL", quotedBuildConfig(cepApiUrl))
        buildConfigField("String", "NOMINATIM_PROXY_URL", quotedBuildConfig(nominatimProxyUrl))
        buildConfigField("String", "ODONTOART_PROXY_URL", quotedBuildConfig(odontoartProxyUrl))
        buildConfigField("String", "UPDATE_METADATA_URL", quotedBuildConfig(""))
        buildConfigField("String", "UPDATE_APK_URL", quotedBuildConfig(""))
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

        create("direct") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            buildConfigField("String", "UPDATE_METADATA_URL", quotedBuildConfig(updateMetadataUrl))
            buildConfigField("String", "UPDATE_APK_URL", quotedBuildConfig(directApkUrl))
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

val generateWebClientEnv = tasks.register("generateWebClientEnv") {
    outputs.file(generatedWebEnvFile)
    doLast {
        val target = generatedWebEnvFile.get().asFile
        target.parentFile.mkdirs()
        val values = linkedMapOf(
            "VITE_SUPABASE_URL" to supabaseUrl,
            "VITE_SUPABASE_ANON_KEY" to supabaseAnonKey,
            "VITE_CEP_API_URL" to cepApiUrl,
            "VITE_NOMINATIM_PROXY_URL" to nominatimProxyUrl,
            "VITE_ODONTOART_PROXY_URL" to odontoartProxyUrl,
            "VITE_ODONTOART_TOKEN" to odontoartClientToken,
            "VITE_DASHBOARD_URL" to dashboardUrl,
            "VITE_DASHBOARD_ANON_KEY" to dashboardAnonKey,
        )
        target.writeText(
            values.entries.joinToString(System.lineSeparator()) { (key, value) ->
                "$key=${value.replace("\r", "").replace("\n", "")}" 
            } + System.lineSeparator(),
        )
    }
}

val prepareWebRuntimeSource = tasks.register<Sync>("prepareWebRuntimeSource") {
    group = "build setup"
    description = "Copia o Odontoart-rotas para uma area temporaria sem alterar o repositorio web."
    dependsOn(generateWebClientEnv)
    doFirst {
        if (!webReferenceDir.resolve("package.json").isFile) {
            throw GradleException(
                "Repositorio web Odontoart-rotas nao encontrado. " +
                    "Mantenha-o ao lado de rotas-mobile ou configure ODONTOART_WEB_REPO_PATH.",
            )
        }
    }
    from(webReferenceDir) {
        include("package.json")
        include("package-lock.json")
        include("index.html")
        include("vite.config.*")
        include("tsconfig*.json")
        include("postcss.config.*")
        include("tailwind.config.*")
        include("src/**")
        include("public/**")
    }
    from(generatedWebEnvFile) {
        rename { ".env.local" }
    }
    into(webRuntimeDir)
    includeEmptyDirs = false
}

val installWebRuntimeDependencies = tasks.register<Exec>("installWebRuntimeDependencies") {
    group = "build setup"
    description = "Instala as dependencias da copia temporaria do Odontoart-rotas."
    dependsOn(prepareWebRuntimeSource)
    workingDir(webRuntimeDir.get().asFile)
    commandLine(npmExecutable, "ci", "--no-audit", "--no-fund")
}

val buildWebRuntime = tasks.register<Exec>("buildWebRuntime") {
    group = "build"
    description = "Compila o mesmo React/CSS/TypeScript do Odontoart-rotas para o APK."
    dependsOn(installWebRuntimeDependencies)
    workingDir(webRuntimeDir.get().asFile)
    environment("NODE_OPTIONS", "--max-old-space-size=4096")
    commandLine(npmExecutable, "run", "build")
}

val syncWebRuntimeAssets = tasks.register<Sync>("syncWebRuntimeAssets") {
    group = "build setup"
    description = "Empacota o dist do Odontoart-rotas como assets do Android."
    dependsOn(buildWebRuntime)
    from(webRuntimeDir.map { it.dir("dist") })
    into(layout.projectDirectory.dir("src/main/assets"))
    includeEmptyDirs = false
}

tasks.named("preBuild") {
    dependsOn(syncWebRuntimeAssets)
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

tasks.register<Copy>("exportDirectApk") {
    dependsOn("assembleDirect")
    from(layout.buildDirectory.dir("outputs/apk/direct")) {
        include("*.apk")
        exclude("*.idsig")
    }
    into(layout.buildDirectory.dir("outputs/direct-distribution"))
    rename { directApkFileName }
}

tasks.register("generateDirectDistributionFiles") {
    doLast {
        val outputDir = layout.buildDirectory.dir("outputs/direct-distribution").get().asFile
        outputDir.mkdirs()

        val json = """
            {
              "versionCode": $appVersionCode,
              "versionName": "$appVersionName",
              "apkUrl": "$directApkUrl",
              "notes": "Atualizacao da versao $appVersionName"
            }
        """.trimIndent() + System.lineSeparator()

        val html = """
            <!doctype html>
            <html lang="pt-BR">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Odontoart Rotas - Android</title>
              <style>
                :root { color-scheme: light; font-family: Arial, sans-serif; }
                body { margin: 0; background: #f5f7f6; color: #183227; }
                main { max-width: 680px; margin: 0 auto; padding: 48px 20px; }
                .card { background: #fff; border-radius: 20px; padding: 28px; box-shadow: 0 12px 36px rgba(0,0,0,.08); }
                h1 { margin-top: 0; color: #0c6f3d; }
                .version { color: #587066; margin-bottom: 24px; }
                .button { display: inline-block; background: #0c6f3d; color: white; text-decoration: none; padding: 14px 20px; border-radius: 12px; font-weight: 700; }
                .hint { margin-top: 24px; font-size: 14px; color: #6c7d75; line-height: 1.5; }
              </style>
            </head>
            <body>
              <main>
                <section class="card">
                  <h1>Odontoart Rotas</h1>
                  <p class="version">Versao $appVersionName · build $appVersionCode</p>
                  <p>Baixe a versao Android de distribuicao interna.</p>
                  <a class="button" href="$directApkUrl">Baixar APK</a>
                  <p class="hint">Ao instalar pela primeira vez, o Android pode solicitar autorizacao para instalar aplicativos desta fonte. As proximas versoes podem ser detectadas pelo proprio aplicativo.</p>
                </section>
              </main>
            </body>
            </html>
        """.trimIndent() + System.lineSeparator()

        file("${outputDir.absolutePath}/android-update.json").writeText(json)
        file("${outputDir.absolutePath}/android-update-v$appVersionName.json").writeText(json)
        file("${outputDir.absolutePath}/index.html").writeText(html)
    }
}

tasks.register("directDistributionReady") {
    group = "build"
    description = "Gera APK direct, android-update.json e pagina HTML para hospedagem publica."
    dependsOn("exportDirectApk", "generateDirectDistributionFiles")
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
    description = "Gera APK versionado (teste) e AAB versionado (Google Play), sem autoatualizacao direta."
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
    implementation("androidx.webkit:webkit:1.11.0")
}
