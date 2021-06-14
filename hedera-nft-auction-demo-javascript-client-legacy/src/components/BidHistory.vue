<template>
  <v-row justify="space-around">
    <v-col cols="auto">
      <v-card class="mx-auto" outlined width="650px">
        <v-toolbar dark>
          <v-toolbar-title class="white--text">Recent bids</v-toolbar-title>
        </v-toolbar>
        <v-data-table
          :headers="headers"
          :items="bids"
          class="elevation-1"
          hide-default-footer
          height="600px"
          fixed-header
        >
          <template v-slot:item.timestamp="{ item }">
            <a
              :href="transactionURL(item.transactionid, item.transactionhash)"
              style="text-decoration: none; color: inherit;"
              target="_blank"
              ><b>{{ timeFromSeconds(item.timestamp) }}</b></a
            >
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
            <a
              :href="accountURL(item.bidderaccountid)"
              style="text-decoration: none; color: inherit;"
              target="_blank"
              ><b>{{ item.bidderaccountid }}</b></a
            >
          </template>
          <template v-slot:item.status="{ item }">
            <div v-if="item.status == ''">Current leader</div>
            <div v-else>{{ item.status }}</div>
          </template>
          <template v-slot:item.refundtxid="{ item }">
            <div v-if="item.refundtxid !== ''">
              <v-chip color="green">
                <a
                  :href="transactionURL(item.refundtxid, item.refundtxhash)"
                  style="text-decoration: none; color: inherit;"
                  target="_blank"
                  ><b>Yes</b></a
                >
              </v-chip>
            </div>
            <div v-else-if="item.status !== ''">
              <v-chip color="yellow">
                Not yet
              </v-chip>
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
    </v-col>
  </v-row>
</template>

<script>
import { getBids } from "@/service/bids";
import { getAccountUrl, getTransactionURL, timeFromSeconds } from "@/utils";
const { Hbar } = require("@hashgraph/sdk");

export default {
  name: "BidHistory",
  props: ["mirror", "auctionid"],
  data: function() {
    return {
      network: process.env.VUE_APP_NETWORK,
      loadingBids: false,
      interval: null,
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
      bids: []
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
    this.showBids();
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
