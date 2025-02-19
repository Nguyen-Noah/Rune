plugins {
    kotlin("jvm")
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":Rune"))
}

sourceSets {
    main {
        kotlin {
            setSrcDirs(listOf("src"))
        }
    }
}

application {
    mainClass.set("MainKt")
}
