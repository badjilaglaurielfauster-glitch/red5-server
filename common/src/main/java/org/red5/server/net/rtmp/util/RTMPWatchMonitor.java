package org.red5.server.net.rtmp.util;

import org.red5.server.net.rtmp.event.Ping;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RTMPWatchMonitor {

    /**
     * Last ping round trip time
     */
    protected AtomicInteger lastPingRoundTripTime = new AtomicInteger(-1);

    /**
     * Timestamp when last ping command was sent.
     */
    protected AtomicLong lastPingSentOn = new AtomicLong(0);

    /**
     * Timestamp when last ping result was received.
     */
    protected AtomicLong lastPongReceivedOn = new AtomicLong(0);

    /**
     * Ping interval in ms to detect dead clients.
     */
    protected volatile Duration pingInterval = Duration.ofMillis(5000L);

    /**
     * Maximum time in ms after which a client is disconnected because of inactivity.
     */
    protected volatile int maxInactivity = 60000;

    public int getMaxInactivity() {
        return maxInactivity;
    }

    public int getLastPingTime() {
        return lastPingRoundTripTime.get();
    }

    public void setMaxInactivity(int max) {
        this.maxInactivity = max;
    }

    public void setPingInterval(int interval) {
        this.pingInterval = Duration.ofMillis(interval);
    }

    public boolean isMonitoringEnabled() {
        return pingInterval != null && !pingInterval.isZero();
    }

    public Instant getInitialDelay() {
        return Instant.now().plusMillis(2000L);
    }

    public Duration getPingInterval() {
        return pingInterval;
    }

    /**
     * Difference between when the last ping was sent and when the last pong was received.
     *
     * @return last interval of ping minus pong
     */
    public int getLastPingSentAndLastPongReceivedInterval() {
        return (int) (lastPingSentOn.get() - lastPongReceivedOn.get());
    }

    /**
     * Prépare les données pour l'envoi d'un ping.
     * @return la valeur entière du timestamp à envoyer.
     */
    public int preparePing() {
        long now = System.currentTimeMillis();
        if (lastPingSentOn.get() == 0) {
            lastPongReceivedOn.set(now);
        }
        lastPingSentOn.set(now);
        return (int) (now & 0xffffffffL);
    }

    /**
     * Traite la réception d'un pong et calcule le RTT.
     */
    public void processPong(Ping pong) {
        long now = System.currentTimeMillis();
        long sentOn = lastPingSentOn.get();
        int sentValue = (int) (sentOn & 0xffffffffL);
        int pongValue = pong.getValue2().intValue();
        if (pongValue == sentValue) {
            lastPingRoundTripTime.set((int) ((now - sentOn) & 0xffffffffL));
        }
        lastPongReceivedOn.set(now);
    }

    public long getLastPingSentOn() {
        return lastPingSentOn.get();
    };

    /**
     * Détermine si la connexion est inactive selon les seuils configurés.
     * @return true si le délai d'inactivité est dépassé.
     */
    public boolean isIdle() {
        long lastPingTime = lastPingSentOn.get();
        long lastPongTime = lastPongReceivedOn.get();
        return (lastPongTime > 0 && (lastPingTime - lastPongTime > maxInactivity));
    }

    /**
     * Détermine si la connexion doit être coupée pour inactivité.
     * Centralise les calculs de timestamps et de seuils.
     */
    public boolean shouldDisconnect(long now, long lastBytesReadTime) {
        long lastPing = lastPingSentOn.get();
        long lastPong = lastPongReceivedOn.get();
        boolean noPongResponse = (lastPong > 0 && (lastPing - lastPong > maxInactivity));
        boolean noDataRx = (now - lastBytesReadTime > maxInactivity);

        return noPongResponse && noDataRx;
    }

}
