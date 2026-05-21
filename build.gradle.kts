import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
}

kotlin {
    jvmToolchain(21)
}

group = "me.user"
version = "0.2"

//Klaxon needs at least version 11, Montoya requires 17, JVM Toolchain is 21
java.sourceCompatibility = JavaVersion.VERSION_21
java.targetCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
}


dependencies {
    compileOnly("net.portswigger.burp.extensions:montoya-api:2026.2")
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.beust:klaxon:5.6")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    //Klaxon needs at least version 11, Montoya requires 17, Toolchain is 21
    kotlinOptions.jvmTarget = "21"
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "ch.pentagrid.burpexts.pentagridscancontroller.PentagridScanControllerExtension"
    }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}
