package com.hedera.demo.auction.test.unit.mirrormapping;

import io.vertx.core.json.JsonObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public abstract class AbstractMirrorMapping {

    protected JsonObject loadJsonFile(String fileName) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        var file = new File(Objects.requireNonNull(classLoader.getResource(fileName)).getFile());
        var jsonData = Files.readString(Paths.get(file.getAbsolutePath()));

        return new JsonObject(jsonData);
    }
}
