import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

public class Monitor {

    private static final int MAX_REDIRECTS = 5;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Monitor <urls-file>");
            return;
        }

        String urlsFile = args[0];
        Monitor monitor = new Monitor();

        try {
            List<String> urls = monitor.readURLsFromFile(urlsFile);
            monitor.processURLs(urls);
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }

    private List<String> readURLsFromFile(String urlsFile) throws IOException {
        List<String> urls = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(urlsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                urls.add(line);
            }
        }
        return urls;
    }

    private void processURLs(List<String> urls) {
        for (String url : urls) {
            processURL(url, 0, true);
        }
    }

    private void processURL(String url, int redirectCount, boolean isOriginalUrl) {
        if (redirectCount > MAX_REDIRECTS) {
            System.out.println("URL: " + url);
            System.out.println("Exceeded max redirects for: " + url);
            System.out.println();
            return;
        }

        try {
            if (isOriginalUrl) {
                System.out.println("URL: " + url);
            }

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(false);

            int statusCode = connection.getResponseCode();
            System.out.println("Status: " + statusCode + " " + connection.getResponseMessage());

            if (statusCode == HttpURLConnection.HTTP_MOVED_TEMP || statusCode == HttpURLConnection.HTTP_MOVED_PERM || statusCode == HttpURLConnection.HTTP_SEE_OTHER) {
                String newUrl = connection.getHeaderField("Location");
                System.out.println("Redirected URL: " + newUrl);
                processURL(newUrl, redirectCount + 1, false);
            } else if (statusCode == HttpURLConnection.HTTP_OK && isOriginalUrl) {
                if (url.endsWith("page.html") || url.endsWith("temp/page.html")) {
                    extractAndPrintReferencedURLs(connection, url);
                }
            }
        } catch (IOException e) {
            if (!isOriginalUrl) {
                System.out.println("URL: " + url);
            }
            System.out.println("Status: Network Error");
        }
        System.out.println();
    }


    private void extractAndPrintReferencedURLs(HttpURLConnection connection, String baseUrl) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        Pattern pattern = Pattern.compile("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String imgSrc = matcher.group(1);
            String resolvedUrl = resolveUrl(baseUrl, imgSrc);
            checkReferencedUrl(resolvedUrl);
        }
    }

    private String resolveUrl(String baseUrl, String imgSrc) throws MalformedURLException {
        URL base = new URL(baseUrl);
        URL resolved = new URL(base, imgSrc); 
        return resolved.toString();
    }

    private void checkReferencedUrl(String urlString) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
            connection.setRequestMethod("GET");
            int status = connection.getResponseCode();
            System.out.println("Referenced URL: " + urlString);
            System.out.println("Status: " + status + " " + connection.getResponseMessage());
        } catch (IOException e) {
            System.out.println("Referenced URL: " + urlString);
            System.out.println("Status: Network Error");
        }
        System.out.println();
    }
}
