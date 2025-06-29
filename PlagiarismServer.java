import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;

public class PlagiarismServer {
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "a", "an", "and", "the", "in", "on", "at", "to", "for", "of", "with", "by", "is", "are", "was", "were",
        "be", "been", "being", "have", "has", "had", "do", "does", "did", "will", "would", "could", "should",
        "may", "might", "can", "this", "that", "these", "those", "i", "you", "he", "she", "it", "we", "they",
        "me", "him", "her", "us", "them", "my", "your", "his", "its", "our", "their", "what", "where", "when",
        "why", "how", "all", "any", "both", "each", "few", "more", "most", "other", "some", "such", "no", "nor",
        "not", "only", "own", "same", "so", "than", "too", "very", "just", "now"
    ));

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        // Add CORS handler
        server.createContext("/check-plagiarism", new PlagiarismHandler());
        
        server.setExecutor(null);
        server.start();
        
        System.out.println("Plagiarism Checker Server started on http://localhost:8080");
        System.out.println("Ready to accept file uploads for plagiarism detection...");
    }

    static class PlagiarismHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Add CORS headers
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendErrorResponse(exchange, "Method not allowed", 405);
                return;
            }

            try {
                String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
                if (contentType == null || !contentType.startsWith("multipart/form-data")) {
                    sendErrorResponse(exchange, "Content-Type must be multipart/form-data", 400);
                    return;
                }

                // Parse multipart form data
                Map<String, String> files = parseMultipartData(exchange.getRequestBody(), contentType);
                
                if (!files.containsKey("file1") || !files.containsKey("file2")) {
                    sendErrorResponse(exchange, "Both file1 and file2 are required", 400);
                    return;
                }

                String text1 = files.get("file1");
                String text2 = files.get("file2");

                // Calculate Jaccard similarity
                double similarity = calculateJaccardSimilarity(text1, text2);
                
                // Send response
                String jsonResponse = String.format("{\"similarity\": %.2f}", similarity);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
                
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonResponse.getBytes());
                }

                System.out.println("Plagiarism check completed. Similarity: " + String.format("%.2f%%", similarity));

            } catch (Exception e) {
                e.printStackTrace();
                sendErrorResponse(exchange, "Internal server error: " + e.getMessage(), 500);
            }
        }

        private Map<String, String> parseMultipartData(InputStream inputStream, String contentType) throws IOException {
            Map<String, String> files = new HashMap<>();
            
            // Extract boundary from content type
            String boundary = null;
            String[] parts = contentType.split(";");
            for (String part : parts) {
                part = part.trim();
                if (part.startsWith("boundary=")) {
                    boundary = "--" + part.substring(9);
                    break;
                }
            }
            
            if (boundary == null) {
                throw new IOException("No boundary found in multipart data");
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            String currentFieldName = null;
            StringBuilder currentContent = new StringBuilder();
            boolean inContent = false;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith(boundary)) {
                    // Save previous field if any
                    if (currentFieldName != null && inContent) {
                        files.put(currentFieldName, currentContent.toString().trim());
                    }
                    
                    // Reset for new field
                    currentFieldName = null;
                    currentContent = new StringBuilder();
                    inContent = false;
                    
                } else if (line.startsWith("Content-Disposition:")) {
                    // Extract field name
                    String[] dispositionParts = line.split(";");
                    for (String part : dispositionParts) {
                        part = part.trim();
                        if (part.startsWith("name=")) {
                            currentFieldName = part.substring(6, part.length() - 1); // Remove quotes
                            break;
                        }
                    }
                } else if (line.trim().isEmpty() && currentFieldName != null) {
                    // Empty line indicates start of content
                    inContent = true;
                } else if (inContent) {
                    // Content line
                    if (currentContent.length() > 0) {
                        currentContent.append("\n");
                    }
                    currentContent.append(line);
                }
            }

            // Save last field
            if (currentFieldName != null && inContent) {
                files.put(currentFieldName, currentContent.toString().trim());
            }

            return files;
        }

        private double calculateJaccardSimilarity(String text1, String text2) {
            Set<String> tokens1 = tokenizeAndClean(text1);
            Set<String> tokens2 = tokenizeAndClean(text2);

            if (tokens1.isEmpty() && tokens2.isEmpty()) {
                return 100.0; // Both empty, consider 100% similar
            }

            if (tokens1.isEmpty() || tokens2.isEmpty()) {
                return 0.0; // One empty, 0% similar
            }

            // Calculate intersection
            Set<String> intersection = new HashSet<>(tokens1);
            intersection.retainAll(tokens2);

            // Calculate union
            Set<String> union = new HashSet<>(tokens1);
            union.addAll(tokens2);

            // Jaccard similarity = |intersection| / |union|
            double similarity = (double) intersection.size() / union.size();
            
            return similarity * 100.0; // Convert to percentage
        }

        private Set<String> tokenizeAndClean(String text) {
            if (text == null || text.trim().isEmpty()) {
                return new HashSet<>();
            }

            // Convert to lowercase and remove punctuation
            String cleaned = text.toLowerCase().replaceAll("[^a-zA-Z\\s]", " ");
            
            // Split into words and filter
            Set<String> tokens = new HashSet<>();
            String[] words = cleaned.split("\\s+");
            
            for (String word : words) {
                word = word.trim();
                if (word.length() > 2 && !STOP_WORDS.contains(word)) {
                    tokens.add(word);
                }
            }
            
            return tokens;
        }

        private void sendErrorResponse(HttpExchange exchange, String message, int statusCode) throws IOException {
            String jsonError = String.format("{\"error\": \"%s\"}", message);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, jsonError.getBytes().length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(jsonError.getBytes());
            }
        }
    }
}
