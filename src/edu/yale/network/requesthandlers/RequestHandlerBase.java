package edu.yale.network.requesthandlers;

import edu.yale.network.Util.CachedBytes;
import edu.yale.network.Util.HttpRequestParser;
import edu.yale.network.Util.Monitor;

import java.io.*;
import java.net.Socket;
import java.net.URLConnection;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
public class RequestHandlerBase {
    private final Logger logger = Logger.getLogger(RequestHandlerBase.class.getSimpleName());
    private static final ConcurrentHashMap<Path, CachedBytes> cache = new ConcurrentHashMap<>();
    private static final String STATUS_OKAY = "HTTP/1.0 200: OK";
    private static final String STATUS_ERR = "HTTP/1.0 400 Bad Request";
    private static final String STATUS_OVERLOAD = "HTTP/1.0 503: Service Unavailable";
    private static final String STATUS_NOT_MODIFIED = "HTPP/1.0 304: Not Modified";


    private final int cacheSize;
    private final int timeout;
    private final Monitor monitor;
    private final HashMap<String, String> docRoots; // read only, no need to lock

    RequestHandlerBase(int cacheSize, Monitor monitor, int timeout, HashMap<String, String> docRoots) {
        this.cacheSize = cacheSize;
        this.monitor = monitor;
        this.timeout = timeout;
        this.docRoots = docRoots;
    }

    protected void process(Socket connection, String serverName) {
        BufferedOutputStream raw;
        try {
            raw = new BufferedOutputStream(connection.getOutputStream());
        } catch (IOException ex) {
            logger.warning("Exception thrown opening input stream, exit");
            try {connection.close();} catch (IOException e) {}
            return ;
        }

        HttpRequestParser parser;
        try {
            connection.setSoTimeout(timeout);
            parser = new HttpRequestParser(connection.getInputStream());
        } catch (IOException | DateTimeParseException ex) {
            sendHeader(raw, STATUS_ERR, serverName, true);
            logger.info("Exception thrown parsing request.");
            try {connection.close();} catch (IOException e) {}
            return ;
        }

        String uri = parser.getUri();
        if (uri.equals("/load")) {
            String status = monitor.overload() ? STATUS_OVERLOAD : STATUS_OKAY;
            sendHeader(raw, status, serverName, true);
            logger.info("Heartbeat Monitoring reply: " + status);
            try {connection.close();} catch (IOException e) {}
            return ;
        }

        String host = parser.getParam("Host"); // if not specified, return is defaulted to "default.host"
        String docRootStr = docRoots.get(host); // docRoots contains default.host to first root

        if (docRootStr == null) { // unknown host
            sendHeader(raw, STATUS_ERR, serverName, true);
            logger.info("Rejecting request for " + uri);
            try {connection.close();} catch (IOException e) {}
            return ;
        }

        Path docRoot = FileSystems.getDefault().getPath(docRoots.get(host));
        if (uri.contains("?") || uri.contains("cgi")) { // cgi request
            String tokens[] = uri.split("\\?");
            String cgi = tokens[0].substring(1);
            Path cgiFile = docRoot.resolve(cgi);
            if (!Files.exists(cgiFile) || !Files.isExecutable(cgiFile)) {
                sendHeader(raw, STATUS_ERR, serverName, true);
                try {connection.close();} catch (IOException e) {}
                logger.info("Rejecting request for " + uri);
                return ;
            }
            ProcessBuilder pb = new ProcessBuilder(cgiFile.toString());
            Map<String, String> env = pb.environment();
            if (tokens.length > 1) env.put("QUERY_STRING", tokens[1]);
            env.put("REQUEST_METHOD", "GET");
            env.put("REMOTE_ADDR", connection.getInetAddress().toString());
            try {
                Process process = pb.start();
                process.waitFor();
                InputStreamReader is = new InputStreamReader(process.getInputStream());
                BufferedReader r = new BufferedReader(is);

                String contentType = r.readLine();
                r.readLine();
                StringBuffer content = new StringBuffer();
                while (r.ready()) {
                    content.append(r.readLine());
                }
                sendResponse(raw, STATUS_OKAY, serverName, contentType, content.toString().getBytes(), null);

            } catch (Exception ex) {
                sendHeader(raw, STATUS_ERR, serverName, true);
            }
            try {connection.close();} catch (IOException e) {}
            return ;
        }

        uri = uri.substring(1); // remove starting slash
        String userAgent = parser.getParam("User-Agent");
        if (uri.isEmpty() || uri.endsWith("/")) {
            if (userAgent != null && userAgent.contains("iPhone") &&
                    Files.exists(docRoot.resolve(uri + "index_m.html")) ) {
                uri += "index_m.html";
            } else {
                uri += "index.html";
            }
        }

        Path filePath = docRoot.resolve(uri);
        filePath.normalize();
        Instant ifModifiedSince = Instant.parse(parser.getParam("If-Modified-Since"));
        String contentType = URLConnection.getFileNameMap().getContentTypeFor(uri);
        if (contentType == null) contentType = "text/plain; charset=utf-8";

        if (cache.containsKey(filePath)) { // cache hit
            CachedBytes cb = cache.get(filePath);
            Instant lastModified = cb.getLastModified();
            if (!ifModifiedSince.isBefore(lastModified)) {
                sendHeader(raw, STATUS_NOT_MODIFIED, serverName,  true);
                logger.info("Cache Hit, not modified: " + uri);
            } else {
                byte[] body = cb.getBytes();
                sendResponse(raw, STATUS_OKAY, serverName, contentType, body, lastModified);
                logger.info("Requested File sent(cache hit): " + uri);
            }
            try {connection.close();} catch (IOException e) {}
            return ;
        }

        // read file modified time which at the same time checked if file exists
        Instant lastModified;
        try {
            lastModified = Files.getLastModifiedTime(filePath).toInstant();
        } catch (IOException ex) { // file does not exist
            sendHeader(raw, STATUS_ERR, serverName,  true);
            try {connection.close();} catch (IOException e) {}
            logger.info("Requested File not found: " + filePath.toString());
            return ;
        }

        if (!ifModifiedSince.isBefore(lastModified)) {
            sendHeader(raw, STATUS_NOT_MODIFIED, serverName,  true);
            try {connection.close();} catch (IOException e) {}
            logger.info("Cache Miss, not modified: " + uri);
            return ;
        }

        if (!Files.isReadable(filePath)) { // cannot read
            sendHeader(raw, STATUS_ERR, serverName,  true);
            try {connection.close();} catch (IOException e) {}
            logger.info("Requested File not readable: " + filePath.toString());
            return ;
        }

        // file exist and can read so this is a cache miss
        try { // read file and send back, maybe put into cache
            byte[] fileBytes = Files.readAllBytes(filePath);

            if (cache.size() < cacheSize) {
                cache.put(filePath, new CachedBytes(fileBytes, lastModified));
            }
            sendResponse(raw, STATUS_OKAY, serverName, contentType, fileBytes, lastModified);
            try {connection.close();} catch (IOException e) {}
            logger.info("Request File sent: " + filePath.toString());
        } catch (OutOfMemoryError ex) {
            sendHeader(raw, STATUS_ERR, serverName, true);
            logger.severe("Requested File is too large: " + filePath.toString());
            try {connection.close();} catch (IOException e) {}
        } catch (IOException ex) {
            sendHeader(raw, STATUS_ERR, serverName, true);
            logger.severe("Requested File fail to open after checked readable: " + filePath.toString());
            try {connection.close();} catch (IOException e) {}
        }
    }

