import java.io.*;
import java.util.*;

public class PlagiarismChecker {

    // Common English stop words to ignore in comparison
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "a", "an", "the", "and", "or", "but", "about", "above", "after", "again",
        "against", "all", "am", "are", "as", "at", "be", "because", "been", "before",
        "being", "below", "between", "both", "by", "could", "did", "do", "does", "doing",
        "down", "during", "each", "few", "for", "from", "further", "had", "has", "have",
        "having", "he", "her", "here", "hers", "herself", "him", "himself", "his", "how",
        "i", "if", "in", "into", "is", "it", "its", "itself", "me", "more", "most", "my",
        "myself", "no", "nor", "not", "of", "off", "on", "once", "only", "other", "our",
        "ours", "ourselves", "out", "over", "own", "same", "she", "should", "so", "some",
        "such", "than", "that", "their", "theirs", "them", "themselves", "then", "there",
        "these", "they", "this", "those", "through", "to", "too", "under", "until", "up",
        "very", "was", "we", "were", "what", "when", "where", "which", "while", "who",
        "whom", "why", "will", "with", "you", "your", "yours", "yourself", "yourselves"
    ));

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Plagiarism Checker Tool ===");
        System.out.print("Enter path to first file: ");
        String file1 = scanner.nextLine();

        System.out.print("Enter path to second file: ");
        String file2 = scanner.nextLine();

        try {
            String text1 = readFile(file1);
            String text2 = readFile(file2);

            List<String> words1 = tokenizeAndFilter(text1);
            List<String> words2 = tokenizeAndFilter(text2);

            double similarity = calculateJaccardSimilarity(words1, words2);

            System.out.printf("Plagiarism Similarity Score: %.2f%%\n", similarity * 100);
        } catch (IOException e) {
            System.err.println("Error reading files: " + e.getMessage());
        }
    }

    // Read the contents of a file into a single string
    private static String readFile(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();

        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;

        while ((line = reader.readLine()) != null) {
            content.append(line).append(" ");
        }

        reader.close();
        return content.toString().toLowerCase();
    }

    // Remove punctuation, tokenize text, and filter out stop words
    private static List<String> tokenizeAndFilter(String text) {
        text = text.replaceAll("[^a-zA-Z ]", " "); // remove punctuation
        String[] tokens = text.split("\\s+");

        List<String> result = new ArrayList<>();
        for (String word : tokens) {
            if (!STOP_WORDS.contains(word) && word.length() > 2) {
                result.add(word);
            }
        }

        return result;
    }

    // Compute Jaccard Similarity = |A ∩ B| / |A ∪ B|
    private static double calculateJaccardSimilarity(List<String> list1, List<String> list2) {
        Set<String> set1 = new HashSet<>(list1);
        Set<String> set2 = new HashSet<>(list2);

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        if (union.isEmpty()) return 0.0;

        return (double) intersection.size() / union.size();
    }
}
