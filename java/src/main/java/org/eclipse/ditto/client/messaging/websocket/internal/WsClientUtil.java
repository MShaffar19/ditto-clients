/*
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.ditto.client.messaging.websocket.internal;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.annotation.Nullable;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.ditto.client.configuration.ProxyConfiguration;
import org.eclipse.ditto.client.configuration.TrustStoreConfiguration;

import com.neovisionaries.ws.client.ProxySettings;
import com.neovisionaries.ws.client.WebSocketFactory;

/**
 * Util to create an WebSocketFactory from DittoClientConfiguration.
 *
 * @since 1.0.0
 */
final class WsClientUtil {

    private WsClientUtil() {
        throw new AssertionError();
    }

    /**
     * Creates the WebSocketFactory configuration for the passed {@code proxyConfiguration}, {@code
     * trustStoreConfiguration} and {@code clientVersion}.
     *
     * @return the created WebSocketFactory configuration.
     */
    static WebSocketFactory createConfiguration(@Nullable final ProxyConfiguration proxyConfiguration,
            @Nullable final TrustStoreConfiguration trustStoreConfiguration) {

        final WebSocketFactory webSocketFactory = new WebSocketFactory();
        if (proxyConfiguration != null && proxyConfiguration.getHost() != null && proxyConfiguration.getPort() > 0) {
            final ProxySettings proxySettings = webSocketFactory.getProxySettings()
                    .setHost(proxyConfiguration.getHost())
                    .setPort(proxyConfiguration.getPort());
            if (proxyConfiguration.getUsername() != null && proxyConfiguration.getPassword() != null) {
                proxySettings.setCredentials(proxyConfiguration.getUsername(), proxyConfiguration.getPassword());
            }
        }

        if (trustStoreConfiguration != null && trustStoreConfiguration.getLocation() != null) {
            webSocketFactory.setSSLContext(
                    sslContext(trustStoreConfiguration, loadTrustStore(trustStoreConfiguration)));
        }

        return webSocketFactory;
    }

    private static SSLContext sslContext(final TrustStoreConfiguration configuration, final KeyStore trustStore) {
        try {
            final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(trustStore, configuration.getPassword().toCharArray());
            final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
            trustManagerFactory.init(trustStore);

            final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(
                    keyManagerFactory.getKeyManagers(),
                    trustManagerFactory.getTrustManagers(),
                    new SecureRandom());

            return sslContext;
        } catch (final NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException | KeyManagementException e) {
            throw new IllegalStateException("Could not init keymanager", e);
        }
    }

    private static KeyStore loadTrustStore(final TrustStoreConfiguration configuration) {
        try (InputStream keyStoreStream = configuration.getLocation().openStream()) {
            final KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(keyStoreStream, configuration.getPassword().toCharArray());
            return keyStore;
        } catch (CertificateException | NoSuchAlgorithmException | IOException | KeyStoreException e) {
            throw new IllegalStateException("Could not load keystore", e);
        }
    }

}
