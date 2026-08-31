# Apache Xerces-J JMH Microbenchmarks

This module provides an isolated, comprehensive [JMH (Java Microbenchmark Harness)](https://openjdk.org/projects/code-tools/jmh/) suite for Apache Xerces-J.

## Overview of Benchmarks

* **`SaxParserBenchmark`**: SAX parsing performance comparison across Xerces Repo, JDK internal SAX, FasterXML Woodstox, and FasterXML Aalto.
* **`StaxParserBenchmark`**: StAX (`XMLStreamReader`) pull-parsing throughput across Xerces StAX, FasterXML Woodstox, FasterXML Aalto, and JDK internal StAX.
* **`DomParserBenchmark`**: DOM parsing throughput comparing Xerces deferred (`defer-node-expansion=true`) vs eager (`defer-node-expansion=false`) mode alongside JDK DOM.
* **`DomTraversalBenchmark`**: Full DOM parsing + DOM tree traversal using recursive DOM walk, `TreeWalker`, and `NodeIterator`.
* **`SchemaValidationBenchmark`**: W3C XML Schema (XSD) compilation and validation throughput using `XMLSchemaFactory`, `Validator`, and grammar caching.

## Running Benchmarks

### Quick Run (All Benchmarks, fast iterations)
```bash
./gradlew :xerces-benchmarks:jmh -PjmhArgs="-f 1 -wi 2 -i 3 -r 500ms -w 500ms"
```

### Run Specific Benchmark Categories
```bash
# SAX Benchmarks only
./gradlew :xerces-benchmarks:jmh -PjmhArgs=".*SaxParserBenchmark.*"

# StAX Benchmarks only
./gradlew :xerces-benchmarks:jmh -PjmhArgs=".*StaxParserBenchmark.*"

# DOM & Traversal Benchmarks only
./gradlew :xerces-benchmarks:jmh -PjmhArgs=".*Dom.*"

# Schema Validation Benchmarks only
./gradlew :xerces-benchmarks:jmh -PjmhArgs=".*SchemaValidationBenchmark.*"
```

### Parameterized Runs (Workload Profiles & Sizes)
```bash
# Test with attribute-heavy profile and 500 items
./gradlew :xerces-benchmarks:jmh -PjmhArgs="-p profile=ATTRIBUTE_HEAVY -p itemCount=500 .*SaxParserBenchmark.*"

# Test with deep nesting profile
./gradlew :xerces-benchmarks:jmh -PjmhArgs="-p profile=DEEP_NESTING -p itemCount=100 .*DomParserBenchmark.*"
```

### GC & Allocation Profiling
```bash
./gradlew :xerces-benchmarks:jmh -PjmhArgs="-prof gc .*SaxParserBenchmark.*"
```

### Exporting Results to JSON or CSV
```bash
./gradlew :xerces-benchmarks:jmh -PjmhResultFormat=json -PjmhResultFile=build/jmh-results.json
```
