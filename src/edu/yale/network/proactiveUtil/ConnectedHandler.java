package edu.yale.network.proactiveUtil;

import edu.yale.network.ProactiveServer;
import edu.yale.network.Util.CachedBytes;
import edu.yale.network.Util.HttpRequestParser;
import edu.yale.network.Util.ServerConf;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

public class ConnectedHandler implements CompletionHandler<AsynchronousSocketChannel, Object> {

    private static final ConcurrentHashMap<Path, CachedBytes> cache = new ConcurrentHashMap<>();
    private static final String STATUS_OKAY = "HTTP/1.0 200: OK";
    private static final String STATUS_ERR = "HTTP/1.0 400 Bad Request";
    private static final String STATUS_OVERLOAD = "HTTP/1.0 503: Service Unavailable";
    private static final String STATUS_NOT_MODIFIED = "HTPP/1.0 304: Not Modified";

    private final ServerConf serverConf;
    private final AsynchronousServerSocketChannel server;

    public ConnectedHandler(ServerConf serverConf, AsynchronousServerSocketChannel server) {
        this.server = server;
        this.serverConf = serverConf;
    }

    @Override
    public void completed(AsynchronousSocketChannel connection, Object object) {
        server.accept(null, this);

        ByteBuffer requestByteBuf = ByteBuffer.allocate(2048);
        String request = "";

        try {
            while (true) {
                StringBuffer requestStringBuf = new StringBuffer();
                Future<Integer> rf = connection.read(requestByteBuf);
                Integer readBytes = rf.get(serverConf.timeout, TimeUnit.MILLISECONDS);

                if (readBytes != -1) {
                    requestByteBuf.flip(); // read input
                    while (requestByteBuf.hasRemaining()) {
                        requestStringBuf.append((char) requestByteBuf.get());
                    }
                    requestByteBuf.clear();
                    request += requestStringBuf.toString();
                    if (request.endsWith("\r\n\r\n")) {
                        break;
                    }
                } else {
                    break;
                }
            }
        } catch (TimeoutException ex) {
            try{ connection.close();} catch (IOException cex) {}
        } catch (Exception ex) {
            System.out.println("Fatal Error");
            try{ connection.close();} catch (IOException cex) {}
            return ;
        }

        HttpRequestParser parser;
        try {
            parser = new HttpRequestParser(new ByteArrayInputStream(request.getBytes()));
        } catch (IOException | DateTimeParseException ex) {
            try{ connection.close();} catch (IOException cex) {}
            return ;
        }

        String uri = parser.getUri();
        if (uri.equals("/load")) {
            String status = serverConf.monitor.overload() ? STATUS_OVERLOAD : STATUS_OKAY;
            sendResponse(connection, status, null, null, null);
            return ;
        }

        String host = parser.getParam("Host"); // if not specified, return is defaulted to "default.host"
        String docRootStr = serverConf.docRoots.get(host); // docRoots contains default.host to first root

        if (docRootStr == null) { // unknown host
            sendResponse(connection, STATUS_ERR, null, null, null);
            return ;
        }

        Path docRoot = FileSystems.getDefault().getPath(serverConf.docRoots.get(host));
        if (uri.contains("?") || uri.contains("cgi")) { // cgi request
            String tokens[] = uri.split("\\?");
            String cgi = tokens[0].substring(1);
            Path cgiFile = docRoot.resolve(cgi);
            if (!Files.exists(cgiFile) || !Files.isExecutable(cgiFile)) {
                sendResponse(connection, STATUS_ERR, null, null, null);
                try {connection.close();} catch (IOException e) {}
                return ;
            }
            ProcessBuilder pb = new ProcessBuilder(cgiFile.toString());
            Map<String, String> env = pb.environment();
            if (tokens.length > 1) env.put("QUERY_STRING", tokens[1]);
            env.put("REQUEST_METHOD", "GET");
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
                sendResponse(connection, STATUS_OKAY, contentType, content.toString().getBytes(), null);

            } catch (Exception ex) {
                sendResponse(connection, STATUS_ERR, null, null, null);
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
                sendResponse(connection, STATUS_NOT_MODIFIED, null, null, null);
            } else {
                byte[] body = cb.getBytes();
                sendResponse(connection, STATUS_OKAY, contentType, body, lastModified);
            }
            return ;
        }

        // read file modified time which at the same time checked if file exists
        Instant lastModified;
        try {
            lastModified = Files.getLastModifiedTime(filePath).toInstant();
        } catch (IOException ex) { // file does not exist
            sendResponse(connection, STATUS_ERR, null, null, null);
            return ;
        }

        if (!ifModifiedSince.isBefore(lastModified)) {
            sendResponse(connection, STATUS_NOT_MODIFIED, null, null, null);
            return ;
        }

        if (!Files.isReadable(filePath)) { // cannot read
            sendResponse(connection, STATUS_ERR, null, null, null);
            return ;
        }

        // file exist and can read so this is a cache miss
        try { // read file and send back, maybe put into cache
            byte[] fileBytes = Files.readAllBytes(filePath);

            if (cache.size() < serverConf.cacheSize) {
                cache.put(filePath, new CachedBytes(fileBytes, lastModified));
            }
            sendResponse(connection, STATUS_OKAY, contentType, fileBytes, lastModified);
        } catch (OutOfMemoryError ex) {
            sendResponse(connection, STATUS_ERR, null, null, null);
        } catch (IOException ex) {
            sendResponse(connection, STATUS_ERR, null, null, null);
        }
    }

    private void sendResponse(AsynchronousSocketChannel conn, String responseCode,
                              String contentType, byte[] content, Instant lastModified) {
        StringBuilder response = new StringBuilder();
        response.append(responseCode).append("\r\n")
                .append("Date: ").append(ZonedDateTime.now(ZoneOffset.UTC).format(RFC_1123_DATE_TIME)).append("\r\n")
                .append("Server: ").append(ProactiveServer.class.getSimpleName()).append("\r\n");

        if (content != null) {
            if (lastModified != null) {
                String lastModifiedStr = ZonedDateTime.ofInstant(lastModified, ZoneOffset.UTC).format(RFC_1123_DATE_TIME);
                response.append("Last-Modified: ").append(lastModifiedStr).append("\r\n");
            }
            response.append("Content-Length: ").append(content.length) .append("\r\n")
                    .append("Content-Type: ").append(contentType);
        }
        response.append("\r\n\r\n");

        ByteBuffer headBuf = ByteBuffer.wrap(response.toString().getBytes());

        if (content != null) {
            conn.write(headBuf);
            ByteBuffer bodyBuf = ByteBuffer.wrap(content);
            conn.write(bodyBuf, conn, new ResponseSentHandler());
        } else {
            conn.write(headBuf, conn, new ResponseSentHandler());
        }
    }

    @Override
    public void failed(Throwable exc, Object attachment) {

    }
}