    protected void sendResponse(OutputStream raw, String responseCode, String serverName,
                              String contentType, byte[] content, Instant lastModified) {
        try {
            sendHeader(raw, responseCode, serverName,  false);
            Writer w = new OutputStreamWriter(raw);
            StringBuilder response = new StringBuilder();

            if (lastModified != null) {
                String lastModifiedStr = ZonedDateTime.ofInstant(lastModified, ZoneOffset.UTC).format(RFC_1123_DATE_TIME);
                response.append("Last-Modified: ").append(lastModifiedStr) .append("\r\n");
            }
            response.append("Content-Length: ").append(content.length) .append("\r\n")
                    .append("Content-Type: ").append(contentType)
                    .append("\r\n\r\n");

            w.write(response.toString());
            w.flush();
            raw.write(content);
            raw.flush();
        } catch (IOException ex) {
        }
    }

    protected void sendHeader(OutputStream raw, String responseCode, String serverName, boolean emptyFile) {
        try {
            Writer w = new OutputStreamWriter(raw);
            StringBuilder response = new StringBuilder();

            response.append(responseCode)
                    .append("\r\n")
                    .append("Date: ").append(ZonedDateTime.now(ZoneOffset.UTC).format(RFC_1123_DATE_TIME))
                    .append("\r\n")
                    .append("Server: ").append(serverName)
                    .append("\r\n");
            if (emptyFile) {
                response.append("\r\n\r\n");
            }
            w.write(response.toString());
            w.flush();
        } catch (IOException ex) {
        }
    }
}
