package edu.yale.network.Util;

import java.io.*;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

public class HttpRequestParser {

    private final String[] tokens;
    private final HashMap<String, String> params = new HashMap<>();

    public HttpRequestParser(InputStream ins) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(ins));

        String requestLine = reader.readLine();
        tokens = requestLine.split("\\s+");
        if (   tokens.length != 3
            || !tokens[0].equals("GET")
            || !tokens[1].startsWith("/") || tokens[1].contains("..")
            || !tokens[2].startsWith("HTTP/"))
            throw new IOException("Illegal request line: " + requestLine);

        for (;;) {
            String line = reader.readLine();
            if (line.isEmpty()) break;

            String[] param = line.split(":\\s+");
            if (param.length != 2) {
                throw new IOException("Illegal http field: " + line);
            }
            params.put(param[0], param[1]);
        }

        if (!params.containsKey("Host")) {
            params.put("Host", "default.host");
        } else {
            String host = params.get("Host");
            int indexof = host.indexOf(':');
            if (indexof != -1) {
                params.put("Host", host.substring(0, indexof));
            }
        }

        if (params.containsKey("If-Modified-Since")) {
            Instant parsedInstant = ZonedDateTime.parse(params.get("If-Modified-Since"), RFC_1123_DATE_TIME).toInstant();
            params.put("If-Modified-Since", parsedInstant.toString());
        } else {
            params.put("If-Modified-Since", Instant.MIN.toString()); // Default value
        }
    }

    public String getUri() {
        return tokens[1];
    }

    public String getParam(String key) {
        return params.get(key);
    }

}
