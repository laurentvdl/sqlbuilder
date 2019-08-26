import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
    }
}

val kotlinVersion = "1.3.50"

plugins {
    kotlin("jvm") version "1.3.50"
    id("org.jetbrains.kotlin.plugin.noarg").version("1.3.50")
    `maven-publish`
}

group = "com.github.sqlbuilder"
version = "1.15.4"

val sourcesJar by tasks.registering(Jar::class) {
    classifier = "sources"
    from(sourceSets["main"].allSource)
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
    compile(kotlin("stdlib-jdk8", version = kotlinVersion))
    compile(kotlin("reflect", version = kotlinVersion))
    compile("org.slf4j:slf4j-api:1.7.25")
    testCompile (kotlin("test", version = kotlinVersion))
    testCompile("com.h2database:h2:1.4.197")
    testCompile("junit:junit:4.11")
    testCompile("org.slf4j:slf4j-simple:1.7.7")
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar.get())
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

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

defaultTasks = mutableListOf("build")
