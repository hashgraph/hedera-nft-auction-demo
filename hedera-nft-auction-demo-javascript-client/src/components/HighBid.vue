<template>
  <div>
<!--    <div v-if="status !== 'PENDING'">-->
      <div v-if="status !== 'TESTING'">
      <v-card class="ma-1" outlined width="650px">
        <v-toolbar dark>
          <v-toolbar-title v-if="status !== 'PENDING'" class="white--text">Highest Bid</v-toolbar-title>
        </v-toolbar>
        <v-card-text>
          <v-sheet
            elevation="1"
            height="100%"
            width="100%"
            outlined
            rounded
          >
            <v-row align="center">
              <v-col>
                <p class="ma-6 display-1 font-weight-bold text--white">
                  {{ winningBidText }}
                </p>
              </v-col>
              <v-col>
                <div v-if="winningaccount">
                  <P v-if="accountURL">by <a :href="accountURL" style="text-decoration: none; color: inherit;" target="_blank"><b>{{ winningaccount }}</b></a></P>
                  <P v-else>by <b>{{ winningaccount }}</b></P>
                  <P v-if="winningTxURL">on <a :href="winningTxURL" style="text-decoration: none; color: inherit;" target="_blank"><b>{{ timeFromSeconds(winningtimestamp) }}</b></a></P>
                  <P v-else>on <b>{{ timeFromSeconds(winningtimestamp) }}</b></P>
                </div>
                <div v-else>
                  <p class="ma-6 display-1 font-weight-bold text--white">
                    No valid bid
                  </P>
                </div>
              </v-col>
            </v-row>
          </v-sheet>

          <div
              v-if="accountid"
          >
            <v-card-actions>
              <v-btn
                text
                class="ma-2"
                color="green accent-4"
                @click="revealBids()"
              >
                Show history
              </v-btn>
              <v-spacer></v-spacer>
              <v-btn
                class="ma-2"
                color="green accent-4"
                @click="showBidDialog()"
                v-if="status !== 'CLOSED'"
              >
                Bid
              </v-btn>

            </v-card-actions>
          </div>
          <div v-else>
            <a :href=extensionURL target="_blank">Please install and setup this browser extension (desktop only) and reload this page in order to bid</a>
          </div>
        </v-card-text>
      </v-card>
    </div>
    <div v-else>
      <v-card class="ma-2" outlined width="650px">
        <v-card-text>
          <v-sheet
            elevation="1"
            height="100%"
            width="100%"
            outlined
            rounded
          >
            <p class="ma-6 display-1 font-weight-bold text--white">
              This auction has not started yet
            </p>
          </v-sheet>

          <div v-if="! accountid">
            <a :href=extensionURL target="_blank">You may install and setup this browser extension (desktop only) in the mean time in order to bid later</a>
          </div>
        </v-card-text>
      </v-card>
    </div>
  </div>
</template>

<script>
import {getAccountUrl
  , getTransactionURL, timeFromSeconds} from "@/utils";
import {EventBus, SHOW_BID_DLG_EVENT, SHOW_BID_HISTORY} from "@/eventBus";
const { Hbar } = require("@hashgraph/sdk");

export default {
  name: "HighBid",
  props: ['status','winningaccount','winningtimestamp','winningbid','accountid','auctionid','auctionaccountid','winningtxid','mirror','winningtxhash'],
  data: function () {
    return {
      extensionURL: process.env.VUE_APP_BROWSER_EXTENSION_URL,
      network: process.env.VUE_APP_NETWORK,
    }
  },
  methods: {
    timeFromSeconds(timestamp) {
      return timeFromSeconds(timestamp);
    },
    showBidDialog() {
      const bid = {"bid": this.winningbid / 100000000, "auctionaccountid": this.auctionaccountid};
      EventBus.$emit(SHOW_BID_DLG_EVENT, bid);
    },
    revealBids() {
      EventBus.$emit(SHOW_BID_HISTORY, this.auctionid);
    }
  },
  computed: {
    winningBidText() {
      return Hbar.fromTinybars(this.winningbid);
    },
    accountURL() {
      return getAccountUrl(this.mirror, this.winningaccount);
    },
    winningTxURL() {
      return getTransactionURL(this.mirror, this.winningtxid, this.winningtxhash);
    }
  },
};
</script>
<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
h3 {
  margin: 40px 0 0;
}

ul {
  list-style-type: none;
  padding: 0;
}

li {
  display: inline-block;
  margin: 0 10px;
}

a {
  color: #42b983;
}

</style>
