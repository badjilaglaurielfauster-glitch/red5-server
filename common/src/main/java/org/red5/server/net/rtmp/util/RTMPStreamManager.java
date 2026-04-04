package org.red5.server.net.rtmp.util;

import org.red5.server.api.stream.IClientStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RTMPStreamManager {

    private static final Logger log = LoggerFactory.getLogger(RTMPStreamManager.class);

    public static final double MAX_RESERVED_STREAMS = 320;

    private transient ConcurrentMap<Number, IClientStream> streams = new ConcurrentHashMap<>(1, 0.9f, 1);

    private transient Set<Number> reservedStreams = Collections.newSetFromMap(new ConcurrentHashMap<Number, Boolean>(1, 0.9f, 1));

    private AtomicInteger usedStreams = new AtomicInteger(0);

    private transient ConcurrentMap<Number, Integer> streamBuffers = new ConcurrentHashMap<>(1, 0.9f, 1);

    /**
     * Réserve un ID de flux disponible (methode extraite de RTMPConnection).
     */
    public Number reserveStreamId() {
        double d = 1.0d;
        for (; d < MAX_RESERVED_STREAMS; d++) {
            if (reservedStreams.add(d)) {
                break;
            }
        }
        if (d == MAX_RESERVED_STREAMS) {
            throw new IndexOutOfBoundsException("Unable to reserve new stream");
        }
        return d;
    }

    /**
     * Valide si un ID peut être utilisé pour un nouveau flux. (methode extraite de RTMPConnection)
     */
    public boolean isValidStreamId(Number streamId) {
        double d = streamId.doubleValue();

        if (d <= 0 || !reservedStreams.contains(d)) {
            log.warn("Stream id: {} was not reserved", d);
            return false;
        }

        if (streams.get(d) != null) {
            log.warn("Another stream already exists with id: {}", d);
            return false;
        }
        return true;
    }

    /**
     * Enregistre un flux actif.
     */
    public boolean registerStream(IClientStream stream) {
        if (streams.putIfAbsent(stream.getStreamId().doubleValue(), stream) == null) {
            usedStreams.incrementAndGet();
            return true;
        }
        return false;
    }

    /**
     * Supprime un flux par son ID.
     */
    public void deleteStream(Number streamId) {
        double d = streamId.doubleValue();
        if (d > 0.0d) {
            if (streams.remove(d) != null) {
                usedStreams.decrementAndGet();
                streamBuffers.remove(d);
            }
        }
    }

    public int getCountUsedStream() {
        return usedStreams.get();
    }

    public IClientStream getStreamById(Number streamId) {
        return streams.get(streamId.doubleValue());
    }

    public Map<Number, IClientStream> getStreamsMap() {
        return Collections.unmodifiableMap(streams);
    }

    public void rememberBufferDuration(Number streamId, int duration) {
        streamBuffers.put(streamId, duration);
    }

    public Integer getBufferDuration(Number streamId) {
        return streamBuffers.get(streamId.doubleValue());
    }

    public void clear() {
        streams.clear();
        reservedStreams.clear();
        streamBuffers.clear();
    }

    /**
     * Tente de réserver un ID spécifique, sinon en génère un nouveau.
     */
    public Number reserveStreamId(Number streamId) {
        if (reservedStreams.add(streamId.doubleValue())) {
            return streamId;
        }
        return reserveStreamId();
    }

    /**
     * Tente de réserver un ID de flux spécifique.
     * @return true si l'ID a pu être réservé (n'existait pas encore), false sinon.
     */
    public boolean reserveId(Number streamId) {
        return reservedStreams.add(streamId.doubleValue());
    }

    /**
     * Supprime la réservation d'un ID de flux.
     * @return true si l'ID était effectivement réservé, false sinon.
     */
    public boolean unreserveId(Number streamId) {
        return reservedStreams.remove(streamId.doubleValue());
    }

}
