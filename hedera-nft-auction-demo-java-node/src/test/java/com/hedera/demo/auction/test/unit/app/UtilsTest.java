package com.hedera.demo.auction.test.unit.app;

import com.hedera.demo.auction.app.Utils;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PfxOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UtilsTest {

    @Test
    public void testHttpServerOptionsJKS() {
        String certificate = "certificate.jks";
        String key = "server-key";

        JsonObject config = new JsonObject()
                .put("server-key-pass", key)
                .put("server-certificate", certificate);

        HttpServerOptions httpServerOptions = Utils.httpServerOptions(config);

        assertTrue(httpServerOptions.isSsl());
        assertNotNull(httpServerOptions.getKeyStoreOptions());
        JksOptions jksOptions = httpServerOptions.getKeyStoreOptions();
        assertEquals(certificate, jksOptions.getPath());
        assertEquals(key, jksOptions.getPassword());
    }

    @Test
    public void testHttpServerOptionsPFX() {
        String certificate = "certificate.pfx";
        String key = "server-key";

        JsonObject config = new JsonObject()
                .put("server-key-pass", key)
                .put("server-certificate", certificate);

        HttpServerOptions httpServerOptions = Utils.httpServerOptions(config);

        assertTrue(httpServerOptions.isSsl());
        assertNotNull(httpServerOptions.getPfxKeyCertOptions());
        PfxOptions pfxOptions = httpServerOptions.getPfxKeyCertOptions();
        assertEquals(certificate, pfxOptions.getPath());
        assertEquals(key, pfxOptions.getPassword());
    }

    @Test
    public void testHttpServerOptionsP12() {
        String certificate = "certificate.p12";
        String key = "server-key";

        JsonObject config = new JsonObject()
                .put("server-key-pass", key)
                .put("server-certificate", certificate);

        HttpServerOptions httpServerOptions = Utils.httpServerOptions(config);

        assertTrue(httpServerOptions.isSsl());
        assertNotNull(httpServerOptions.getPfxKeyCertOptions());
        PfxOptions pfxOptions = httpServerOptions.getPfxKeyCertOptions();
        assertEquals(certificate, pfxOptions.getPath());
        assertEquals(key, pfxOptions.getPassword());
    }

    @Test
    public void testHttpServerOptionsPEM() {
        String certificate = "certificate.pem";
        String key = "server-key";

        JsonObject config = new JsonObject()
                .put("server-key-pass", key)
                .put("server-certificate", certificate);

        HttpServerOptions httpServerOptions = Utils.httpServerOptions(config);

        assertTrue(httpServerOptions.isSsl());
        assertNotNull(httpServerOptions.getPemKeyCertOptions());
        PemKeyCertOptions pemKeyCertOptions = httpServerOptions.getPemKeyCertOptions();
        assertEquals(certificate, pemKeyCertOptions.getCertPath());
        assertEquals(key, pemKeyCertOptions.getKeyPath());
    }
}
