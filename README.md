DupsFinder
==========

Search for duplicate files in the specified folder recursively.

Wirtten in Java without additional dependencies. Requires Java 8 and Maven to build. SHA-1 is used as a hashing algo.

This is expiremental branch, where walking over directories tree implemented in multitreaded way using Java [Fork/Join framework](http://docs.oracle.com/javase/tutorial/essential/concurrency/forkjoin.html). Do not show performace advantage over single-threaded approach from the master in my tests, but may introduce "Too many open files" exceptions if open files limit in operating system is too low. Most likely will not be merged into master, but shows good use of Fork/Join usage.
