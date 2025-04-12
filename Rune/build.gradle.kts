
plugins {
    kotlin("jvm")
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
val lwjglNatives = "natives-windows"

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
    implementation("org.lwjgl:lwjgl-fmod")
    implementation("org.lwjgl:lwjgl-freetype")
    implementation("org.lwjgl:lwjgl-glfw")
    implementation("org.lwjgl:lwjgl-harfbuzz")
    implementation("org.lwjgl:lwjgl-hwloc")
    implementation("org.lwjgl:lwjgl-jawt")
    implementation("org.lwjgl:lwjgl-jemalloc")
    implementation("org.lwjgl:lwjgl-ktx")
    implementation("org.lwjgl:lwjgl-libdivide")
    implementation("org.lwjgl:lwjgl-llvm")
    implementation("org.lwjgl:lwjgl-lmdb")
    implementation("org.lwjgl:lwjgl-lz4")
    implementation("org.lwjgl:lwjgl-meow")
    implementation("org.lwjgl:lwjgl-meshoptimizer")
    implementation("org.lwjgl:lwjgl-msdfgen")
    implementation("org.lwjgl:lwjgl-nanovg")
    implementation("org.lwjgl:lwjgl-nfd")
    implementation("org.lwjgl:lwjgl-nuklear")
    implementation("org.lwjgl:lwjgl-odbc")
    implementation("org.lwjgl:lwjgl-openal")
    implementation("org.lwjgl:lwjgl-opencl")
    implementation("org.lwjgl:lwjgl-opengl")
    implementation("org.lwjgl:lwjgl-opengles")
    implementation("org.lwjgl:lwjgl-openvr")
    implementation("org.lwjgl:lwjgl-openxr")
    implementation("org.lwjgl:lwjgl-opus")
    implementation("org.lwjgl:lwjgl-ovr")
    implementation("org.lwjgl:lwjgl-par")
    implementation("org.lwjgl:lwjgl-remotery")
    implementation("org.lwjgl:lwjgl-rpmalloc")
    implementation("org.lwjgl:lwjgl-shaderc")
    implementation("org.lwjgl:lwjgl-spvc")
    implementation("org.lwjgl:lwjgl-sse")
    implementation("org.lwjgl:lwjgl-stb")
    implementation("org.lwjgl:lwjgl-tinyexr")
    implementation("org.lwjgl:lwjgl-tinyfd")
    implementation("org.lwjgl:lwjgl-tootle")
    implementation("org.lwjgl:lwjgl-vma")
    implementation("org.lwjgl:lwjgl-vulkan")
    implementation("org.lwjgl:lwjgl-xxhash")
    implementation("org.lwjgl:lwjgl-yoga")
    implementation("org.lwjgl:lwjgl-zstd")

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
    runtimeOnly("org.lwjgl:lwjgl-freetype::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-glfw::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-harfbuzz::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-hwloc::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-jemalloc::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-ktx::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-libdivide::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-llvm::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-lmdb::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-lz4::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-meow::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-meshoptimizer::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-msdfgen::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-nanovg::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-nfd::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-nuklear::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-openal::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-opengl::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-opengles::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-openvr::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-openxr::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-opus::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-ovr::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-par::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-remotery::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-rpmalloc::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-shaderc::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-spvc::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-sse::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-stb::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-tinyexr::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-tinyfd::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-tootle::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-vma::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-xxhash::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-yoga::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-zstd::$lwjglNatives")

    // JOML
    implementation("org.joml:joml:$jomlVersion")
}

// (Optional) If you’re using a non-standard src folder:
sourceSets {
    main {
        kotlin {
            srcDirs("src")
        }
    }
}
