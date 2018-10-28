package edu.yale.network;

import java.io.*;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.logging.Logger;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

public class ServerReadWriteHandler implements IReadWriteHandler {

    private static final String STATUS_OKAY = "HTTP/1.0 200: OK";
    private static final String STATUS_ERR = "HTTP/1.0 400 Bad Request";
    private static final String STATUS_OVERLOAD = "HTTP/1.0 503: Service Unavailable";
    private static final String STATUS_NOT_MODIFIED = "HTPP/1.0 304: Not Modified";

    private final Logger logger = Logger.getLogger(SequentialServer.class.getSimpleName());
    // shared cache between all handlers
    private static final HashMap<Path, CachedBytes> cache = new HashMap<>();
    private final int cacheSize;
    private final int timeout;
    private final Monitor monitor;
    private final HashMap<String, String> docRoots;

    private ByteBuffer inBuffer;
    private ByteBuffer headerBuffer;
    private ByteBuffer bodyBuffer = null;
    private String request;

    private boolean requestComplete = false;
    private boolean responseReady = false;
    private boolean responseSent = false;
    private boolean channelClosed = false;

    public ServerReadWriteHandler(int cacheSize, int timeout, Monitor monitor, HashMap<String, String> docRoots) {
        this.cacheSize = cacheSize;
        this.timeout = timeout;
        this.monitor = monitor;
        this.docRoots = docRoots;

        inBuffer = ByteBuffer.allocate(4096);
        headerBuffer = ByteBuffer.allocate(1024);
        request = new String();
    }

    @Override
    public void handleRead(SelectionKey key) throws IOException {
        if (requestComplete) { // this call should not happen, ignore
            logger.warning("Entering read handler after reading request.");
            return;
        }
        processInBuffer(key);

        if (requestComplete) {
            InputStream is = new ByteArrayInputStream(request.getBytes());
            HttpRequestParser hrp = new HttpRequestParser(is);

            generateResponse(hrp);
            responseReady = true;
            headerBuffer.flip();
            if (bodyBuffer != null) bodyBuffer.flip();
        }
        updateState(key);
    }

    void processInBuffer(SelectionKey key) throws IOException {

        SocketChannel client = (SocketChannel) key.channel();
        int readBytes = client.read(inBuffer);
        StringBuffer requestBuf = new StringBuffer();

        if (readBytes == -1) {
            requestComplete = true;
        } else {
            inBuffer.flip(); // read input
            while (inBuffer.hasRemaining()) {
                char ch = (char) inBuffer.get();
                requestBuf.append(ch);
            }
        }

        inBuffer.compact();
        request += requestBuf.toString();
        if (request.endsWith("\r\n\r\n")) {
            requestComplete = true;
        }
    }

    private void updateState(SelectionKey key) throws IOException {
        if (channelClosed)
            return;

        /*
         * if (responseSent) { Debug.DEBUG(
         * "***Response sent; shutdown connection"); client.close();
         * dispatcher.deregisterSelection(sk); channelClosed = true; return; }
         */
        int nextState = key.interestOps();
        if (requestComplete) {
            nextState = nextState & ~SelectionKey.OP_READ;
        } else {
            nextState = nextState | SelectionKey.OP_READ;
        }

        if (responseReady) {

            if (!responseSent) {
                nextState = nextState | SelectionKey.OP_WRITE;
            } else {
                nextState = nextState & ~SelectionKey.OP_WRITE;
            }
        }
        key.interestOps(nextState);
    }

    @Override
    public void handleWrite(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        client.write(headerBuffer);
        if (bodyBuffer != null) {
            client.write(bodyBuffer);
        }

        if (headerBuffer.remaining() == 0 && (bodyBuffer == null || bodyBuffer.remaining() == 0)) {
            responseSent = true;
            client.close();
        }
    }

    @Override
    public int getInitOps() {
        return SelectionKey.OP_READ;
    }

    @Override
    public void handleException() {

    }

