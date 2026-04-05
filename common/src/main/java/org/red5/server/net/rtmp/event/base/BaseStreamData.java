package org.red5.server.net.rtmp.event.base;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.net.rtmp.event.BaseEvent;
import org.red5.server.stream.IStreamData;

import java.io.*;

public abstract class BaseStreamData extends BaseEvent implements IStreamData<BaseStreamData>, IStreamPacket {

    protected IoBuffer data;

    protected BaseStreamData() {
        super(Type.STREAM_DATA);
    }

    public BaseStreamData(Type type) {
        super(type);
    }

    public BaseStreamData(Type type, IoBuffer data) {
        super(type);
        this.data = data;
    }

    @Override
    public IoBuffer getData() {
        return data;
    }

    @SuppressWarnings("unchecked")
    @Override
    public BaseStreamData duplicate() throws IOException, ClassNotFoundException {
        // 1. Sérialisation en mémoire
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            this.writeExternal(oos);
        }

        // 2. Désérialisation pour créer la copie
        byte[] buf = baos.toByteArray();
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(buf))) {
            // On instancie dynamiquement la classe actuelle
            BaseStreamData result = this.getClass().getDeclaredConstructor().newInstance();
            result.readExternal(ois);

            // 3. Copie des métadonnées héritées de BaseEvent
            if (header != null) {
                result.setHeader(header.clone());
            }
            result.setSourceType(sourceType);
            result.setSource(source);
            result.setTimestamp(timestamp);
            return result;
        } catch (Exception e) {
            throw new IOException("Erreur lors de la duplication du flux", e);
        }
    }

    @Override
    public abstract byte getDataType();

    @Override
    protected void releaseInternal() {
        if (data != null) {
            data.free();
            data = null;
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        if (data != null) {
            byte[] array = new byte[data.remaining()];
            data.mark();
            data.get(array);
            data.reset();
            out.writeObject(array);
        } else {
            out.writeObject(null);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        byte[] byteBuf = (byte[]) in.readObject();
        if (byteBuf != null) {
            this.data = IoBuffer.wrap(byteBuf);
        }
    }

}
