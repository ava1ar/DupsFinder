DupsFinder
==========

Search for duplicate files in the specified folder recursively. Initially, files are compared by size. For files with identical size, hash sum of first 1024 bytes are calculated and compared, if this sums are equal, hash sum are calculated for files and compared again. Files with identical hash sums are listed as equal.   

Wirtten in Java without additional dependencies. Requires Java7+ and Maven to build. SHA-1 is used as a hashing algo.

Build and run
-------------

Checkout the source code and navigate to the project folder (directory with pom.xml file)

To build the jar file execute

***mvn install***

Jar file will be built and stored in the target directory.

To run execute

***java -jar target/dups-0.0.1-SNAPSHOT.jar [directory]***

If [directory] is not provided, current directory is used.
