package com.hedera.demo.auction.app.api;

import org.jooq.tools.StringUtils;

/**
 * Data class to map incoming REST JSON to a java object
 */
@SuppressWarnings("unused")
public class RequestPostValidators {
    private RequestPostValidator[] validators = new RequestPostValidator[0];

    public void setValidators(RequestPostValidator[] validators) {
        this.validators = validators;
    }

    public RequestPostValidator[] getValidators() {
        return validators;
    }

    /**
     * loop through all the validators in the list and check for
     * validity. Return validation error string on first error found.
     *
     * @return String the reason a validator payload failed validation
     */
    public String isValid() {
        for (RequestPostValidator requestPostValidator : validators) {
            String valid = requestPostValidator.isValid();
            if (!StringUtils.isEmpty(valid)) {
                return valid;
            }
        }
        return "";
    }
}
