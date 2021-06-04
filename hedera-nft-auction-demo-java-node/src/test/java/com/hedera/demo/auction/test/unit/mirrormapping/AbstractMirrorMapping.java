package com.hedera.demo.auction.test.unit.mirrormapping;

import io.vertx.core.json.JsonObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class AbstractMirrorMapping {

    protected JsonObject loadJsonFile(String fileName) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());
        String jsonData = new String (Files.readAllBytes(Paths.get(file.getAbsolutePath())), UTF_8);
        JsonObject jsonObject = new JsonObject(jsonData);

        return jsonObject;
    }
}
