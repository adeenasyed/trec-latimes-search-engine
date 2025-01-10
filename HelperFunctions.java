import java.io.*;
import java.util.*;

public class HelperFunctions { 
    public static String getDirectoryPath(String docNo) {
        return docNo.substring(6, 8) + "/" + docNo.substring(4, 6) + "/" + docNo.substring(2, 4);
    }

    public static Document[] loadDocuments(String directory) throws IOException { 
        File docnosFile = new File(directory, "docnos.txt");
        File docLengthsFile = new File(directory, "doc-lengths.txt");
        BufferedReader docnosReader = new BufferedReader(new FileReader(docnosFile));
        BufferedReader docLengthsReader = new BufferedReader(new FileReader(docLengthsFile));

        List<Document> documents = new ArrayList<>();

        int id = 1;
        String docno = "";
        String length = "";
        while ((docno = docnosReader.readLine()) != null && (length = docLengthsReader.readLine()) != null) { 
            documents.add(new Document(id, docno, Integer.parseInt(length)));
            id++;
        }
        docnosReader.close();
        docLengthsReader.close();

        return documents.toArray(new Document[documents.size()]);
    }

    public static HashMap<String, Integer> loadLexicon(String directory) throws IOException {
        File lexiconFile = new File(directory, "lexicon.txt");
        BufferedReader reader = new BufferedReader(new FileReader(lexiconFile));

        HashMap<String, Integer> lexicon = new HashMap<>();
        String token;
        int i = 1;
        
        while ((token = reader.readLine()) != null) { 
            lexicon.put(token, i);
            i++;
        }
        reader.close();

        return lexicon;
    }

    public static String[] tokenizeText(String text) { 
        text = text.toLowerCase();
        List<String> tokens = new ArrayList<>();
        int start = 0;

        for (int i = 0; i < text.length(); i++) {
            char currChar = text.charAt(i);

            if (!Character.isLetterOrDigit(currChar)) {
                if (start != i) { 
                    tokens.add(text.substring(start, i));
                }
                start = i + 1;
            }
        }
        if (start < text.length()) {
            tokens.add(text.substring(start));
        }

        return tokens.toArray(new String[tokens.size()]);
    }

    public static int[] convertTokensToIds(String[] tokens, Map<String, Integer> lexicon) { 
        List<Integer> tokenIds = new ArrayList<>();
        
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            
            if (lexicon.containsKey(token)) {
                tokenIds.add(lexicon.get(token));
            }
        }

        return tokenIds.stream().mapToInt(Integer::intValue).toArray();
    }
}
