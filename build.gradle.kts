// Build description for myELH.
//
// Everyday commands (run from the project folder):
//   ./gradlew run       compile and start the app
//   ./gradlew build     compile and build the single runnable jar
//   ./gradlew deploy    build it and copy it to ~/applications/myELH.jar
//   ./gradlew dependencies --configuration runtimeClasspath
//                       list every library, including the ones pulled in
//                       automatically by the libraries listed below

plugins {
    // "application" adds the java plugin (compile, jar) plus the run task
    application
}

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
}

// Each line is "group:name:version", as listed on https://central.sonatype.com
// Upgrading a library means changing its version number here.
dependencies {
    // built from ../mywidgets (see includeBuild in settings.gradle.kts);
    // it brings the FlatLaf look and feel and MigLayout with it
    implementation("org.lerot:mywidgets")

    implementation("org.jdom:jdom:1.1.3")                         // reading .elh files
    implementation("org.apache.xmlgraphics:batik-svggen:1.19")    // drawing the diagram as SVG
    implementation("org.apache.xmlgraphics:batik-swing:1.19")     // showing the SVG on screen
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // list each use of a deprecated (outdated) method, not just a summary
    options.compilerArgs.add("-Xlint:deprecation")
}

application {
    mainClass = "org.lerot.myELH.myELHgui"
}

// Build one self-contained jar (the app plus every library unpacked into
// it), like the IntelliJ artifact did, so it runs with "java -jar".
tasks.jar {
    archiveFileName = "myELH.jar"
    manifest {
        attributes["Main-Class"] = application.mainClass
    }
    val runtimeJars = configurations.runtimeClasspath
    dependsOn(runtimeJars)
    from({ runtimeJars.get().filter { it.name.endsWith(".jar") }.map { zipTree(it) } })
    // signature files from signed libraries would make the merged jar
    // fail its signature check, so leave them out
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/*.EC")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.register<Copy>("deploy") {
    group = "distribution"
    description = "Builds myELH.jar and copies it to ~/applications"
    from(tasks.jar)
    into(layout.dir(providers.systemProperty("user.home").map { File(it, "applications") }))
}
