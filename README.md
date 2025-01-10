# TREC LA Times Search Engine

This project uses the LA Times portion of TREC volumes 4 and 5 as the document collection for information retrieval. You can access it here: https://trec.nist.gov/data/cd45/index.html


This repo contains:
1. `IndexEngine.java`, a program that takes a gzip version of the LA Times document collection and stores each document and its metadata separately. It also creates and stores a lexicon and an inverted index using a simple tokenization scheme.
2. `InteractiveRetrieval.java`, a program that implements BM25 retrieval and generates query biased snippets to provide an interactive search experience.
3. `Document.java`, `Result.java`, and `HelperFunctions.java`. These are class files that are used by the above programs.

To run these programs:
1. Run `javac Document.java Result.java HelperFunctions.java` to compile class files.
2. Run `java IndexEngine.java <path_to_latimes.gz> <path_to_output_directory>` to index the LA Times document collection.
3. Run `java InteractiveRetrieval.java <path_to_indexed_document_collection>` to start the program.
