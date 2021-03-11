<template>
  <v-row justify="space-around">
    <v-col cols="auto">
      <v-dialog transition="dialog-bottom-transition" v-model="bidDialog" width="650px">
        <v-card class="mx-auto" outlined width="650px">
          <v-toolbar dark>
            <v-toolbar-title class="white--text">Recent bids</v-toolbar-title>
          </v-toolbar>
          <v-data-table
            :headers="headers"
            :items="bids"
            class="elevation-1"
            hide-default-footer
            :loading="loadingBids"
            loading-text="refreshing bid list"
            height="600px"
            fixed-header
          >
            <template v-slot:item.timestamp="{ item }">
              <a :href="transactionURL(item.transactionid, item.transactionhash)" style="text-decoration: none; color: inherit;" target="_blank"><b>{{ timeFromSeconds(item.timestamp) }}</b></a>
            </template>
            <template v-slot:item.bidamount="{ item }">
              <v-chip v-if="item.status === ''" color="green">
                {{ bidToText(item.bidamount) }}
              </v-chip>
              <v-chip v-else color="orange">
                {{ bidToText(item.bidamount) }}
              </v-chip>
            </template>
            <template v-slot:item.bidderaccountid="{ item }">
              <a :href="accountURL(item.bidderaccountid)" style="text-decoration: none; color: inherit;" target="_blank"><b>{{ item.bidderaccountid }}</b></a>
            </template>
            <template v-slot:item.status="{ item }">
              <div v-if="item.status == ''">Current winner</div>
              <div v-else>{{ item.status }}</div>
            </template>
            <template v-slot:item.refundtxid="{ item }">
              <div v-if="item.refundtxid !== null">
                <a :href="transactionURL(item.refundtxid, item.refundtxhash)" style="text-decoration: none; color: inherit;" target="_blank"><b>Refund Tx</b></a>
              </div>
              <div v-else-if="item.status !== ''">
                Not yet
              </div>
              <div v-else>
                Winner
              </div>
            </template>
          </v-data-table>
          <v-card-actions class="pt-0">
            <v-spacer></v-spacer>
            <v-btn text color="teal accent-4" @click="close">
              Close
            </v-btn>
          </v-card-actions>
        </v-card>
      </v-dialog>
    </v-col>
  </v-row>
</template>

<script>
import { SHOW_BID_HISTORY, EventBus } from "../eventBus";
import {getBids} from "@/service/bids";
import {getAccountUrl, getTransactionURL, timeFromSeconds} from "@/utils";
const { Hbar } = require("@hashgraph/sdk");

export default {
  name: "BidHistory",
  props: ['mirror'],
  data: function() {
    return {
      bidDialog: false,
      network: process.env.VUE_APP_NETWORK,
      loadingBids: false,
      interval: null,
      auctionid: null,
      headers: [
        {
          text: "Timestamp",
          align: "start",
          sortable: false,
          value: "timestamp"
        },
        {
          text: "Bid",
          value: "bidamount",
          align: "center",
          sortable: false
        },
        {
          text: "Account ID",
          value: "bidderaccountid",
          align: "center",
          sortable: false
        },
        {
          text: "Status",
          value: "status",
          align: "center",
          sortable: false
        },
        {
          text: "Refunded",
          value: "refundtxid",
          align: "center",
          sortable: false
        }
      ],
      bids: [],
    };
  },
  methods: {
    accountURL(accountId) {
      return getAccountUrl(this.mirror, accountId);
    },
    transactionURL(transactionId, transactionHash) {
      return getTransactionURL(this.mirror, transactionId, transactionHash);
    },
    close() {
      clearInterval(this.interval);
      this.bidDialog = false;
    },
    timeFromSeconds(timestamp) {
      return timeFromSeconds(timestamp);
    },
    bidToText(amount) {
      return Hbar.fromTinybars(amount);
    },
    showBids() {
      this.loadingBids = true;
      getBids(this.auctionid).then(response => {
        this.bids = response;
        this.loadingBids = false;
      });

      this.interval = setInterval(() => {
        if (!this.loadingBids) {
          this.loadingBids = true;
          getBids(this.auctionid).then(response => {
            this.bids = response;
            this.loadingBids = false;
          });
        }
      }, 5000);
    }
  },
  created() {
    EventBus.$on(SHOW_BID_HISTORY, auctionid => {
      this.auctionid = auctionid;
      this.bidDialog = true;
      this.showBids();
    });
  }
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
