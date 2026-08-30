# Gradle Wrapper in Apache Xerces-J

Per **Apache Software Foundation (ASF) Release Policy**, executable binary JAR files (`gradle-wrapper.jar`) must not be committed to source code repositories.

Instead, the `gradlew` (Linux/macOS) and `gradlew.bat` (Windows) scripts will automatically download the official `gradle-wrapper.jar` on demand upon the first build invocation and verify its integrity against the official SHA-256 checksum:

* **Gradle Version**: 8.10.2
* **Wrapper JAR SHA-256**: `2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046`
* **Distribution SHA-256**: `31c55713e40233a8303827ceb42ca48a47267a0ad4bab9177123121e71524c26`

Official release checksums can be verified at [https://gradle.org/release-checksums/](https://gradle.org/release-checksums/).
