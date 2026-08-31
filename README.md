# Apache Xerces-J (Xerces2 Java XML Parser)

Apache Xerces-J is a high-performance, fully compliant XML parser written in Java, supporting XML 1.0 (4th & 5th Edition), Namespaces 1.0/1.1, DOM Level 2 & Level 3, SAX 2.0.2, JAXP 1.4, and W3C XML Schema 1.0.

---

## Build Prerequisites

* **Java Development Kit (JDK)**: Version 8 or higher (Xerces-J targets Java 8 bytecode compatibility).
* **Build System**: [Gradle Wrapper](https://gradle.org/) is included (`./gradlew` on Unix/macOS, `gradlew.bat` on Windows). No separate Ant or Gradle installation is required.

---

## Building and Testing

### 1. Build and Run All Tests
To compile all modules, run unit tests, and assemble the JARs:

```bash
./gradlew build
```

### 2. Run Tests Only
To run the full JUnit 5 test suite:

```bash
./gradlew test
```

### 3. Run W3C XML Conformance Test Suite
To download and execute the official W3C XML Conformance Test Suite:

```bash
./gradlew :xerces-impl:conformanceTest
```

### 4. Generate Documentation & Javadocs
To generate the static HTML documentation and API Javadocs under `build/docs/`:

```bash
./gradlew docs
```

### 5. Build Release Distributions
To package ASF-compliant source and binary distribution archives (`.zip`, `.tar.gz`) along with their cryptographic **SHA-512** checksums into `build/distributions/`:

```bash
./gradlew dist
```

---

## Project Structure

This repository uses a standard Gradle multi-module architecture:

* **`xerces-impl/`**: The core parser implementation (`xercesImpl.jar`, `xerces-dtd.jar`).
* **`xerces-samples/`**: Sample applications and CLI utilities (`xercesSamples.jar`).
* **`xerces-tools/`**: Build utilities and helpers.
* **`docs/`**: Source XML files and XSLT stylesheets for generating the official documentation.
* **`data/`**: Sample XML and schema files.

---

## Licensing

Apache Xerces-J is open-source software licensed under the [Apache License, Version 2.0](LICENSE).  
W3C DOM specifications and documentation are subject to the [W3C Software Notice](LICENSE.DOM-software.html) and [W3C Document Notice](LICENSE.DOM-documentation.html).

---

## Acknowledgments

We use the [JProfiler](https://www.ej-technologies.com/jprofiler) tool for Java software run-time analysis and optimization of Xerces-J.

Sincerely,<br>
**The Apache Xerces Team**
