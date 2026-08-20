plugins {
    java
}

group = "io.github.georgetimbershift"
version = "1.2.1"
val pluginVersion = version.toString()

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
        name = "spigotmc-snapshots"
    }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:26.2-R0.1-SNAPSHOT")

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 25
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing"))
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand("version" to pluginVersion)
    }
}

tasks.jar {
    archiveFileName = "TimberShift-${project.version}.jar"
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.test {
    useJUnitPlatform()
}
