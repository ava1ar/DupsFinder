DupsFinder
==========

Search for duplicate files in the specified folder recursively.

Wirtten in Java without additional dependencies. Requires Java 8 and Maven to build. SHA-1 is used as a hashing algo.

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

Duplicates files list is printed to stdout and all other output (first informaion line, summary at the end and all warnings/errors) is printed to stderr.

Implementation details
----------------------

The main idea behind the application is multiple operations of grouping files by set of file properties and droping files with unique property value, so the remaining files in groups have equal property value. File size, hashsum of first 1024 bytes of file and hashsum of the complete file are used as the file properties for grouping. Every grouping operation excludes unique files and pass remaining to the next grouping operation, so at the end only groups of identical files remain.

First version requires Java 7 and up and using ExecutorService for parallel calculations. Never versions require Java 8 and use parallel streams to speedup execution.
