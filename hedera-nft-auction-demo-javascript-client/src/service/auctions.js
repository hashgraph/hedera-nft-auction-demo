import {EventBus, FOOTER_NOTIFICATION} from "@/eventBus";
const apiEndPoint = process.env.VUE_APP_API_URL;
const axios = require("axios");

export async function getAuctions() {
    let auctions = [];
    const url = apiEndPoint.concat('/auctions');
    await axios
        .get(url)
        .then(function (response) {
            if (response.status !== 200) {
                const error = 'Error while fetching auctions';
                EventBus.$emit(FOOTER_NOTIFICATION, error);
                console.error(error);
            } else {
                auctions = response.data;
            }
        })
        .catch(function (err) {
            EventBus.$emit(FOOTER_NOTIFICATION, err.toString());
            console.error(err);
        });
    return auctions;
}

export async function getAuction(id) {
    let auction = {};
    const url = apiEndPoint.concat('/auctions/').concat(id);
    await axios
        .get(url)
        .then(function (response) {
            if (response.status !== 200) {
                const error = 'Error while fetching auctions';
                EventBus.$emit(FOOTER_NOTIFICATION, error);
                console.error(error);
            } else {
                auction = response.data;
            }
        })
        .catch(function (err) {
            EventBus.$emit(FOOTER_NOTIFICATION, err.toString());
            console.error(err);
        });
    return auction;
}
