// Build description for myELH.
//
// Everyday commands (run from the project folder):
//   ./gradlew test      run the automated tests (test/java)
//   ./gradlew run       compile and start the app
//   ./gradlew build     compile and build the single runnable jar
//   ./gradlew deploy    build it and copy it to ~/applications/myELH.jar
//   ./gradlew appImage  build build/jpackage/image/myELH, a folder holding
//                       the app with its own Java; start it with bin/myELH
//   ./gradlew deb       build build/jpackage/myelh_<version>_amd64.deb;
//                       install it with "sudo apt install ./<that file>"
//   ./gradlew dependencies --configuration runtimeClasspath
//                       list every library, including the ones pulled in
//                       automatically by the libraries listed below

plugins {
    // "application" adds the java plugin (compile, jar) plus the run task
    application
}

// shown in Help > About and given to the installer
version = "2.0"

java {
    // compile for Java 21, whatever Java version runs Gradle
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// Libraries are downloaded from Maven Central and cached in ~/.gradle
repositories {
    mavenCentral()
}

// The source lives directly in src/ rather than Gradle's usual
// src/main/java, so say so here.
sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        // the window icon, put in the jar alongside the classes
        resources.setSrcDirs(listOf("resources"))
    }
    // automated tests (./gradlew test)
    test {
        java.setSrcDirs(listOf("test/java"))
        resources.setSrcDirs(emptyList<String>())
    }
}

// Each line is "group:name:version", as listed on https://central.sonatype.com
// Upgrading a library means changing its version number here.
dependencies {
    // built from ../mywidgets (see includeBuild in settings.gradle.kts);
    // it brings the FlatLaf look and feel and MigLayout with it
    implementation("org.lerot:mywidgets")

    implementation("org.jdom:jdom2:2.0.6.1")                      // reading and writing .elh files
    implementation("org.apache.xmlgraphics:batik-svggen:1.19")    // Export > Print SVG
    implementation("org.apache.xmlgraphics:batik-dom:1.19")       // the SVG document it writes

    // JUnit, for the tests only; it is not put in myELH.jar
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    // show each test's result, and the output of failing tests
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // list each use of a deprecated (outdated) method, not just a summary
    options.compilerArgs.add("-Xlint:deprecation")
}

application {
    mainClass = "org.lerot.myELH.MyElh"
}

// Build one self-contained jar (the app plus every library unpacked into
// it), like the IntelliJ artifact did, so it runs with "java -jar".
tasks.jar {
    archiveFileName = "myELH.jar"
    manifest {
        attributes["Main-Class"] = application.mainClass
        attributes["Implementation-Version"] = project.version
    }
    val runtimeJars = configurations.runtimeClasspath
    dependsOn(runtimeJars)
    from({ runtimeJars.get().filter { it.name.endsWith(".jar") }.map { zipTree(it) } })
    // signature files from signed libraries would make the merged jar
    // fail its signature check, so leave them out
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/*.EC")
    // mywidgets' module description; the merged jar is not a module
    exclude("module-info.class")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.register<Copy>("deploy") {
    group = "distribution"
    description = "Builds myELH.jar and copies it to ~/applications"
    from(tasks.jar)
    into(layout.dir(providers.systemProperty("user.home").map { File(it, "applications") }))
}

// Packaging with jpackage, from the same Java 21 the build compiles with.
// The app gets its own cut-down Java holding just the modules it uses
// (found with jdeps; jdk.unsupported is used by FlatLaf).
val jpackage = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(21) }
    .map { it.metadata.installationPath.file("bin/jpackage").asFile.path }
val jpackageDir = layout.buildDirectory.dir("jpackage")
val appVersion = version.toString()

val jpackageInput = tasks.register<Sync>("jpackageInput") {
    from(tasks.jar)
    into(jpackageDir.map { it.dir("input") })
}

tasks.register<Exec>("appImage") {
    group = "distribution"
    description = "Builds build/jpackage/image/myELH, the app with its own Java"
    dependsOn(jpackageInput)
    val input = jpackageDir.map { it.dir("input") }
    val output = jpackageDir.map { it.dir("image") }
    val icon = layout.projectDirectory.file("resources/elh.png")
    inputs.dir(input)
    inputs.file(icon)
    outputs.dir(output)
    // jpackage will not overwrite an earlier image
    doFirst { output.get().asFile.deleteRecursively() }
    executable(jpackage.get())
    args(
        "--type", "app-image",
        "--name", "myELH",
        "--app-version", appVersion,
        "--input", input.get().asFile.path,
        "--main-jar", "myELH.jar",
        "--main-class", application.mainClass.get(),
        "--icon", icon.asFile.path,
        "--add-modules", "java.base,java.desktop,java.logging,jdk.unsupported",
        "--jlink-options", "--strip-debug --no-man-pages --no-header-files",
        "--dest", output.get().asFile.path,
    )
}

tasks.register<Exec>("deb") {
    group = "distribution"
    description = "Builds a .deb installer for Debian/Ubuntu from the app image"
    dependsOn("appImage")
    val image = jpackageDir.map { it.dir("image/myELH") }
    val output = jpackageDir
    inputs.dir(image)
    outputs.file(jpackageDir.map { it.file("myelh_${appVersion}_amd64.deb") })
    executable(jpackage.get())
    args(
        "--type", "deb",
        "--name", "myELH",
        "--app-version", appVersion,
        "--app-image", image.get().asFile.path,
        "--description", "Entity life history editor",
        "--vendor", "lerot",
        "--linux-package-name", "myelh",
        "--linux-menu-group", "Office",
        "--linux-shortcut",
        "--dest", output.get().asFile.path,
    )
}
