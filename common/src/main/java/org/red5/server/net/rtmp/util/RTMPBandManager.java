package org.red5.server.net.rtmp.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RTMPBandManager {

    /**
     * Data read interval - send BytesRead acknowledgement every 128KB
     * Reduced from 1MB to improve compatibility with some encoders
     */
    protected long bytesReadInterval = 128 * 1024;

    /**
     * Number of bytes to read next.
     */
    protected long nextBytesRead = 128 * 1024;

    /**
     * Number of bytes the client reported to have received.
     */
    protected AtomicLong clientBytesRead = new AtomicLong(0L);

    /**
     * Map for pending video packets keyed by stream id.
     */
    protected transient ConcurrentMap<Number, AtomicInteger> pendingVideos = new ConcurrentHashMap<>(1, 0.9f, 1);


    public void recordClientBytesRead(int bytes) {
        clientBytesRead.addAndGet(bytes);
    }

    public long getClientBytesRead() {
        return clientBytesRead.get();
    }

    public void incrementPendingVideo(Number streamId) {
        pendingVideos.computeIfAbsent(streamId.doubleValue(), k -> new AtomicInteger(0)).incrementAndGet();
    }

    public void decrementPendingVideo(Number streamId) {
        AtomicInteger pending = pendingVideos.get(streamId.doubleValue());
        if (pending != null) {
            pending.decrementAndGet();
        }
    }

    public int getPendingVideoCount(Number streamId) {
        AtomicInteger pending = pendingVideos.get(streamId.doubleValue());
        return pending != null ? pending.get() : 0;
    }

    public int getPendingCountSize() {
        return pendingVideos.size();
    }

    public void clear() {
        pendingVideos.clear();
        clientBytesRead.set(0);
    }

    /**
     * Vérifie si un acquittement de lecture (BytesRead) doit être envoyé.
     * @param currentReadBytes Octets totaux lus par la connexion
     * @return true si le seuil d'intervalle est atteint
     */
    public boolean shouldSendAcknowledgement(long currentReadBytes) {
        if (currentReadBytes >= nextBytesRead) {
            nextBytesRead += bytesReadInterval;
            return true;
        }
        return false;
    }

    /**
     * Supprime le suivi des vidéos en attente pour un flux.
     * @param streamId ID du flux
     */
    public void removePendingVideoStats(Number streamId) {
        pendingVideos.remove(streamId.doubleValue());
    }
}
