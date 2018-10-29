package edu.yale.network.Util;

import java.time.Instant;

public class CachedBytes {
    private Instant lastModified;
    private byte[] bytes;

    public CachedBytes(byte[] bytes, Instant lastModified) {
        this.bytes = bytes;
        this.lastModified = lastModified;
    }

    public Instant getLastModified() {
        return lastModified;
    }

    public byte[] getBytes() {
        return bytes;
    }
}
