package com.hedera.demo.auction.app.api;

import com.hedera.demo.auction.app.Utils;
import com.hedera.hashgraph.sdk.PublicKey;
import org.apache.commons.validator.routines.UrlValidator;
import org.jooq.tools.StringUtils;

/**
 * Data class to map incoming REST JSON to a java object
 */
@SuppressWarnings("unused")
public class RequestPostValidator {
    public String operation = "";
    private String name = "";
    public String url = "";
    public String publicKey = "";
    private String nameToUpdate = "";

    public void setName(String name) {
        this.name = Utils.normalize(name);
    }

    public String getName() {
        return name;
    }

    public void setNameToUpdate(String nameToUpdate) {
        this.nameToUpdate = Utils.normalize(nameToUpdate);
    }

    public String getNameToUpdate() {
        return nameToUpdate;
    }

    public String isValid() {
        String valid = "";

        String[] schemes = {"http", "https"};
        UrlValidator urlValidator = new UrlValidator(schemes);

        if (!StringUtils.isEmpty(url) && !urlValidator.isValid(url)) {
            return "invalid validator url";
        }
        switch (operation) {
            case "add":
                if (StringUtils.isEmpty(name)) {
                    return "unable to add, validator object has empty name";
                }
                break;
            case "delete":
                if (StringUtils.isEmpty(name)) {
                    return "unable to delete, validator object has empty name";
                }
                break;
            case "update":
                if (StringUtils.isEmpty(name)) {
                    return "unable to update, validator object has empty name";
                } else if (StringUtils.isEmpty(nameToUpdate)) {
                    return "unable to update, validator object has empty nameToUpdate";
                }
                break;
            default:
                return "validator object has invalid value combinations";
        }

        if (! StringUtils.isEmpty(publicKey)) {
            try {
                PublicKey.fromString(publicKey);
            } catch (@SuppressWarnings("UnusedException") Exception e) {
                return "invalid public key";
            }
        }

        return "";
    }
}
