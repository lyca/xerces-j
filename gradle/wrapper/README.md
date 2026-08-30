# Gradle Wrapper in Apache Xerces-J

Per **Apache Software Foundation (ASF) Release Policy**, executable binary JAR files (`gradle-wrapper.jar`) must not be committed to source code repositories.

Instead, the `gradlew` (Linux/macOS) and `gradlew.bat` (Windows) scripts will automatically download the official `gradle-wrapper.jar` on demand upon the first build invocation and verify its integrity against the official SHA-256 checksum:

* **Gradle Version**: 9.7.1
* **Wrapper JAR SHA-256**: `7a9ce74cff467ca1bf60a4fcd9f05185acceda4d0f382434d393e17864262c5d`
* **Distribution SHA-256**: `acd53f1edaf02f1a8ff99879f8a34b302661a057d9b063ae9e35b552f804d20a`

Official release checksums can be verified at [https://gradle.org/release-checksums/](https://gradle.org/release-checksums/).
