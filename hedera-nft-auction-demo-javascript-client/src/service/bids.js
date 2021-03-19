import {EventBus, FOOTER_NOTIFICATION} from "@/eventBus";
const axios = require("axios");
import { apiEndPoint} from "../utils";

export async function getLastBid(auctionId, bidderAccountId) {
    let bid = {};
    const url = apiEndPoint().concat('/lastbid/').concat(auctionId).concat("/").concat(bidderAccountId);
    await axios
        .get(url)
        .then(function (response) {
            if (response.status !== 200) {
                const error = 'Error while fetching auctions';
                EventBus.$emit(FOOTER_NOTIFICATION, error);
                console.error(error);
            } else {
                bid = response.data;
            }
        })
        .catch(function (err) {
            EventBus.$emit(FOOTER_NOTIFICATION, err.toString());
            console.error(err);
        });
    return bid;
}

export async function getBids(auctionId) {
    let bids = [];
    const url = apiEndPoint().concat('/bids/').concat(auctionId);
    await axios
        .get(url)
        .then(function (response) {
            if (response.status !== 200) {
                const error = 'Error while fetching auctions';
                EventBus.$emit(FOOTER_NOTIFICATION, error);
                console.error(error);
            } else {
                bids = response.data;
            }
        })
        .catch(function (err) {
            EventBus.$emit(FOOTER_NOTIFICATION, err.toString());
            console.error(err);
        });
    return bids;
}
