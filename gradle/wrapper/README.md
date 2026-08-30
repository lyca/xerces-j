# Gradle Wrapper in Apache Xerces-J

Per **Apache Software Foundation (ASF) Release Policy**, executable binary JAR files (`gradle-wrapper.jar`) must not be committed to source code repositories.

Instead, the `gradlew` (Linux/macOS) and `gradlew.bat` (Windows) scripts will automatically download the official `gradle-wrapper.jar` on demand upon the first build invocation and verify its integrity against the official SHA-256 checksum:

* **Gradle Version**: 8.14.5
* **Wrapper JAR SHA-256**: `7d3a4ac4de1c32b59bc6a4eb8ecb8e612ccd0cf1ae1e99f66902da64df296172`
* **Distribution SHA-256**: `6f74b601422d6d6fc4e1f9a1ab6522f642c2fdcbc15ae33ebd30ba3d7198e854`

Official release checksums can be verified at [https://gradle.org/release-checksums/](https://gradle.org/release-checksums/).
