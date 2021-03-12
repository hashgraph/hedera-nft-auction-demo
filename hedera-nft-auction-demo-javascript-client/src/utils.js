import { EventBus } from "./eventBus";

export function notifySuccess(message) {
  notify(true, message);
}

export function timeFromSeconds(timestamp) {
  const seconds = timestamp.substr(0, timestamp.indexOf("."));
  return new Date(seconds * 1000).toLocaleDateString().concat(" at ").concat(new Date(seconds * 1000).toLocaleTimeString());
}

export function getMirrorURL(type, mirror, id) {
  const network = process.env.VUE_APP_NETWORK.toUpperCase();
  if (mirror.toUpperCase() === "KABUTO") {
    const localNetwork = (network === "MAINNET") ? "mainnet" : "testnet";
    if (type === "transactions") {
      return "https://explorer.kabuto.sh/".concat(localNetwork).concat("/transaction/").concat(id);
    } else {
      return "https://explorer.kabuto.sh/".concat(localNetwork).concat("/id/").concat(id);
    }
  } else {
    let transformId = id;
    if ((typeof (transformId) !== "undefined") && (type === "transactions") && (transformId !== "")) {
      transformId = transformId.replace(/\./g, "");
      transformId = transformId.replace("-", "");
      let left = "";
      let right = "";
      if (transformId.indexOf("@") > 0) {
        left = transformId.substr(0, transformId.indexOf("@"));
        right = transformId.substr(transformId.indexOf("@") + 1);
      } else {
        left = transformId.substr(0, transformId.indexOf("-"));
        right = transformId.substr(transformId.indexOf("-") + 1);
      }

      while (right.charAt(0) === "0") {
        right = right.substr(1);
      }
      transformId = left.concat(right);
    }
    if (network === "MAINNET") {
      return "https://app.dragonglass.me/hedera/".concat(type).concat("/").concat(transformId);
    } else if (network === "TESTNET") {
      return "https://testnet.dragonglass.me/hedera/".concat(type).concat("/").concat(transformId);
    } else {
      return "";
    }
  }
}

export function getTopicURL (mirror, topicId) {
  return getMirrorURL("topics", mirror, topicId);
}

export function getAccountUrl (mirror, accountId) {
  return getMirrorURL("accounts", mirror, accountId);
}

export function getTokenUrl (mirror, tokenId) {
  return getMirrorURL("tokens", mirror, tokenId);
}

export function getTransactionURL (mirror, transactionId, transactionhash) {
  if (mirror.toUpperCase() === "KABUTO") {
    return getMirrorURL("transactions", mirror, transactionhash);
  } else {
    return getMirrorURL("transactions", mirror, transactionId);
  }
}

export function notifyError(message) {
  notify(false, message);
}

function notify(status, message) {
  EventBus.$emit("notify", {
    status: status,
    message: message
  });
}

export function secondsToParts(seconds) {
  const secondsInMonth = 30 * 24 * 60 * 60;
  const secondsInDay = 24 * 60 * 60;
  const secondsInHour = 60 * 60;

  const months = seconds / secondsInMonth;
  seconds = seconds % secondsInMonth;
  const days = seconds / secondsInDay;
  seconds = seconds % secondsInDay;
  const hours = seconds / secondsInHour;
  seconds = seconds % secondsInHour;
  const minutes = seconds / 60;
  seconds = seconds % 60;

  let result = months + " months ";
  if (days + hours + minutes + seconds != 0) {
    result += days + " days ";
    if (hours + minutes + seconds != 0) {
      result += hours + " hours ";
      if (minutes + seconds != 0) {
        result += minutes + " minutes ";
        if (seconds != 0) {
          result += seconds + " seconds ";
        }
      }
    }
  }
  return result;
}
