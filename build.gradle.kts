plugins {
    kotlin("jvm") version "2.0.21"
    id("io.ktor.plugin") version "3.0.0"
    id("org.flywaydb.flyway") version "9.2.3"
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
    val kotlinVersion: String by project
    val logbackVersion: String by project

    // ktor
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-config-yaml")

    // exposed
    implementation("org.jetbrains.exposed:exposed-core:0.55.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.55.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.55.0")

    implementation("org.postgresql:postgresql:42.7.4")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    // html parsing
    implementation("org.jsoup:jsoup:1.18.1")

    testImplementation("io.ktor:ktor-server-test-host-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}

// migrations
flyway {
    url = "jdbc:postgresql://localhost:5432/search_engine"
    user = "search_engine"
    password = "search_engine"
    locations = arrayOf("filesystem:src/main/resources/migrations")
    validateOnMigrate = true
    encoding = "UTF-8"
    schemas = arrayOf("public")
}