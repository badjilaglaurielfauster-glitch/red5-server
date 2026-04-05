/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2023 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.server.net.rtmp.event;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.LinkedList;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.IoConstants;
import org.red5.io.utils.IOUtils;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.net.rtmp.event.base.BaseStreamData;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.stream.IStreamData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aggregate data event
 *
 * @author mondain
 */
public class Aggregate extends BaseStreamData implements IoConstants {

    private static final long serialVersionUID = 5538859593815804830L;

    private static Logger log = LoggerFactory.getLogger(Aggregate.class);

    /**
     * Data type
     */
    private byte dataType = TYPE_AGGREGATE;

    /**
     * Constructs a new Aggregate.
     */
    public Aggregate() {
        super(Type.STREAM_DATA, IoBuffer.allocate(0).flip());
    }

    /**
     * Create aggregate data event with given data buffer.
     *
     * @param data
     *            data
     */
    public Aggregate(IoBuffer data) {
        super(Type.STREAM_DATA, data);
    }

    /**
     * Create aggregate data event with given data buffer.
     *
     * @param data
     *            aggregate data
     * @param copy
     *            true to use a copy of the data or false to use reference
     */
    public Aggregate(IoBuffer data, boolean copy) {
        super(Type.STREAM_DATA);
        if (copy) {
            byte[] array = new byte[data.remaining()];
            data.mark();
            data.get(array);
            data.reset();
            setData(array);
        } else {
            setData(data);
        }
    }

    /** {@inheritDoc} */
    @Override
    public byte getDataType() {
        return dataType;
    }

    /**
     * <p>Setter for the field <code>dataType</code>.</p>
     *
     * @param dataType a byte
     */
    public void setDataType(byte dataType) {
        this.dataType = dataType;
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link org.apache.mina.core.buffer.IoBuffer} object
     */
    public IoBuffer getData() {
        return data;
    }

    /**
     * <p>Setter for the field <code>data</code>.</p>
     *
     * @param data a {@link org.apache.mina.core.buffer.IoBuffer} object
     */
    public void setData(IoBuffer data) {
        this.data = data;
    }

    /**
     * <p>Setter for the field <code>data</code>.</p>
     *
     * @param data an array of {@link byte} objects
     */
    public void setData(byte[] data) {
        this.data = IoBuffer.allocate(data.length);
        this.data.put(data).flip();
    }

    private Header createPartHeader(byte subType, int size, int timestamp) {
        Header partHeader = new Header();
        partHeader.setChannelId(header.getChannelId());
        partHeader.setDataType(subType);
        partHeader.setSize(size);
        partHeader.setStreamId(header.getStreamId());
        partHeader.setTimer(timestamp);
        return partHeader;
    }

    private void consumeBackPointer() {
        if (data.remaining() >= 4) {
            int backPointer = data.getInt();
            log.trace("Back pointer consumed: {}", backPointer);
        }
    }

    private IRTMPEvent extractPart(byte subType) {
        int size = IOUtils.readUnsignedMediumInt(data);
        int timestamp = IOUtils.readExtendedMediumInt(data);
        int streamId = IOUtils.readUnsignedMediumInt(data);

        Header partHeader = createPartHeader(subType, size, timestamp);

        IRTMPEvent event;
        switch (subType) {
            case TYPE_AUDIO_DATA:
                event = new AudioData(data.getSlice(size));
                break;
            case TYPE_VIDEO_DATA:
                event = new VideoData(data.getSlice(size));
                break;
            default:
                log.debug("Non-A/V subtype: {}", subType);
                event = new Unknown(subType, data.getSlice(size));
        }

        event.setTimestamp(timestamp);
        event.setHeader(partHeader);
        return event;
    }

    /**
     * Breaks-up the aggregate into its individual parts and returns them as a list. The parts are returned based on the ordering of the aggregate itself.
     *
     * @return list of IRTMPEvent objects
     */
    public LinkedList<IRTMPEvent> getParts() {
        LinkedList<IRTMPEvent> parts = new LinkedList<>();
        log.trace("Aggregate data length: {}", data.limit());

        while (data.position() < data.limit()) {
            try {
                byte subType = data.get();
                if (subType == 0) {
                    log.debug("Subtype 0 encountered, exiting aggregate processing");
                    break;
                }

                IRTMPEvent part = extractPart(subType);
                if (part != null) {
                    parts.add(part);
                }
                consumeBackPointer();
            } catch (Exception e) {
                log.error("Exception decoding aggregate parts", e);
                break;
            }
        }
        return parts;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("Aggregate - ts: %s length: %s", getTimestamp(), (data != null ? data.limit() : '0'));
    }

}
