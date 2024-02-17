import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.net.HttpURLConnection;

public class Monitor {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Monitor <urls-file>");
            return;
        }

        String urlsFile = args[0];

        try {
            parseAndProcessURLs(urlsFile);
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }

    private static void parseAndProcessURLs(String urlsFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(urlsFile))) {
            String url;
            while ((url = reader.readLine()) != null) {
                processURL(url);
            }
        }
    }

    private static void processURL(String url) {
        try {
            URL u = new URL(url);
            String protocol = u.getProtocol();
            String host = u.getHost();
            int port = u.getPort() != -1 ? u.getPort() : (protocol.equalsIgnoreCase("https") ? 443 : 80);
            String path = u.getPath();

            Socket socket = new Socket(host, port);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("GET " + path + " HTTP/1.1\r\n" +
                    "Host: " + host + "\r\n" +
                    "Connection: close\r\n");

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String responseLine = in.readLine();

            if (responseLine != null && responseLine.startsWith("HTTP/")) {
                int statusCode = Integer.parseInt(responseLine.split(" ")[1]);

                if (statusCode >= 200 && statusCode < 300) {
                    System.out.println("URL: " + url + "\nStatus: " + statusCode + " OK");
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (line.contains("<img src=")) {
                            String imageUrl = line.split("<img src=\"")[1].split("\"")[0];
                            fetchAndPrintImageStatus(url, imageUrl);
                        }
                    }
                } else if (statusCode >= 300 && statusCode < 400) {
                    System.out.println("URL: " + url + "\nStatus: " + statusCode + " Moved Permanently");
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

            out.close();
            in.close();
            socket.close();
        } catch (IOException | NumberFormatException e) {
            System.out.println("URL: " + url + "\nStatus: Network Error");
        }
    }

    private static String getRedirectedURL(BufferedReader in) throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            if (line.startsWith("Location:")) {
                return line.substring("Location:".length()).trim();
            }
        }
        return null;
    }

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

