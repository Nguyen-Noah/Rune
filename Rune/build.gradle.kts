
plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.1.0"
}

repositories {
    mavenCentral()
}

// LWJGL versions (Kotlin DSL style)
val lwjglVersion = "3.3.6"
val jomlVersion = "1.10.7"
val imguiVersion = "1.89.0"
val glmVersion = "0.9.9.1-12"
val koolVersion = "0.9.79"
val fleksVersion = "2.11"
val kamlVersion = "0.77.0"
val lwjglNatives = "natives-windows"
val ktxVersion = "1.13.1-rc1"
val gdxVersion = "1.13.1"
val jsonVersion = "1.6.3"

dependencies {
    // Kotlin stdlib
    implementation(kotlin("stdlib"))

    // Use LWJGL BOM (Bill of Materials) to manage versions for all LWJGL artifacts
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    // LWJGL core + all the modules you need
    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-assimp")
    implementation("org.lwjgl:lwjgl-bgfx")
    implementation("org.lwjgl:lwjgl-cuda")
    implementation("org.lwjgl:lwjgl-egl")
    implementation("org.lwjgl:lwjgl-glfw")
    implementation("org.lwjgl:lwjgl-jawt")
    implementation("org.lwjgl:lwjgl-ktx")
    implementation("org.lwjgl:lwjgl-nfd")
    implementation("org.lwjgl:lwjgl-odbc")
    implementation("org.lwjgl:lwjgl-openal")
    implementation("org.lwjgl:lwjgl-opencl")
    implementation("org.lwjgl:lwjgl-opengl")
    implementation("org.lwjgl:lwjgl-opengles")
    implementation("org.lwjgl:lwjgl-shaderc")
    implementation("org.lwjgl:lwjgl-spvc")
    implementation("org.lwjgl:lwjgl-stb")
    implementation("org.lwjgl:lwjgl-vulkan")

    // imgui
    implementation("io.github.spair:imgui-java-app:${imguiVersion}")

    // glm
    implementation("io.github.kotlin-graphics:glm:$glmVersion")
    implementation("io.github.kotlin-graphics:kool:$koolVersion")

    // fleks
    implementation("io.github.quillraven.fleks:Fleks:$fleksVersion")

    // Natives (Windows in this example). If you need other OS natives, add them similarly.
    runtimeOnly("org.lwjgl:lwjgl::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-assimp::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-bgfx::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-glfw::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-harfbuzz::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-ktx::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-nfd::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-openal::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-opengl::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-opengles::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-shaderc::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-spvc::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-stb::$lwjglNatives")
    runtimeOnly("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-desktop")

    // JOML
    implementation("org.joml:joml:$jomlVersion")

    // Reflection
    implementation(kotlin("reflect"))

    // Scripting TODO: get the kotlin version instead of hard coding it
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:2.1.0")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:2.1.0")
    implementation("org.jetbrains.kotlin:kotlin-scripting-common:2.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$jsonVersion")

    // Physics
    implementation("io.github.libktx:ktx-box2d:$ktxVersion")
    implementation("com.badlogicgames.gdx:gdx-box2d:$gdxVersion")
}

// (Optional) If you’re using a non-standard src folder:
sourceSets {
    main {
        kotlin {
            srcDirs("src")
        }
        resources {
            srcDir("resources")
        }
    }
}
