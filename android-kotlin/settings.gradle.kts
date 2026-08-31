pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

/*
 * O dist do Odontoart-rotas so existe depois que buildWebRuntime termina.
 * O Sync declarado no modulo app pode ser avaliado antes disso e ficar NO-SOURCE.
 * Esta tarefa tardia garante que o bundle recem-compilado seja efetivamente
 * copiado para os assets antes do preBuild Android continuar.
 */
gradle.afterProject {
    if (path == ":app") {
        val copyWebRuntimeAssetsLate = tasks.register("copyWebRuntimeAssetsLate") {
            group = "build setup"
            description = "Copia o dist web gerado para os assets Android apos o Vite finalizar."
            dependsOn("buildWebRuntime")

            doLast {
                val distDir = layout.buildDirectory.dir("web-runtime/dist").get().asFile
                val assetsDir = layout.projectDirectory.dir("src/main/assets").asFile

                if (!distDir.resolve("index.html").isFile) {
                    throw GradleException(
                        "O build web terminou sem gerar dist/index.html em ${distDir.absolutePath}",
                    )
                }

                if (assetsDir.exists()) {
                    assetsDir.deleteRecursively()
                }
                assetsDir.mkdirs()

                project.copy {
                    from(distDir)
                    into(assetsDir)
                }

                if (!assetsDir.resolve("index.html").isFile || !assetsDir.resolve("assets").isDirectory) {
                    throw GradleException("Falha ao preparar os assets do runtime web para o APK.")
                }
            }
        }

        tasks.named("preBuild").configure {
            dependsOn(copyWebRuntimeAssetsLate)
        }
    }
}

rootProject.name = "OdontoartRotasAndroid"
include(":app")