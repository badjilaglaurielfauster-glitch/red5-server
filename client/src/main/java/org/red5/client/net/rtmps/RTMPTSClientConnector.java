/*
 * RED5 Open Source Flash Server - https://github.com/Red5/ Copyright 2006-2015 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.client.net.rtmps;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.util.EntityUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.client.net.rtmp.OutboundHandshake;
import org.red5.client.net.rtmp.RTMPClientConnManager;
import org.red5.client.net.rtmpt.RTMPTClientConnection;
import org.red5.client.net.rtmpt.RTMPTClientConnector;
import org.red5.server.api.Red5;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.util.HttpConnectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client connector for RTMPT/S (RTMPS Tunneled)
 *
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class RTMPTSClientConnector extends RTMPTClientConnector {

    private static final Logger log = LoggerFactory.getLogger(RTMPTSClientConnector.class);

    {
        httpClient = HttpConnectionUtil.getSecureClient();
    }

    /**
     * <p>Constructor for RTMPTSClientConnector.</p>
     *
     * @param server a {@link java.lang.String} object
     * @param port a int
     * @param client a {@link org.red5.client.net.rtmps.RTMPTSClient} object
     */
    public RTMPTSClientConnector(String server, int port, RTMPTSClient client) {
        targetHost = new HttpHost(server, port, "https");
        this.client = client;
    }

}
