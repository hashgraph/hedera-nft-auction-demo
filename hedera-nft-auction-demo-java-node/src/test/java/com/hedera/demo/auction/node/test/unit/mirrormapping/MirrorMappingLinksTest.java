package com.hedera.demo.auction.node.test.unit.mirrormapping;

import com.hedera.demo.auction.node.mirrormapping.MirrorLinks;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MirrorMappingLinksTest {

    String link = "link";

    @Test
    public void testMirrorMappingLinksHedera() {

        JsonObject links = new JsonObject();
        links.put("next", link);

        MirrorLinks mirrorLinks = links.mapTo(MirrorLinks.class);

        assertEquals(link, mirrorLinks.getNext());
    }

    @Test
    public void testMirrorMappingLinksKabuto() {

        // TODO: Match Kabuto's format
        JsonObject links = new JsonObject();
        links.put("next", link);

        MirrorLinks mirrorLinks = links.mapTo(MirrorLinks.class);

        assertEquals(link, mirrorLinks.getNext());
    }

    @Test
    public void testMirrorMappingLinksDragonglass() {

        // TODO: Match Dragonglass's format
        JsonObject links = new JsonObject();
        links.put("next", link);

        MirrorLinks mirrorLinks = links.mapTo(MirrorLinks.class);

        assertEquals(link, mirrorLinks.getNext());
    }

    @Test
    public void testMirrorMappingLinksExtraData() {

        JsonObject links = new JsonObject();
        links.put("next", link);
        links.put("dummy", "dummy");

        assertDoesNotThrow(() -> links.mapTo(MirrorLinks.class));
    }

}
