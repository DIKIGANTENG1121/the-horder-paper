plugins {
    java
}

group = "com.geminineon.neontestkit"
version = rootProject.property("projectVersion") as String

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${rootProject.property("paperApiVersion")}")
    compileOnly(rootProject)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of((rootProject.property("javaVersion") as String).toInt()))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set((rootProject.property("javaVersion") as String).toInt())
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}
