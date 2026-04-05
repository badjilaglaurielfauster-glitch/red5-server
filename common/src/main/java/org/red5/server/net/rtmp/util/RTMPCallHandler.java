package org.red5.server.net.rtmp.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.net.rtmp.DeferredResult;
import org.red5.server.service.Call;

public class RTMPCallHandler {

    /**
     * Initial pending calls capacity
     */
    private int pendingCallsInitalCapacity = 3;

    /**
     * Concurrency level for pending calls collection
     */
    private int pendingCallsConcurrencyLevel = 1;

    /**
     * Hash map that stores pending calls and ids as pairs.
     */
    protected transient ConcurrentMap<Integer, IPendingServiceCall> pendingCalls = new ConcurrentHashMap<>(pendingCallsInitalCapacity, 0.75f, pendingCallsConcurrencyLevel);

    /**
     * Deferred results set.
     *
     * @see org.red5.server.net.rtmp.DeferredResult
     */
    protected transient CopyOnWriteArraySet<DeferredResult> deferredResults = new CopyOnWriteArraySet<>();

    /**
     * Transaction identifier for remote commands.
     */
    protected AtomicInteger transactionId = new AtomicInteger(1);

    public int getNextTransactionId() {
        return transactionId.incrementAndGet();
    }

    public void registerPendingCall(int invokeId, IPendingServiceCall call) {
        pendingCalls.put(invokeId, call);
    }

    public IPendingServiceCall getPendingCall(int invokeId) {
        return pendingCalls.get(invokeId);
    }

    public IPendingServiceCall retrievePendingCall(int invokeId) {
        return pendingCalls.remove(invokeId);
    }

    /**
     * Gère la fermeture : notifie tous les appels en attente qu'ils ont échoué.
     */
    public void sendCloseError() {
        if (!pendingCalls.isEmpty()) {
            for (IPendingServiceCall call : pendingCalls.values()) {
                call.setStatus(Call.STATUS_NOT_CONNECTED);
                for (IPendingServiceCallback callback : call.getCallbacks()) {
                    callback.resultReceived(call);
                }
            }
            pendingCalls.clear();
        }
    }

    public void clear() {
        pendingCalls.clear();
        deferredResults.clear();
    }

    public void registerDeferredResult(DeferredResult result) {
        deferredResults.add(result);
    }

    public void unregisterDeferredResult(DeferredResult result) {
        deferredResults.remove(result);
    }

    /**
     * Vérifie s'il y a des appels de service en attente de réponse.
     * @return true si au moins un appel est en attente, false sinon.
     */
    public boolean hasPendingCalls() {
        return pendingCalls != null && !pendingCalls.isEmpty();
    }

}
