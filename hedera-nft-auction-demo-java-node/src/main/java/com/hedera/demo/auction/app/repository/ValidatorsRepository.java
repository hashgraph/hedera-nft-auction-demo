package com.hedera.demo.auction.app.repository;

import com.hedera.demo.auction.app.SqlConnectionManager;
import com.hedera.demo.auction.app.api.RequestPostValidator;
import com.hedera.demo.auction.app.domain.Validator;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.jooq.tools.StringUtils;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.hedera.demo.auction.app.db.Tables.VALIDATORS;

@Log4j2
public class ValidatorsRepository {
    private final SqlConnectionManager connectionManager;

    public ValidatorsRepository(SqlConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Nullable
    private Result<Record> getValidators () throws SQLException {
        DSLContext cx = connectionManager.dsl();
        Result<Record> rows = cx.selectFrom(VALIDATORS).orderBy(VALIDATORS.NAME).fetch();

        return rows;
    }

    public void deleteAllValidators() throws SQLException {
        DSLContext cx = connectionManager.dsl();
        cx.deleteFrom(VALIDATORS)
                .execute();
    }

    public List<Validator> getValidatorsList() throws SQLException {
        List<Validator> validators = new ArrayList<>();
        Result<Record> validatorsData = getValidators();
        if (validatorsData != null) {
            for (Record record : validatorsData) {
                Validator validator = new Validator(record);
                validators.add(validator);
            }
        }
        return validators;
    }

    public void manage(JsonArray validators) throws Exception {
        DSLContext cx = connectionManager.dsl();
        cx.transaction(configuration -> {
            for (Object validatorObject : validators.getList()) {
                JsonObject validator = JsonObject.mapFrom(validatorObject);
                RequestPostValidator postValidator = validator.mapTo(RequestPostValidator.class);
                if (StringUtils.isEmpty(postValidator.isValid())) {
                    switch (postValidator.operation) {
                        case "add":
                            log.debug("adding validator");
                            DSL.using(configuration).insertInto(VALIDATORS)
                                    .set(VALIDATORS.NAME, postValidator.getName())
                                    .set(VALIDATORS.URL, postValidator.url)
                                    .set(VALIDATORS.PUBLICKEY, postValidator.publicKey)
                                    .execute();
                            break;
                        case "delete":
                            log.debug("deleting validator");
                            cx.delete(VALIDATORS)
                                    .where(VALIDATORS.NAME.eq(postValidator.getName()))
                                    .execute();
                            break;
                        case "update":
                            log.debug("updating validator");
                            cx.update(VALIDATORS)
                                    .set(VALIDATORS.NAME, postValidator.getName())
                                    .set(VALIDATORS.URL, postValidator.url)
                                    .set(VALIDATORS.PUBLICKEY, postValidator.publicKey)
                                    .where(VALIDATORS.NAME.eq(postValidator.getNameToUpdate()))
                                    .execute();
                            break;
                        default:
                            throw new Exception("invalid consensus message contents - validator object has invalid value combinations");
                    }
                } else {
                    throw new Exception("invalid validator definition");
                }
            }

            // Implicit commit executed here
        });
    }
}
