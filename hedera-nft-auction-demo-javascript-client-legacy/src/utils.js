import { EventBus } from "./eventBus";

export function notifySuccess(message) {
  notify(true, message);
}

export function timeFromSeconds(timestamp) {
  const seconds = timestamp.substr(0, timestamp.indexOf("."));
  return new Date(seconds * 1000)
    .toLocaleDateString()
    .concat(" at ")
    .concat(new Date(seconds * 1000).toLocaleTimeString());
}

export function getMirrorURL(type, mirror, id, network) {
  if (network === "PREVIEWNET") {
    return "";
  }
  if (mirror.toUpperCase() === "KABUTO") {
    const localNetwork = network === "MAINNET" ? "mainnet" : "testnet";
    if (type === "transactions") {
      return "https://explorer.kabuto.sh/".concat(localNetwork)
        .concat("/transaction/")
        .concat(id);
    } else {
      return "https://explorer.kabuto.sh/".concat(localNetwork)
        .concat("/id/")
        .concat(id);
    }
  } else {
    let transformId = id;
    if (
      typeof transformId !== "undefined" &&
      type === "transactions" &&
      transformId !== ""
    ) {
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

      if (right === "000000000") {
        right = "0";
      } else {
        while (right.charAt(0) === "0") {
          right = right.substr(1);
        }
      }

      transformId = left.concat(right);
    }
    //0.0.1865125-1623235870-000000000
    //0018651251623235870-000000000
    //https://testnet.dragonglass.me/hedera/transactions/0018651251623235870
    // transformId = transformId.replace('','0')
    if (network === "MAINNET") {
      return "https://app.dragonglass.me/hedera/".concat(type)
        .concat("/")
        .concat(transformId);
    } else {
      return "https://testnet.dragonglass.me/hedera/".concat(type)
        .concat("/")
        .concat(transformId);
    }
  }
}

export function getTopicURL(mirror, topicId, network) {
  return getMirrorURL("topics", mirror, topicId, network);
}

export function getAccountUrl(mirror, accountId, network) {
  return getMirrorURL("accounts", mirror, accountId, network);
}

export function getTokenUrl(mirror, tokenId, network) {
  return getMirrorURL("tokens", mirror, tokenId, network);
}

export function getTransactionURL(mirror, transactionId, transactionhash, network) {
  if (mirror.toUpperCase() === "KABUTO") {
    return getMirrorURL("transactions", mirror, transactionhash, network);
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

export function apiEndPoint() {
  let endPoint = window.location.protocol
    .concat("//")
    .concat(window.location.hostname)
    .concat(":");
  let port = process.env.VUE_APP_API_PORT;
  if (typeof port === 'undefined' || port === null || port === "") {
    port = "8081";
  }
  endPoint = endPoint.concat(port).concat("/v1");

  return endPoint;
}
