package com.hedera.demo.auction.app.api;

import com.hedera.demo.auction.app.ManageValidator;
import com.hedera.demo.auction.app.Utils;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaParser;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.validator.routines.UrlValidator;
import org.jooq.tools.StringUtils;

import static io.vertx.json.schema.common.dsl.Schemas.arraySchema;
import static io.vertx.json.schema.common.dsl.Schemas.objectSchema;

/**
 * Adds, deletes or modifies a validator
 */
@Log4j2
public class PostValidators implements Handler<RoutingContext> {
    private final SchemaParser schemaParser;
    public PostValidators(SchemaParser schemaParser) {
        this.schemaParser = schemaParser;
    }

    /**
     * Given validator details, add, update or delete validator(s)
     *
     * @param routingContext the RoutingContext
     */
    @Override
    public void handle(RoutingContext routingContext) {
        var body = routingContext.getBodyAsJson();

        Schema validatorsSchemaBuilder = objectSchema()
                .requiredProperty("validators", arraySchema()
                        .items(objectSchema()
                                .requiredProperty("name", Utils.LONG_STRING_MAX_SCHEMA)
                                .optionalProperty("nameToUpdate", Utils.LONG_STRING_MAX_SCHEMA)
                                .property("url", Utils.LONG_STRING_MAX_SCHEMA)
                                .optionalProperty("publicKey", Utils.KEY_STRING_MAX_SCHEMA)
                                .requiredProperty("operation", Utils.OPERATION_STRING_SCHEMA)
                        )
                )
                .build(schemaParser);

        validatorsSchemaBuilder.validateSync(body);

        if (body.containsKey("validators")) {
            JsonArray validators = body.getJsonArray("validators");
            if (validators != null) {
                // check operations and contents are valid
                String[] schemes = {"http","https"};
                UrlValidator urlValidator = new UrlValidator(schemes);

                for (Object validatorObject : validators) {
                    JsonObject validatorJson = JsonObject.mapFrom(validatorObject);
                    if ( ! validatorJson.getString("operation").contains("add")
                            && ! validatorJson.getString("operation").contains("delete")
                            && ! validatorJson.getString("operation").contains("update")) {
                        // one of the operations is invalid
                        String errorMessage = "invalid operation on one of the validators, should be one of add, update or delete";
                        log.error(errorMessage);
                        routingContext.fail(500, new Exception(errorMessage));
                        return;
                    }
                    if (! StringUtils.isEmpty(validatorJson.getString("url", ""))) {
                        if ( ! urlValidator.isValid(validatorJson.getString("url"))) {
                            String errorMessage = "invalid url on one of the validators";
                            log.error(errorMessage);
                            routingContext.fail(500, new Exception(errorMessage));
                            return;
                        }
                    }
                }


                for (Object validatorObject : validators) {
                    JsonObject validatorJson = JsonObject.mapFrom(validatorObject);
                    String[] args = new String[5];

                    args[0] = "--name=".concat(validatorJson.getString("name", ""));
                    args[1] = "--nameToUpdate=".concat(validatorJson.getString("nameToUpdate", ""));
                    args[2] = "--operation=".concat(validatorJson.getString("operation", ""));
                    args[3] = "--url=".concat(validatorJson.getString("url", ""));
                    args[4] = "--publicKey=".concat(validatorJson.getString("publicKey", ""));

                    try {
                        ManageValidator manageValidator = new ManageValidator();
                        manageValidator.manage(args);

                        JsonObject response = new JsonObject();
                        log.info("validator request submission successful");
                        response.put("status", "success");

                        routingContext.response()
                                .putHeader("content-type", "application/json")
                                .end(Json.encodeToBuffer(response));
                    } catch (Exception e) {
                        log.error(e, e);
                        routingContext.fail(500, e);
                        return;
                    }
                }
            } else {
                String errorMessage = "message body does not contain validators array";
                log.error(errorMessage);
                routingContext.fail(500, new Exception(errorMessage));
                return;
            }
        } else {
            String errorMessage = "message body does not contain validators attribute";
            log.error(errorMessage);
            routingContext.fail(500, new Exception(errorMessage));
            return;
        }
    }
}
