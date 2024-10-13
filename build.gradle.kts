plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "1.8.0"
    id("io.ktor.plugin") version "3.0.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "ru.nstu"
version = "1.0.0"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    val ktorVersion: String by project
    val kotlinVersion: String by project
    val logbackVersion: String by project
    val prometheusVersion: String by project

    // ktor
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-config-yaml")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$prometheusVersion")

    // exposed
    implementation("org.jetbrains.exposed:exposed-core:0.39.2")
    implementation("org.jetbrains.exposed:exposed-dao:0.39.2")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.39.2")

    implementation("org.postgresql:postgresql:42.7.4")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    // html parsing
    implementation("org.jsoup:jsoup:1.18.1")

    testImplementation("io.ktor:ktor-server-test-host-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}

tasks {
    shadowJar {
        archiveBaseName.set("mycrawler")
        archiveVersion.set("")
        manifest {
            attributes(Pair("Main-Class", "ru.nstu.searchengine.ApplicationKt"))
        }
    }
}