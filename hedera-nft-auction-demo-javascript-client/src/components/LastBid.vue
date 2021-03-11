<template>
  <v-card class="mx-auto" outlined width="650px">
    <v-toolbar dark>
      <v-toolbar-title class="white--text">Your last bid</v-toolbar-title>

    </v-toolbar>
    <v-card-text v-if="lastbid !== 0">
      <v-sheet
          elevation="1"
          height="100%"
          width="100%"
          outlined
          rounded
      >
        <v-row>
          <v-col>
            <p class="ma-6 display-1 font-weight-bold text--white">
              {{ winningBidText }}
            </p>
          </v-col>
          <v-col>
            <P>on <a :href="txURL" style="text-decoration: none; color: inherit;" target="_blank"><b>{{ timeFromSeconds(timestamp) }}</b></a></P>
            <P v-if="refundtxid">on <a :href="refundTxURL" style="text-decoration: none; color: inherit;" target="_blank"><b>Refund transaction</b></a></P>
            <p v-else>Not refunded (yet)</p>
          </v-col>
        </v-row>
      </v-sheet>
    </v-card-text>
    <v-card-text v-else>
      <v-sheet
          elevation="1"
          height="100%"
          width="100%"
          outlined
          rounded
      >
        <div>none found</div>
      </v-sheet>
    </v-card-text>
  </v-card>
</template>

<script>
import { getTransactionURL, timeFromSeconds} from "@/utils";

const { Hbar } = require("@hashgraph/sdk");
import {getLastBid} from "../service/bids";
import {EventBus, MIRROR_SELECTION} from "@/eventBus";
export default {
  name: "LastBid",
  props: ['accountid','auctionid'],
  data: function () {
    return {
      lastbid: 0,
      timestamp: '',
      status: '',
      refundtxid: '',
      txId: '',
      querying: false,
      interval: null,
      mirror: "Kabuto",
      network: process.env.VUE_APP_NETWORK,
      txURL: "",
      refundTxURL: "",
    }
  },
  async mounted() {
    this.interval = setInterval(() => {
      if (this.accountid) {
        if (!this.querying) {
          this.querying = true;
          getLastBid(this.auctionid, this.accountid).then(bid => {
            this.querying = false;
            if (bid.bidamount !== null) {
              this.lastbid = bid.bidamount;
              this.timestamp = bid.timestamp;
              this.status = bid.status;
              this.refundtxid = bid.refundtxid;
              this.txId = bid.winningtxid;
            }
          });
        }
      }
    }, 2000);
  },
  methods: {
    timeFromSeconds(timestamp) {
      return timeFromSeconds(timestamp);
    },
    setUrls() {
      this.txURL = getTransactionURL(this.mirror, this.txId);
      this.refundTxURL = getTransactionURL(this.mirror, this.refundtxid);
    }
  },
  created() {
    EventBus.$on(MIRROR_SELECTION, mirror => {
      this.mirror = mirror;
      this.setUrls();
    });

    this.setUrls();
  },
  computed: {
    winningBidText() {
      return Hbar.fromTinybars(this.lastbid);
    },
  },
  beforeDestroy() {
    clearInterval(this.interval);
  }
}
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
