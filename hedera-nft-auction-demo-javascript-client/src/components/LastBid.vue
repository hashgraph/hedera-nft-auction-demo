<template>
  <v-card class="mx-auto ma-1" outlined width="650px">
    <v-toolbar dark>
      <v-toolbar-title class="white--text">Your last bid</v-toolbar-title>
    </v-toolbar>
    <v-card-text v-if="bid.bidamount !== null">
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
          <v-col v-if="this.bid.bidamount !== 0">
            <P v-if="txURL">on <a :href="txURL" style="text-decoration: none; color: inherit;" target="_blank"><b>{{ timeFromSeconds(bid.timestamp) }}</b></a></P>
            <P v-else>on <b>{{ timeFromSeconds(bid.timestamp) }}</b></P>
            <div v-if="bid.status === ''">
              <P>Status: Current Leader</P>
            </div>
            <div v-else>
              <P>Status: {{bid.status }}</P>
              <P v-if="bid.refundtxid">
                <a v-if="refundTxURL" :href="refundTxURL" style="text-decoration: none; color: inherit;" target="_blank"><b>Refund transaction</b></a>
                <a v-else style="text-decoration: none; color: inherit;"><b>Refunded</b></a>
              </P>
              <p v-else>Not refunded (yet)</p>
            </div>
          </v-col>
          <v-col v-else>
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
      querying: false,
      interval: null,
      mirror: "Kabuto",
      network: process.env.VUE_APP_NETWORK,
      txURL: "",
      refundTxURL: "",
      bid: {"bidamount": 0}
    }
  },
  async mounted() {
    this.interval = setInterval(() => {
      if (this.accountid) {
        if (!this.querying) {
          this.querying = true;
          getLastBid(this.auctionid, this.accountid).then(bid => {
            this.querying = false;
            this.bid = bid;
            this.setUrls();
          });
        }
      }
    }, 2000);
  },
  methods: {
    timeFromSeconds(timestamp) {
      if ((timestamp) && (timestamp !== null)) {
        return timeFromSeconds(timestamp);
      } else {
        return "";
      }
    },
    setUrls() {
      if (this.bid) {
        this.txURL = getTransactionURL(this.mirror, this.bid.transactionid, this.bid.transactionhash);
        this.refundTxURL = getTransactionURL(this.mirror, this.bid.refundtxid, this.bid.refundtxhash);
      } else {
        this.txURL = "";
        this.refundTxURL = "";
      }
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
      if (this.bid) {
        if (this.bid.bidamount === null) {
          return "";
        } else {
          return Hbar.fromTinybars(this.bid.bidamount);
        }
      } else {
        return "";
      }
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
