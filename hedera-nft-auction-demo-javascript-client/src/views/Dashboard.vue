<template>
  <div>
    <v-overlay :value="busy">
      <v-progress-circular
        v-if="busy"
        indeterminate
        color="primary"
      ></v-progress-circular>
    </v-overlay>
    <v-alert :value="error" dense dismissible type="error">{{
      errorMessage
    }}</v-alert>

    <div v-if="loading">
      <P v-if="loading">Auctions loading, please wait</P>
    </div>
    <div v-else-if="auctions.length !== 0">
      <v-carousel v-model="auctionIndex" hide-delimiters height="100%" :show-arrows=showArrows>
        <v-carousel-item
          v-for="auction in auctions"
          :key=auction.id
        >
          <v-sheet
              height="100%"
              color="white"
              tile
          >
            <v-row
                align="center"
                justify="center"
            >
              <Auction
                :tokenid="auction.tokenid"
                :auctionaccountid="auction.auctionaccountid"
                :reserve="auction.reserve"
                :endtimestamp="auction.endtimestamp"
                :status="auction.status"
                :mirror="mirror"
                :tokenimage="auction.tokenimage"
                :minimumbid="auction.minimumbid"
                :transfertxid="auction.transfertxid"
                :transfertxhash="auction.transfertxhash"
              />
            </v-row>
            <v-row
                align="center"
                justify="center"
            >
              <HighBid
                :status="auction.status"
                :winningaccount="auction.winningaccount"
                :winningtimestamp="auction.winningtimestamp"
                :winningbid="auction.winningbid"
                :accountid="accountId"
                :auctionid="auction.id"
                :auctionaccountid="auction.auctionaccountid"
                :winningtxid="auction.winningtxid"
                :mirror="mirror"
                :winningtxhash="auction.winningtxhash"
              />
            </v-row>
            <v-row
                align="center"
                justify="center"
            >
              <BidHistory :mirror="mirror" :auctionid="auction.id"/>
            </v-row>
          </v-sheet>
        </v-carousel-item>
      </v-carousel>
    </div>
    <div v-else>
      <P>No auctions in progress</P>
    </div>

    <v-footer :color="footerColor" absolute class="font-weight-medium" padless>
      <v-card flat tile width="100%" :class="footerColor">
        <v-card-text :class="textColor">
          <strong>{{ message }}</strong>
        </v-card-text>
      </v-card>
    </v-footer>
  </div>
</template>

<script>
import HighBid from "@/components/HighBid";
import Auction from "../components/Auction";
const {
  Status,
} = require("@hashgraph/sdk");
import BidHistory from "../components/BidHistory";
import {BUSY_EVENT, EventBus, ERROR_NOTIFICATION, FOOTER_NOTIFICATION, MIRROR_SELECTION} from "../eventBus";
import { getAuctions} from "../service/auctions"
import { timeFromSeconds } from "@/utils";

export default {
  name: "Dashboard",
  components: {
    HighBid,
    Auction,
    BidHistory
  },
  computed: {
  },
  data: function() {
    return {
      auctionIndex: -1,
      auctions: null,
      message: "",
      footerColor: "primary",
      textColor: "white--text",
      busy: false,
      loading: true,
      success: false,
      error: false,
      errorMessage: "",
      auctionQuery: false,
      interval: null,
      accountId: undefined,
      account: null,
      mirror: "Kabuto",
      showArrows: false
    };
  },
  methods: {
    timeFromSeconds(timestamp) {
      return timeFromSeconds(timestamp);
    },
  },
  async mounted() {
    this.interval = setInterval(() => {
      if (! this.auctionQuery) {
        this.auctionQuery = true;
        getAuctions().then(refreshedAuctions => {
          this.auctions = [];
          this.message = "";
          this.auctions = refreshedAuctions;
          this.showArrows = (this.auctions.length > 1);
          this.auctionQuery = false;
        })
      }
    }, 2000);
  },
  async created() {

    this.auctions = await getAuctions();
    this.loading = false;

    EventBus.$on(MIRROR_SELECTION, mirror => {
      this.mirror = mirror;
    });

    EventBus.$on(BUSY_EVENT, busy => {
      this.busy = busy;
    });
    EventBus.$on(ERROR_NOTIFICATION, error => {
      this.error = true;
      this.errorMessage = error;
    });
    EventBus.$on(FOOTER_NOTIFICATION, message => {
      this.message = message;
    });

  },
  beforeDestroy() {
    clearInterval(this.interval);
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
