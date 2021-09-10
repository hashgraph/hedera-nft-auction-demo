package com.hedera.demo.auction.app.api;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.domain.Validator;
import com.hedera.demo.auction.app.repository.ValidatorsRepository;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Returns environment variables for use by the UI
 */
@Log4j2
public class GetEnvironmentHandler implements Handler<RoutingContext>  {

    private final ValidatorsRepository validatorsRepository;
    private final String network;
    private final String topicId;
    private final String nodeOperator;

    /**
     * Constructor
     * @param validatorsRepository the repository to use
     * @param network the network to use
     * @param topicId the topic id to use
     * @param nodeOperator the name of the node's operator
     */
    GetEnvironmentHandler(ValidatorsRepository validatorsRepository, String network, String topicId, String nodeOperator) {
        this.validatorsRepository = validatorsRepository;
        this.network = network;
        this.topicId = topicId;
        this.nodeOperator = nodeOperator;
    }

    /**
     * Creates a JSON response containing the environment data
     *
     * @param routingContext the RoutingContext
     */
    @Override
    public void handle(RoutingContext routingContext) {

        log.debug("handle environment");
        JsonObject response = new JsonObject();
        response.put("network", this.network);
        response.put("topicId", this.topicId);
        response.put("nodeOperator", this.nodeOperator);

        log.debug("getting validators");
        @Var List<Validator> validatorList = new ArrayList<>();
        log.debug("got validators");
        try {
            log.debug("preparing response");
            validatorList = validatorsRepository.getValidatorsList();
            response.put("validators", validatorList);

            log.debug("responding");
            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodeToBuffer(response));
        } catch (SQLException e) {
            log.error(e, e);
            routingContext.fail(500, e);
        }
    }
}
