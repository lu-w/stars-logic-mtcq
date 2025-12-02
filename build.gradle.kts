/*
 * STARS MTCQ Integration
 */

plugins {
    kotlin("jvm") version "2.2.0"
}

group = "tools.aqua"
version = "1.0.0"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("tools.aqua:stars-core:1.0")
    implementation("org.antlr:antlr4-runtime:4.13.1") // required by Topllet for parsing MTCQs
    implementation(files("lib/openllet-distribution-2.6.6-SNAPSHOT.jar"))
    implementation("javax.xml.bind:jaxb-api:2.3.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.10")
    implementation("net.sourceforge.owlapi:owlapi-distribution:5.1.20")

	testImplementation(kotlin("test"))
    testImplementation("tools.aqua:stars-data-av:1.0")
}

tasks.test {
    useJUnitPlatform()
}
