plugins {
    java
    application
}

group = "com.jrts"
version = "0.1.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
    maven { url = uri("https://jmonkeyengine.github.io/maven") }
}

val jmeVersion = "3.6.1-stable"

dependencies {
    implementation("org.jmonkeyengine:jme3-core:$jmeVersion")
    implementation("org.jmonkeyengine:jme3-desktop:$jmeVersion")
    implementation("org.jmonkeyengine:jme3-lwjgl3:$jmeVersion")
    implementation("org.jmonkeyengine:jme3-plugins:$jmeVersion")
    implementation("org.jmonkeyengine:jme3-effects:$jmeVersion")
    implementation("org.jmonkeyengine:jme3-niftygui:$jmeVersion")

    implementation("com.moandjiezana.toml:toml4j:0.7.2")
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.7.0")
}

application {
    mainClass.set("com.jrts.Main")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.register<JavaExec>("previewModel") {
    mainClass.set("com.jrts.tools.preview.UnitPreviewApp")
    classpath = sourceSets["main"].runtimeClasspath
    args = project.findProperty("modelPath")?.toString()?.let { listOf(it) } ?: emptyList()
}

tasks.register<JavaExec>("importModels") {
    mainClass.set("com.jrts.tools.importer.BatchImporter")
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf(
        "--source", file("assets/blender").absolutePath,
        "--intermediate", file("assets/models/intermediate").absolutePath,
        "--output", file("assets/models/final").absolutePath
    )
}
