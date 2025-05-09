plugins {
    kotlin("jvm")
    application
}

repositories {
    mavenCentral()
}

val imguiVersion = "1.89.0"
val glmVersion = "0.9.9.1-12"
val koolVersion = "0.9.79"
val fleksVersion = "2.11"
val ktxVersion = "1.13.1-rc1"

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":Rune"))

    // imgui
    implementation("io.github.spair:imgui-java-app:${imguiVersion}")

    // glm
    implementation("io.github.kotlin-graphics:glm:${glmVersion}")
    implementation("io.github.kotlin-graphics:kool:${koolVersion}")

    // fleks
    implementation("io.github.quillraven.fleks:Fleks:$fleksVersion")

    // Scripting TODO: get the kotlin version instead of hard coding it
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:2.1.0")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:2.1.0")
    implementation("org.jetbrains.kotlin:kotlin-scripting-common:2.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    // Physics  TODO: dont expose this to the client
    implementation("io.github.libktx:ktx-box2d:$ktxVersion")

    implementation(project(":Rune"))
}

sourceSets {
    main {
        kotlin {
            setSrcDirs(listOf("src"))
        }

        resources.srcDir("scripts")
    }
}

application {
    mainClass.set("MainKt")
}
