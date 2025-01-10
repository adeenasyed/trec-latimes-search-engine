import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InteractiveRetrieval {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java InteractiveRetrieval.java <path_to_directory>");
            System.exit(1);
        }

        String directoryPath = args[0];
        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            System.out.println("Error: the directory " + directoryPath + " does not exist");
            System.exit(1);
        }

        try { 
            Document[] documents = HelperFunctions.loadDocuments(directoryPath);
            HashMap<String, Integer> lexicon = HelperFunctions.loadLexicon(directoryPath);

            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("Enter query: ");
                String query = scanner.nextLine();
                System.out.println();

                double startTime = System.nanoTime();

                String[] tokens = HelperFunctions.tokenizeText(query);
                int[] tokenIds = HelperFunctions.convertTokensToIds(tokens, lexicon);
                
                if (tokenIds.length != 0) {
                    HashMap<Integer, HashMap<Document, Integer>> invertedIndex = buildInvertedIndex(tokenIds, documents, directoryPath);
                    HashMap<String, Double> docScore = new HashMap<>();
                    for (HashMap.Entry<Integer, HashMap<Document, Integer>> invertedIndexEntry : invertedIndex.entrySet()) {
                        HashMap<Document, Integer> docTF = invertedIndexEntry.getValue();
                        int n = docTF.size();
                        for (HashMap.Entry<Document, Integer> docTFEntry : docTF.entrySet()) {
                            Document document = docTFEntry.getKey();
                            int tf = docTFEntry.getValue();
                            double K = 1.2*((1-0.75)+0.75*(document.getLength()/513.458907));
                            double score = (tf/(K+tf))*Math.log((131896-n+0.5)/(n+0.5));
                            docScore.merge(document.getDocno(), score, Double::sum);
                        }
                    }

                    PriorityQueue<Result> pq = new PriorityQueue<>();
                    for (HashMap.Entry<String, Double> entry : docScore.entrySet()) {
                        String docno = entry.getKey();
                        double score = entry.getValue();
                        pq.add(new Result(docno, score));
                    }

                    String[] results = new String[10];
                    int rank = 1;
                    while (!pq.isEmpty() && rank != 11) {
                        Result result = pq.remove();
                        System.out.print(String.valueOf(rank) + ". ");
                        String docno = result.getId();
                        printResult(docno, tokenIds, lexicon, directoryPath);
                        //printQueryBiasedSummary(docno, tokenIds, lexicon, directoryPath);
                        results[rank-1] = docno;
                        rank++;
                    }

                    double endTime = System.nanoTime();
                    String retrievalTime = String.format("%.2f", (endTime-startTime)/Math.pow(10,9));
                    System.out.println("Retrieval took " +  retrievalTime + " seconds");
                    System.out.println();

                    while (true) {
                        System.out.print("Enter rank of document to view, N to enter a new query, or Q to quit: ");
                        String input = scanner.nextLine();
                        if (input.equals("Q")) {
                            scanner.close();
                            System.exit(0);
                        } else if (input.equals("N")) {
                            break;
                        } else {
                            try {
                                int documentRank = Integer.parseInt(input);
                                if (documentRank >= 1 && documentRank <= 10) {
                                    String docno = results[documentRank-1];
                                    printDocument(docno, directoryPath);
                                } else {
                                    System.out.println("Invalid input, please try again");
                                }
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid input, please try again");
                            }
                        }
                    }
                }
            }
        } catch (IOException e) { 
            System.out.println("Error: " + e.getMessage());
            System.exit(1);
        }

    }

    public static HashMap<Integer, HashMap<Document, Integer>> buildInvertedIndex(int[] tokenIds, Document[] documents, String directory) throws IOException { 
        HashMap<Integer, HashMap<Document, Integer>> invertedIndex = new HashMap<>();

        for (int i = 0; i < tokenIds.length; i++) { 
            File file = new File(directory, "inverted-index/" + String.valueOf(tokenIds[i]) + ".txt");
            BufferedReader reader = new BufferedReader(new FileReader(file));

            HashMap<Document, Integer> termFrequencies = new HashMap<>();
            String id = "";
            String tf = "";
            while ((id = reader.readLine()) != null && (tf = reader.readLine()) != null) { 
                termFrequencies.put(documents[Integer.parseInt(id)-1], Integer.parseInt(tf));
            }
            invertedIndex.put(tokenIds[i], termFrequencies);

            reader.close();
        }

        return invertedIndex;
    }

    public static void printResult(String docno, int[] queryTokenIds, HashMap<String, Integer> lexicon, String directory) throws IOException { 
        File metadata = new File(directory + "/" + HelperFunctions.getDirectoryPath(docno) + "/metadata", docno + ".metadata.json");
        BufferedReader metadataReader = new BufferedReader(new FileReader(metadata));
        String metadataLine;
        String date = "";
        String headline = "";
        while ((metadataLine = metadataReader.readLine()) != null) {
            if (metadataLine.contains("\"date\":")) {
                date = metadataLine.substring(metadataLine.indexOf(":") + 3, metadataLine.lastIndexOf("\""));
            } else if (metadataLine.contains("\"headline\":")) {
                headline = metadataLine.substring(metadataLine.indexOf(":") + 3, metadataLine.lastIndexOf("\""));
            }
        }
        metadataReader.close();


        File file = new File(directory + "/" + HelperFunctions.getDirectoryPath(docno), docno + ".txt");
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        StringBuilder document = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            document.append(line).append("\n");
        }
        reader.close();
        String stringDocument = document.toString().replaceAll("\\s+", " ");

        PriorityQueue<Result> pq = new PriorityQueue<>();
        String[] tags = {"HEADLINE", "TEXT", "GRAPHIC"};
        Pattern sentencePattern = Pattern.compile("[^.!?]+[.!?]");
        int l_score = 3;
        for (String tag : tags) { 
            Pattern pattern = Pattern.compile("<" + tag + ">.*?</" + tag + ">", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(stringDocument);
            while (matcher.find()) {
                String section = matcher.group();
                section = section.replaceAll("<.*?>", " ");
                Matcher sentenceMatcher = sentencePattern.matcher(section);
                while (sentenceMatcher.find()) {
                    String sentence = sentenceMatcher.group().trim();
                    if (sentence.split(" ").length >= 5) {
                        String[] tokens = HelperFunctions.tokenizeText(sentence);
                        int[] tokenIds = HelperFunctions.convertTokensToIds(tokens, lexicon);

                        int h_score = 0;
                        if (tag.equals("HEADLINE")) { 
                            h_score++;
                        }
                        if (tag.equals("TEXT") && l_score > 0) { 
                            l_score--;
                        }

                        double score = calculateSentenceScore(tokenIds, queryTokenIds, h_score, l_score);
                        pq.add(new Result(sentence, score));
                    }
                }
            }
        }

        String summary = "";
        int it = 1;
        while (!pq.isEmpty() && it < 3) {
            Result result = pq.remove();
            summary += (result.getId() + " ");
            it++;
        }

        if (headline.isEmpty()) {
            headline = summary.substring(0, 51) + "...";
        }


        System.out.print(headline + " (" + date + ")");
        System.out.println();
        System.out.print(summary);
        System.out.print("(" + docno + ")");
        System.out.println();
        System.out.println();
    }

    public static double calculateSentenceScore(int[] sentenceTokenIds, int[] queryTokenIds, int h_score, int l_score) {
        int h = h_score;
        int l = l_score;

        int c = 0;
        int d = 0;
        int k = 0;
        int currentContiguousRun = 0;
        HashMap<Integer, Integer> tf = new HashMap<>();
        for (int queryTokenId : queryTokenIds) {
            tf.put(queryTokenId, 0);
        }
        for (int sentenceTokenId : sentenceTokenIds) {
            if (tf.containsKey(sentenceTokenId)) {
                c++;

                if (tf.get(sentenceTokenId) == 0) { 
                    d++;
                }
                tf.put(sentenceTokenId, 1);

                currentContiguousRun++;
                if (currentContiguousRun > k) {
                    k = currentContiguousRun;
                }
            } else { 
                currentContiguousRun = 0;
            }
        }

        return h*1 + l*2 + c*1.5 + d*3 + k*2.5;
    }

    public static void printDocument(String docno, String directory) throws IOException { 
        File document = new File(directory + "/" + HelperFunctions.getDirectoryPath(docno), docno + ".txt");
        BufferedReader documentReader = new BufferedReader(new FileReader(document));
        String documentLine;
        System.out.println();
        while ((documentLine = documentReader.readLine()) != null) {
            System.out.println(documentLine);
        }
        System.out.println();
        documentReader.close();
    }
}