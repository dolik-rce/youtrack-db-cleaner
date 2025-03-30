val xodusVersion = "3.0.134"

plugins {
    kotlin("jvm") version "2.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
    maven { url = uri("https://packages.jetbrains.team/maven/p/xodus/xodus-daily") }
}

dependencies {
    implementation("org.jetbrains.xodus:xodus-environment:$xodusVersion")
    implementation("org.jetbrains.xodus:xodus-entity-store:$xodusVersion")
    implementation("org.jetbrains.xodus:xodus-openAPI:$xodusVersion")
    implementation("org.jetbrains.xodus:xodus-utils:$xodusVersion")
    implementation("org.jetbrains.xodus:xodus-crypto:$xodusVersion")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")
}

tasks {
    jar {
        enabled = false
    }
}
