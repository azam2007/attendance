import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.21"
}

group = "uz.m1nex"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://company/com/maven2")
    }
    maven { url = uri("https://jitpack.io") }

}

dependencies {
// https://mvnrepository.com/artifact/io.github.kotlin-telegram-bot.kotlin-telegram-bot/telegram
    implementation("io.github.kotlin-telegram-bot:kotlin-telegram-bot:6.0.7")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}