package com.hedera.demo.auction.app.api;

import com.hedera.demo.auction.app.ManageValidator;
import com.hedera.demo.auction.app.Utils;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaParser;
import lombok.extern.log4j.Log4j2;
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
                                .optionalProperty("url", Utils.LONG_STRING_MAX_SCHEMA)
                                .optionalProperty("publicKey", Utils.KEY_STRING_MAX_SCHEMA)
                                .requiredProperty("operation", Utils.OPERATION_STRING_SCHEMA)
                        )
                )
                .build(schemaParser);

        validatorsSchemaBuilder.validateSync(body);

        RequestPostValidators requestPostValidators = body.mapTo(RequestPostValidators.class);
        if (requestPostValidators.getValidators().length == 0) {
            String errorMessage = "empty list of validators";
            log.error(errorMessage);
            routingContext.fail(500, new Exception(errorMessage));
            return;
        }


        String isValid = requestPostValidators.isValid();
        if ( ! StringUtils.isEmpty(isValid)) {
            log.error(isValid);
            routingContext.fail(500, new Exception(isValid));
            return;
        }

        try {
            ManageValidator manageValidator = new ManageValidator();
            manageValidator.manage(body);

            JsonObject response = new JsonObject();
            log.info("validator request submission successful");
            response.put("status", "success");

            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodeToBuffer(response));
        } catch (Exception e) {
            log.error(e, e);
            routingContext.fail(500, e);
        }
    }
}
