import Vue from "vue";

export const EventBus = new Vue();

export const BUSY_EVENT = "busy";
export const SHOW_BID_DLG_EVENT = "bid";
export const FOOTER_NOTIFICATION = "showMessage";
export const ERROR_NOTIFICATION = "showError";
export const SENDBID = "sendbid";
export const MIRROR_SELECTION = "mirrorSelection";
export const SHOW_BID_HISTORY = "showBidHistory";
