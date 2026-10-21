// Gradle reads this file first. It names the project; the build itself is
// described in build.gradle.kts.
rootProject.name = "myELH"

// Build the mywidgets library from its source in the sibling folder, so
// changes to it are picked up straight away (no jar to rebuild and copy).
// The dependency org.lerot:mywidgets in build.gradle.kts resolves to it.
includeBuild("../mywidgets")
