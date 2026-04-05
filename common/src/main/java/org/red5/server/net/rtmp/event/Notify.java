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
import java.util.Map;
import java.util.Objects;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.net.ICommand;
import org.red5.server.net.rtmp.event.base.BaseStreamData;
import org.red5.server.stream.IStreamData;

/**
 * Stream notification event. The invoke / transaction id is "always" equal to zero for a Notify.
 *
 * @author mondain
 */
public class Notify extends BaseStreamData implements ICommand {

    private static final long serialVersionUID = -6085848257275156569L;

    /**
     * Service call
     */
    protected IServiceCall call;

    /**
     * Event data type
     */
    protected byte dataType = TYPE_NOTIFY;

    /**
     * Invoke id / transaction id
     */
    protected int transactionId = 0;

    /**
     * Connection parameters
     */
    private Map<String, Object> connectionParams;

    private String action;

    /**
     * Constructs a new Notify
     */
    public Notify() {
        super(Type.SERVICE_CALL);
    }

    /**
     * Create new notification event with given service call
     *
     * @param call
     *            Service call
     */
    public Notify(IServiceCall call) {
        super(Type.SERVICE_CALL);
        this.call = call;
    }

    /**
     * Create new notification event with given byte buffer
     *
     * @param data
     *            Byte buffer
     */
    public Notify(IoBuffer data) {
        super(Type.STREAM_DATA, data);
    }

    /**
     * Create new notification event with given byte buffer and action.
     *
     * @param data Byte buffer
     * @param action Action / method
     */
    public Notify(IoBuffer data, String action) {
        super(Type.STREAM_DATA, data);
        this.action = action;
    }

    /**
     * {@inheritDoc}
     *
     * @return a byte
     */
    public byte getDataType() {
        return dataType;
    }

    /**
     * Setter for data
     *
     * @param data
     *            Data
     */
    public void setData(IoBuffer data) {
        this.data = data;
    }

    /**
     * Setter for call
     *
     * @param call
     *            Service call
     */
    public void setCall(IServiceCall call) {
        this.call = call;
    }

    /**
     * Getter for service call
     *
     * @return Service call
     */
    public IServiceCall getCall() {
        return this.call;
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
     * Getter for transaction id
     *
     * @return Transaction id
     */
    public int getTransactionId() {
        return transactionId;
    }

    /**
     * Release event (nullify call object)
     */
    protected void doRelease() {
        call = null;
    }

    /**
     * Getter for connection parameters
     *
     * @return Connection parameters
     */
    public Map<String, Object> getConnectionParams() {
        return connectionParams;
    }

    /**
     * Setter for connection parameters
     *
     * @param connectionParams
     *            Connection parameters
     */
    public void setConnectionParams(Map<String, Object> connectionParams) {
        this.connectionParams = connectionParams;
    }

    /**
     * <p>Setter for the field <code>action</code>.</p>
     *
     * @param onCueOrOnMeta a {@link java.lang.String} object
     */
    public void setAction(String onCueOrOnMeta) {
        this.action = onCueOrOnMeta;
    }

    @Override
    public Notify duplicate() throws IOException, ClassNotFoundException {
        return (Notify) super.duplicate();
    }

    /**
     * <p>Getter for the field <code>action</code>.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getAction() {
        return action;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return call != null ? String.format("%s: %s", getClass().getSimpleName(), call) : (action != null ? String.format("%s action: %s", getClass().getSimpleName(), action) : getClass().getSimpleName());
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof Notify other))
            return false;

        return getTimestamp() == other.getTimestamp() && transactionId == other.transactionId && getType() == other.getType() && Objects.equals(action, other.action) && Objects.equals(connectionParams, other.connectionParams) && Objects.equals(call, other.call);
    }

}
