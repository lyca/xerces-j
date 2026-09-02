# Apache Xerces-J (Xerces2 Java XML Parser)

Apache Xerces-J is a high-performance, fully compliant XML parser written in Java, supporting XML 1.0 (4th & 5th Edition), Namespaces 1.0/1.1, DOM Level 2 & Level 3, SAX 2.0.2, JAXP 1.4, and W3C XML Schema 1.0.

---

## Build Prerequisites

* **Java Development Kit (JDK)**: Version 8 or higher (Xerces-J targets Java 8 bytecode compatibility).
* **Build System**: [Maven Wrapper](https://maven.apache.org/) is included (`./mvnw` on Unix/macOS, `mvnw.cmd` on Windows). No separate Maven installation is required.

---

## Building and Testing

### 1. Build and Run All Tests
To compile all modules, run unit tests, and assemble the JARs:

```bash
./mvnw clean install
```

### 2. Run Tests Only
To run the full JUnit 5 test suite:

```bash
./mvnw clean test
```

### 3. Generate Javadocs
To generate the API Javadocs under `target/site/apidocs/`:

```bash
./mvnw javadoc:javadoc
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
