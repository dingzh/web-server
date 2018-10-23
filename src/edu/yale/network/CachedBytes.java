package edu.yale.network;

import java.time.Instant;

class CachedBytes {
    private Instant lastModified;
    private byte[] bytes;

    CachedBytes(byte[] bytes, Instant lastModified) {
        this.bytes = bytes;
        this.lastModified = lastModified;
    }

    Instant getLastModified() {
        return lastModified;
    }

    byte[] getBytes() {
        return bytes;
    }
}
