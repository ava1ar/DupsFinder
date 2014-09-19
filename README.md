DupsFinder
==========

Search for duplicate files in the specified folder recursively.

Wirtten in Java without additional dependencies. Requires Java7+ and Maven to build. SHA-1 is used as a hashing algo.

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

{SHA-1 sum}:{number of duplicates}:{file size in bytes}:"{file full path}"

Implementation details
----------------------

The main idea behind the application is multiple operations of grouping files by set of file properties and droping files with unique property value, so the remaining files in groups have equal property value. File size, hashsum of first 1024 bytes of file and hashsum of the complete file are used as the file properties for grouping. Every grouping operation excludes unique files and pass remaining to the next grouping operation, so at the end only groups of identical files remain.

During the first step program creates the collection of sets of files with the equal file size. All sets containing only single item (basically files with unique size) are dropped, remaining sets are passed to the second step as an input.

During the second step program creates the collection of sets of file with the equal hashsum of the first of 1024 bytes of the file. For optimization purposes hashsums calculation and grouping performed in multiple threads. All sets containing only single item are dropped, remaining sets are passed to the third step as an input.

During the second step program creates the collection of sets of file with the equal hashsum of the complete file. For optimization purposes hashsums calculation and grouping performed in multiple threads and hashsum is not calculated for files with size <= 1024 bytes: partial hashsum, calculated on the second step is used for such file instead. All sets containing only single item are dropped, remaining files in groups are identical.

After third step, the collection of sets of identical files is used to print the output.
