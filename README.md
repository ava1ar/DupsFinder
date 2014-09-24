DupsFinder
==========

Search for duplicate files in the specified folder recursively. Goal of the project is to **create a fastest possible Java implementation** while keeping code clean and easy to read. 

Wirtten in Java without additional dependencies. Requires Java 7 and Maven to build. SHA-1 is used as a hashing algo.

Build and run
-------------

Checkout the source code and navigate to the project folder (directory with pom.xml file)

To build the jar file execute

***mvn package***

Jar file will be built and stored in the target directory.

To run execute

***java -jar target/dupsfinder-0.0.1-SNAPSHOT.jar [directory]***

If [directory] is not provided, current directory is used.

Output format
-------------

{SHA-1 sum}:{number of duplicates}:{file size in bytes}:{file full path}

Duplicates files list is printed to stdout and all other output (first informaion line, summary at the end and all warnings/errors) is printed to stderr, so required part of the output can be easily filtered by output redirection.

Implementation details
----------------------

The main idea behind the application is multiple operations of grouping files by set of file properties and droping files with unique property value, so the remaining files in groups have equal property value. File size, hashsum of first 1024 bytes of file and hashsum of the complete file are used as the file properties for grouping. Every grouping operation excludes unique files and pass remaining to the next grouping operation, so at the end only groups of identical files remain.

Branches
--------
* [**master**](https://github.com/ava1ar/DupsFinder/tree/master) - main development branch for Java8-based implementation. Actively uses Java8-specific features (streams, lambdas) and require Java8+ to run.
* [**java7**](https://github.com/ava1ar/DupsFinder/tree/java7) - branch for Java7-based implementation. Uses ExecutorService for parallel execution and parallel filetree walker based on Fork/Join java framework. Requires Java7+ to run.
* [**java6**](https://github.com/ava1ar/DupsFinder/tree/java6) - branch for Java6-based implementation. Uses ExecutorService for parallel execution. Requires Java6+ to run.
