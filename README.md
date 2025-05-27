Welcome to univocity-parsers
============================

univocity-parsers is a collection of extremely fast and reliable parsers for Java. It provides a consistent interface for handling different file formats,
and a solid framework for the development of new parsers.

**Bugs, contributions & support**

If you find a bug, please file an issue.

Feel free to submit a contribution via pull requests. Every little bit is appreciated!

**Thank you for using univocity-parsers!**

[Snapshot Repository](https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/com/sonofab1rd/univocity-parsers/)
[Maven Central](https://central.sonatype.com/artifact/com.sonofab1rd/univocity-parsers)
```xml
<dependency>
    <groupId>com.sonofab1rd</groupId>
    <artifactId>univocity-parsers</artifactId>
    <version>2.10.1</version>
</dependency>
```

### Release Workflow
release_number = project version from the pom, minus "-SNAPSHOT"

- create a branch release/[release_number]
- remove the "-SNAPSHOT" from the version number in the pom
- draft a new github release
- create a new tag with release_number
- target the release branch
- give the release a title
- publish release
- delete the release branch

This should start the Publish Release workflow and the artifacts should be published to maven central.
