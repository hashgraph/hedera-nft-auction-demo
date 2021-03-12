<template>
  <div>
    <v-overlay :value="busy">
      <v-progress-circular
        v-if="busy"
        indeterminate
        color="primary"
      ></v-progress-circular>
    </v-overlay>
    <v-snackbar
        :value="success"
        color="green"
        timeout=3000
    >Bid sent - thank you</v-snackbar>

    <v-alert :value="error" dense dismissible type="error">{{
      errorMessage
    }}</v-alert>
    <Bid/>
    <BidHistory :mirror="mirror"/>

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
              <LastBid
                :auctionid="auction.id"
                :accountid="accountId"
              />
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
import LastBid from "../components/LastBid";
const {
  Client,
  Status,
  TransferTransaction
} = require("@hashgraph/sdk");
import Bid from "../components/Bid";
import BidHistory from "../components/BidHistory";
import {BUSY_EVENT, EventBus, ERROR_NOTIFICATION, SENDBID, FOOTER_NOTIFICATION, MIRROR_SELECTION} from "../eventBus";
import { getAuctions} from "../service/auctions"
import { timeFromSeconds } from "@/utils";

export default {
  name: "Dashboard",
  components: {
    LastBid,
    HighBid,
    Auction,
    Bid,
    BidHistory
  },
  computed: {
  },
  data: function() {
    return {
      confetti: false,
      auctionIndex: -1,
      auctions: null,
      message: "",
      footerColor: "primary",
      textColor: "white--text",
      busy: false,
      loading: true,
      wallet: null,
      provider: null,
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
    confettiOnOff() {
      if (typeof (this.accountId) == "undefined") {
        this.$confetti.stop();
      } else {
        if ((this.auctions.length > 0) && (this.auctionIndex !== -1)) {
          if (this.auctions[this.auctionIndex].winningaccount === this.accountId) {
            this.startConfetti({
              particlesPerFrame: 0.1,
            });
          } else {
            this.stopConfetti();
          }
        } else {
          this.stopConfetti();
        }
      }
    },
    startConfetti() {
      if ( ! this.confetti) {
        this.confetti = true;
        this.$confetti.start();
      }
    },
    stopConfetti() {
      if (this.confetti) {
        this.confetti = false;
        this.$confetti.stop();
      }
    },
    timeFromSeconds(timestamp) {
      return timeFromSeconds(timestamp);
    },
    async sendBid(bid, auctionAccountId) {
      this.busy = true;
      try {
        this.message = "Preparing Bid Transaction";
        let client;
        if (process.env.VUE_APP_NETWORK.toUpperCase() === 'MAINNET') {
          client = Client.forMainnet();
        } else {
          client = Client.forTestnet();
        }
        if (this.account) {
          client.setOperatorWith(this.account.id, this.account.publicKey, this.provider);
          this.message = "Signing Bid Transaction";
          const toExecute = await new TransferTransaction()
            .addHbarTransfer(this.account.id, -bid)
            .addHbarTransfer(auctionAccountId, bid)
            .freezeWith(client)
            .signWithOperator(client).catch(err => {
              this.message = err.message
              return null;
            });

          if (toExecute) {
            this.message = "Sending Bid Transaction to Hedera";
            const executed = await toExecute.execute(client);

            // reset client so receipt request doesn't prompt for signature
            this.message = "Fetching Receipt";
            if (process.env.VUE_APP_NETWORK.toUpperCase() === 'MAINNET') {
              client = Client.forMainnet();
            } else {
              client = Client.forTestnet();
            }
            const receipt = await executed.getReceipt(client);
            if (receipt.status == Status.Success) {
              this.success = true;
              setTimeout(() => (this.success = false), 2000);
            } else {
              this.error = true;
              this.errorMessage = receipt.status.toString();
            }
            this.message = "";
          }
        } else {
          this.message = "Unable to login with extension - bid aborted";
        }
      } catch (e) {
        this.message = e.toString();
      }
      this.busy = false;
    }
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
          this.confettiOnOff();
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
    EventBus.$on(SENDBID, bid => {
      this.sendBid(bid.bid, bid.auctionaccountid);
    });
    EventBus.$on(ERROR_NOTIFICATION, error => {
      this.error = true;
      this.errorMessage = error;
    });
    EventBus.$on(FOOTER_NOTIFICATION, message => {
      this.message = message;
    });

    if (this.wallet === null) {
      document.addEventListener("hederaWalletLoaded", async () => {
        this.wallet = window.wallet;
        this.provider = this.wallet.getTransactionSigner();

        try {
          this.account = await this.wallet.login(process.env.VUE_APP_NETWORK);
          this.accountId = this.account.id;
        } catch (e) {
          this.message = "Unable to login with extension - ".concat(e.message);
          this.accountId = undefined;
        }

      });
    }
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
