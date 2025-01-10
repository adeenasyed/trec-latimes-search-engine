import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.GZIPInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class IndexEngine {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java IndexEngine.java <path_to_latimes.gz> <path_to_output_directory>");
            System.exit(1);
        }

        String inputFilePath = args[0];
        String outputDirectoryPath = args[1];

        File outputDirectory = new File(outputDirectoryPath);
        if (outputDirectory.exists()) {
            System.out.println("Error: the directory " + outputDirectoryPath + " already exists");
            System.exit(1);
        }
        outputDirectory.mkdir();

        try {
            FileInputStream fileInputStream = new FileInputStream(inputFilePath);
            GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream);
            BufferedReader reader = new BufferedReader(new InputStreamReader(gzipInputStream));

            File docnosFile = new File(outputDirectoryPath, "docnos.txt");
            BufferedWriter docnosWriter = new BufferedWriter(new FileWriter(docnosFile));

            File docLengthsFile = new File(outputDirectoryPath, "doc-lengths.txt");
            BufferedWriter docLengthsWriter = new BufferedWriter(new FileWriter(docLengthsFile));

            File lexiconFile = new File(outputDirectoryPath, "lexicon.txt");
            BufferedWriter lexiconWriter = new BufferedWriter(new FileWriter(lexiconFile));
            Map<String, Integer> lexicon = new HashMap<>();

            File invertedIndex = new File(outputDirectoryPath, "inverted-index");
            invertedIndex.mkdir();

            String line;
            boolean inDocument = false;
            StringBuilder document = new StringBuilder();
            int id = 1;

            while ((line = reader.readLine()) != null) {
                if (line.contains("<DOC>")) {
                    inDocument = true;
                    document = new StringBuilder();
                } 

                if (inDocument) {
                    document.append(line).append("\n");
                }

                if (line.contains("</DOC>")) {
                    inDocument = false;
                    String stringDocument = document.toString();
                    String docNo = extractDocNo(stringDocument);
                    int[] tokens = tokenizeDocument(stringDocument, lexicon, lexiconWriter);
                    
                    docnosWriter.write(docNo);
                    docnosWriter.newLine(); 
                    docnosWriter.flush();

                    docLengthsWriter.write(String.valueOf(tokens.length));
                    docLengthsWriter.newLine(); 
                    docLengthsWriter.flush();

                    updateInvertedIndex(id, tokens, invertedIndex);

                    File directory = new File(outputDirectoryPath, HelperFunctions.getDirectoryPath(docNo));
                    File metadataDirectory = new File(directory, "metadata");
                    if (!directory.exists()) {
                        directory.mkdirs();
                        metadataDirectory.mkdir();
                    }
                    saveDocument(stringDocument, docNo, directory);
                    saveMetadata(id, docNo, extractHeadline(stringDocument), extractDate(docNo), metadataDirectory);
                    
                    id++;
                }
            }

            fileInputStream.close();
            gzipInputStream.close();
            reader.close();
            docnosWriter.close();
            docLengthsWriter.close();
            lexiconWriter.close();
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String extractDocNo(String document) {
        return document.substring(document.indexOf("<DOCNO>") + 7, document.indexOf("</DOCNO>")).trim();
    }

    private static String extractDate(String docNo) {
        String dateString = docNo.substring(2, 4) + "/" + docNo.substring(4, 6) + "/19" + docNo.substring(6, 8);
        DateTimeFormatter originalFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        LocalDate date = LocalDate.parse(dateString, originalFormat);
        DateTimeFormatter desiredFormat = DateTimeFormatter.ofPattern("MMMM d, yyyy");
        return date.format(desiredFormat);
    }

    private static String extractHeadline(String document) {
        int start = document.indexOf("<HEADLINE>");
        if (start == -1) return "";
        start = document.indexOf("<P>", start);
        if (start == -1) return "";
        start += 3;
        int end = document.indexOf("</P>", start);
        if (end == -1) return "";
        String headline = document.substring(start, end).trim();
        headline = headline.replaceAll("\\n", " ");
        return headline;
    }

    private static int[] tokenizeDocument(String document, Map<String, Integer> lexicon, BufferedWriter lexiconWriter) throws IOException {
        String[] tags = {"HEADLINE", "TEXT", "GRAPHIC"};
        String text = "";

        for (String tag : tags) { 
            String pattern = "<" + tag + ">.*?</" + tag + ">";
            Pattern regex = Pattern.compile(pattern, Pattern.DOTALL);
            Matcher matcher = regex.matcher(document);

            while (matcher.find()) {
                String section = matcher.group();
                section = section.replaceAll("<.*?>", " ");
                text += section + " ";
            }
        }
        text = text.trim().toLowerCase();

        String[] tokens = HelperFunctions.tokenizeText(text);
        //String[] tokens = HelperFunctions.tokenizeTextWithPorterStemmer(text);
        int[] tokenIds = new int[tokens.length];
        
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            
            if (lexicon.containsKey(token)) {
                tokenIds[i] = lexicon.get(token);
            } else {
                int tokenId = lexicon.size() + 1;
                lexicon.put(token, tokenId);

                lexiconWriter.write(token);
                lexiconWriter.newLine();
                lexiconWriter.flush();

                tokenIds[i] = tokenId;
            }
        }

        return tokenIds;
    }

    private static void updateInvertedIndex(int id, int[] tokens, File directory) throws IOException { 
        HashMap<Integer, Integer> tokenCounts = new HashMap<>();
        for (int token : tokens) {
            tokenCounts.put(token, tokenCounts.getOrDefault(token, 0) + 1);
        }

        for (Map.Entry<Integer, Integer> entry : tokenCounts.entrySet()) {
            int token = entry.getKey();
            int count = entry.getValue();
            
            File outputFile = new File(directory, token + ".txt");
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true));
            writer.write(String.valueOf(id));
            writer.newLine();
            writer.write(String.valueOf(count));
            writer.newLine();
            writer.close();
        }
    }

    private static void saveDocument(String document, String docNo, File directory) throws IOException { 
        File outputFile = new File(directory, docNo + ".txt");
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
        writer.write(document);
        writer.close();
    }

    private static void saveMetadata(int id, String docNo, String headline, String date, File directory) throws IOException { 
        File metadataFile = new File(directory, docNo + ".metadata.json");
        BufferedWriter writer = new BufferedWriter(new FileWriter(metadataFile));

        String metadata = "{\n" +
        "  \"id\": " + id + ",\n" +
        "  \"docNo\": \"" + docNo + "\",\n" +
        "  \"headline\": \"" + headline + "\",\n" +
        "  \"date\": \"" + date + "\"\n" +
        "}";

        writer.write(metadata);
        writer.close();
    }
}
