import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.version

buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    kotlin("jvm") version "1.2.0"
    id("org.jetbrains.kotlin.plugin.noarg").version("1.2.0")
    `maven-publish`
}

group = "com.github.sqlbuilder"
version = "1.7.0"

val sourcesJar by tasks.creating(Jar::class) {
    classifier = "sources"
    from(java.sourceSets["main"].allSource)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenLocal()
    jcenter()
    mavenCentral()
}

noArg {
    annotation("sqlbuilder.meta.Table")
}

dependencies {
    compile(kotlin("stdlib-jre8"))
    compile(kotlin("reflect"))
    compile("org.slf4j:slf4j-api:1.7.7")
    testCompile (kotlin("test"))
    testCompile("com.h2database:h2:1.4.183")
    testCompile("junit:junit:4.11")
    testCompile("org.slf4j:slf4j-simple:1.7.7")
}

publishing {
    (publications) {
        "mavenJava"(MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar)
        }
    }
    repositories {
        maven {
            url = uri("https://api.bintray.com/maven/laurentvanderlinden/maven/sqlbuilder")
            credentials {
                username = if (project.hasProperty("bintray_username")) project.property("bintray_username") as String else ""
                password = if (project.hasProperty("bintray_api_key")) project.property("bintray_api_key") as String else ""
            }
        }
    }
}

defaultTasks = listOf("build")
