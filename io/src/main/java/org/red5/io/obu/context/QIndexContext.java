package org.red5.io.obu.context;

import org.red5.io.obu.OBPFrameHeader;

public class QIndexContext {

    boolean ignoreDeltaQ;

    int segmentId;

    int currentQIndex;

    OBPFrameHeader fh;

    public QIndexContext(int currentQIndex, OBPFrameHeader fh, boolean ignoreDeltaQ, int segmentId) {
        this.currentQIndex = currentQIndex;
        this.fh = fh;
        this.ignoreDeltaQ = ignoreDeltaQ;
        this.segmentId = segmentId;
    }

    public int getCurrentQIndex() {
        return currentQIndex;
    }

    public void setCurrentQIndex(int currentQIndex) {
        this.currentQIndex = currentQIndex;
    }

    public OBPFrameHeader getFh() {
        return fh;
    }

    public void setFh(OBPFrameHeader fh) {
        this.fh = fh;
    }

    public boolean isIgnoreDeltaQ() {
        return ignoreDeltaQ;
    }

    public void setIgnoreDeltaQ(boolean ignoreDeltaQ) {
        this.ignoreDeltaQ = ignoreDeltaQ;
    }

    public int getSegmentId() {
        return segmentId;
    }

    public void setSegmentId(int segmentId) {
        this.segmentId = segmentId;
    }
}
