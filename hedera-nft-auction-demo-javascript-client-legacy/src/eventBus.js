import Vue from "vue";

export const EventBus = new Vue();

export const BUSY_EVENT = "busy";
export const FOOTER_NOTIFICATION = "showMessage";
export const ERROR_NOTIFICATION = "showError";
export const MIRROR_SELECTION = "mirrorSelection";
export const ENV_DATA = "environmentReady";
