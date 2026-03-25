plugins {
    kotlin("jvm")
    application
}

version = "1.0-SNAPSHOT"
val fleksVersion = "2.11"
val glmVersion = "0.9.9.1-12"
val koolVersion = "0.9.79"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":Rune"))

    // glm
    implementation("io.github.kotlin-graphics:glm:${glmVersion}")
    implementation("io.github.kotlin-graphics:kool:${koolVersion}")

    // fleks
    implementation("io.github.quillraven.fleks:Fleks:$fleksVersion")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("sandbox.SandboxKt")
}
