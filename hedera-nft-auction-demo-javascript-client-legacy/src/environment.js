import {apiEndPoint} from "@/utils";
import {ENV_DATA, EventBus, FOOTER_NOTIFICATION } from "@/eventBus";
const axios = require("axios");

let envData = {
    network: "",
    topicId: "",
    nodeOperator: ""
};
let loaded = false;

function currentEnvironment() {
    if (! loaded) {
        // load from server
        const url = apiEndPoint().concat("/environment");

        try {
            axios
                .get(url)
                .then(function(response) {
                    if (response.status !== 200) {
                        const error = "Error while fetching auctions";
                        EventBus.$emit(FOOTER_NOTIFICATION, error);
                        console.error(error);
                    } else {
                        envData = response.data;
                        loaded = true;
                        console.log(envData);
                        EventBus.$emit(ENV_DATA, envData);
                    }
                })
                .catch(function(err) {
                    EventBus.$emit(FOOTER_NOTIFICATION, err.toString());
                    console.error(err);
                });
        } catch (e) {
            console.error(e);
        }
    }

    return envData;
};

export function loadEnvironment() {
    let interval = setInterval(() => {
        if (! loaded) {
            currentEnvironment();
        } else {
            console.log("Clearing interval");
            clearInterval(interval);
        }
    }, 5000);
};

EventBus.$emit(ENV_DATA, envData);

