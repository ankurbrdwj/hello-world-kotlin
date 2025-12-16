plugins {
    kotlin("jvm") version "1.9.22"
    application
}

group = "com.stockmarket"
version = "1.0.0"

repositories {
    mavenCentral()
}

val ktorVersion = "1.6.8"

dependencies {
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-websockets:$ktorVersion")
    implementation("org.json:json:20231013")
    implementation("com.github.ajalt.clikt:clikt:4.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("HelloKt")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "HelloKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
