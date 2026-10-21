myELH: an editor for entity life history (ELH) diagrams

## Building

    ./gradlew test       # run the automated tests
    ./gradlew run        # start the app
    ./gradlew build      # build/libs/myELH.jar, runs with "java -jar"
    ./gradlew deploy     # copy myELH.jar to ~/applications

The mywidgets library is built from `../mywidgets`, which must be checked out
next to this project.

## Installing

    ./gradlew appImage   # build/jpackage/image/myELH: the app with its own Java
    ./gradlew deb        # build/jpackage/myelh_<version>_amd64.deb

The app image runs from wherever it is put, with `myELH/bin/myELH`; no Java
needs to be installed. The .deb installs it in /opt/myelh and adds myELH to
the Office menu:

    sudo apt install ./build/jpackage/myelh_2.0_amd64.deb

and `sudo apt remove myelh` takes it away again. The version number is set
in build.gradle.kts.
