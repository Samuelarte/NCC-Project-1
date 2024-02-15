import java.io.*;
import java.net.*;

public class Monitor {
    public static void main(String[] args) {
        // Check if the correct number of arguments is provided
        if (args.length != 1) {
            System.out.println("Usage: java Monitor <urls-file>");
            return;
        }

        String urlsFile = args[0]; // Get the name of the URLs file from command-line argument

        try {
            // Parse URLs from the file
            parseAndProcessURLs(urlsFile);
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }

    // Method to parse URLs from the file and process them
    private static void parseAndProcessURLs(String fileName) throws IOException {
        // Open the file containing the list of URLs
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            // Read each line (URL) from the file
            while ((line = reader.readLine()) != null) {
                // Process each URL
                processURL(line);
            }
        }
    }

    // Method to process each URL
    private static void processURL(String url) {
        try {
            // Parse URL
            URL u = new URL(url);
            String protocol = u.getProtocol();
            String host = u.getHost();
            int port = u.getPort() != -1 ? u.getPort() : (protocol.equalsIgnoreCase("https") ? 443 : 80);
            String path = u.getPath();

            // Establish TCP connection
            Socket socket = new Socket(host, port);

            // Construct HTTP request message
            String request = "GET " + path + " HTTP/1.1\r\n" +
                    "Host: " + host + "\r\n" +
                    "Connection: close\r\n\r\n";

            // Send request to the server
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(request);

            // Receive response from the server
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String responseLine = in.readLine();

            // Check if the response is not null and starts with "HTTP/"
            if (responseLine != null && responseLine.startsWith("HTTP/")) {
                // Parse status code from the response line
                int statusCode = Integer.parseInt(responseLine.split(" ")[1]);

                // Print status based on status code
                if (statusCode >= 200 && statusCode < 300) {
                    System.out.println("URL: " + url + "\nStatus: " + statusCode + " OK");

                    // Check if the response contains referenced image URLs
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (line.contains("<img src=")) {
                            String imageUrl = line.split("<img src=\"")[1].split("\"")[0];
                            // Fetch referenced image URL and print status
                            fetchAndPrintImageStatus(url, imageUrl);
                        }
                    }
                } else if (statusCode >= 300 && statusCode < 400) {
                    System.out.println("URL: " + url + "\nStatus: " + statusCode + " Moved Permanently");
                    // Follow redirection
                    String redirectedURL = getRedirectedURL(in);
                    if (redirectedURL != null) {
                        System.out.println("Redirected URL: " + redirectedURL);
                        processURL(redirectedURL);
                    }
                } else if (statusCode >= 400 && statusCode < 500) {
                    System.out.println("URL: " + url + "\nStatus: " + statusCode + " Not Found");
                }
            } else {
                System.out.println("URL: " + url + "\nStatus: Network Error");
            }

            // Close connections
            out.close();
            in.close();
            socket.close();
        } catch (IOException | NumberFormatException e) {
            System.out.println("URL: " + url + "\nStatus: Network Error");
        }
    }

    // Method to extract the redirected URL from the HTTP response
    private static String getRedirectedURL(BufferedReader in) throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            if (line.startsWith("Location:")) {
                return line.substring("Location:".length()).trim();
            }
        }
        return null;
    }

    // Method to fetch and print the status of referenced image URLs
    private static void fetchAndPrintImageStatus(String originalURL, String imageUrl) {
        try {
            URL u = new URL(originalURL);
            String host = u.getHost();
            if (!imageUrl.startsWith("http")) {
                imageUrl = "http://" + host + imageUrl;
            }
            HttpURLConnection connection = (HttpURLConnection) new URL(imageUrl).openConnection();
            connection.setRequestMethod("GET");
            int statusCode = connection.getResponseCode();
            System.out.println("Referenced URL: " + imageUrl);
            if (statusCode >= 200 && statusCode < 300) {
                System.out.println("Status: " + statusCode + " OK");
            } else {
                System.out.println("Status: " + statusCode + " Not Found");
            }
        } catch (IOException e) {
            System.out.println("Referenced URL: " + imageUrl);
            System.out.println("Status: Network Error");
        }
    }
}