    public void generateResponse(HttpRequestParser parser) {

        String uri = parser.getUri();
        if (uri.equals("/load")) {
            String status = monitor.overload() ? STATUS_OVERLOAD : STATUS_OKAY;
            generateHeader(status, false);
            logger.info("Heartbeat Monitoring reply: " + status);
            return ;
        }

        String host = parser.getParam("Host"); // if not specified, return is defaulted to "default.host"
        String docRootStr = docRoots.get(host); // docRoots contains default.host to first root
        if (docRootStr == null) { // unknown host
            generateHeader(STATUS_ERR, false);
            logger.info("Rejecting request for " + uri);
            return ;
        }

        Path docRoot = FileSystems.getDefault().getPath(docRoots.get(host));
        if (uri.contains("?")) { // cgi request
            // parse params and construct xx
            String tokens[] = uri.split("\\?");
            if (tokens.length > 2) {
                generateHeader(STATUS_ERR, false);
                logger.info("Rejecting request for " + uri);
                return ;
            }
            String cgi = tokens[0];
            String cgiParams = tokens[1];
            // TODO parsing and validate params
            // TODO build process, return file path, write back to cache
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
                generateHeader(STATUS_NOT_MODIFIED, false);
                logger.info("Cache Hit, not modified: " + uri);
            } else {
                byte[] body = cb.getBytes();
                generateHeader(STATUS_OKAY, true);
                appendHeader(lastModified, contentType, body);
                bodyBuffer = ByteBuffer.wrap(body);
                logger.info("Requested File sent(cache hit): " + uri);
            }
            return ;
        }

        // read file modified time which at the same time checked if file exists
        Instant lastModified;
        try {
            lastModified = Files.getLastModifiedTime(filePath).toInstant();
        } catch (IOException ex) { // file does not exist
            generateHeader(STATUS_ERR, false);
            logger.info("Requested File not found: " + filePath.toString());
            return ;
        }

        if (!ifModifiedSince.isBefore(lastModified)) {
            generateHeader(STATUS_NOT_MODIFIED, false);
            logger.info("Cache Miss, not modified: " + uri);
            return ;
        }

        if (!Files.isReadable(filePath)) { // cannot read
            generateHeader(STATUS_ERR, false);
            logger.info("Requested File not readable: " + filePath.toString());
            return ;
        }

        // file exist and can read so this is a cache miss
        try { // read file and send back, maybe put into cache
            byte[] fileBytes = Files.readAllBytes(filePath);

            if (cache.size() < cacheSize) {
                cache.put(filePath, new CachedBytes(fileBytes, lastModified));
            }
            generateHeader(STATUS_OKAY, true);
            appendHeader(lastModified, contentType, fileBytes);
            logger.info("Request File sent: " + filePath.toString());
        } catch (OutOfMemoryError ex) {
            generateHeader(STATUS_ERR, false);
            logger.severe("Requested File is too large: " + filePath.toString());
        } catch (IOException ex) {
            generateHeader(STATUS_ERR, false);
            logger.severe("Requested File fail to open after checked readable: " + filePath.toString());
        }
    }

    private void generateHeader(String responseCode, boolean partial) {
        StringBuffer response = new StringBuffer();
        response.append(responseCode) .append("\r\n")
                .append("Date: ").append(ZonedDateTime.now(ZoneOffset.UTC).format(RFC_1123_DATE_TIME)) .append("\r\n")
                .append("Server: ").append(SequentialServer.class.getSimpleName()) .append("\r\n");
        if (!partial) {
            response.append("\r\n\r\n");
        }

        headerBuffer.put(response.toString().getBytes());
    }

    private void appendHeader(Instant lastModified, String contentType, byte[] content) {
        StringBuffer response = new StringBuffer();
        String lastModifiedStr = ZonedDateTime.ofInstant(lastModified, ZoneOffset.UTC).format(RFC_1123_DATE_TIME);

        response.append("Last-Modified: ").append(lastModifiedStr)
                .append("\r\n")
                .append("Content-Length: ").append(content.length)
                .append("\r\n")
                .append("Content-Type: ").append(contentType)
                .append("\r\n\r\n");

        headerBuffer.put(response.toString().getBytes());
    }
}
