import net.minecraftforge.gradle.common.util.RunConfig
import wtf.gofancy.fancygradle.script.extensions.deobf
import java.time.LocalDateTime

plugins {
    java
    id("net.minecraftforge.gradle") version "[6.0,6.2)"
    id("org.parchmentmc.librarian.forgegradle") version "1.+"
    id("wtf.gofancy.fancygradle") version "1.1.+"
    id("wtf.gofancy.koremods.gradle") version "0.2.0"
}

val versionMc: String by project
val versionForge: String by project

val versionDarkUtils: String by project
val versionCurios: String by project
val versionBookshelf: String by project
val versionRunelic: String by project

group = "com.github.dreamdawn_dev"
version = "1.0.2"

java {
    withSourcesJar()

    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

minecraft {
    mappings("parchment", "2023.08.20-1.20.1")

    accessTransformer(file("src/main/resources/META-INF/accesstransformer.cfg"))

    runs {
        val config = Action<RunConfig> {
            properties(mapOf(
                "forge.logging.console.level" to "debug"
            ))
            workingDirectory = project.file("run").canonicalPath
            source(sourceSets.main.get())
        }

        create("client", config)
        create("server", config)
    }
}

repositories {
    maven {
        name = "BlameJared"
        url = uri("https://maven.blamejared.com")
    }
    maven {
        name = "Curios"
        url = uri("https://maven.theillusivec4.top")
    }
    maven {
        name = "Progwml6 maven"
        url = uri("https://dvs1.progwml6.com/files/maven/")
    }
    maven {
        name = "Garden of Fancy Releases"
        url = uri("https://maven.gofancy.wtf/releases")
    }
    mavenLocal()
}

dependencies {
    minecraft("net.minecraftforge:forge:$versionMc-$versionForge")

    koremods(group = "wtf.gofancy.koremods", name = "koremods-modlauncher", version = "0.7.0")

    compileOnly(fg.deobf(group = "top.theillusivec4.curios", name = "curios-forge", version = versionCurios))
}

tasks {
    jar {
        manifest {
            attributes(
                "Specification-Title" to "Somnia Awoken Reforged",
                "Specification-Vendor" to "Dreamdawn",
                "Specification-Version" to 1,
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to "Dreamdawn",
                "Implementation-Timestamp" to LocalDateTime.now()
            )
        }
    }
}