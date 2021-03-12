<template>
  <v-dialog v-model="bidDialog" width="300">
    <v-card class="mx-auto" outlined width="650px">
      <v-card-text>
        <P>A winning bid is a commitment to complete the token purchase.</P>
        <v-row justify="center">
          <v-col>
            <v-form ref="decimalsForm" v-model="bidValid">
              <v-text-field
                v-model="bidAmount"
                :rules="integerRules"
                clearable
                hint="input your bid in hbar"
                label="Your Bid (in hbar)"
              ></v-text-field>
            </v-form>
          </v-col>
        </v-row>
        <P>The specified amount of hbar will be transferred to the auction account and refunded if your bid is too low or does not meet the bidding requirements</P>
      </v-card-text>
      <v-card-actions>
        <v-btn
            class="ma-2"
            text
            color="red"
            @click="bidDialog = false"
        >
          Cancel
        </v-btn>
        <v-spacer></v-spacer>
        <v-btn
            class="ma-2"
            color="green accent-4"
            @click="bid"
        >
          Bid
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
import { SHOW_BID_DLG_EVENT, EventBus, SENDBID } from "../eventBus";

export default {
  name: "Bid",
  data: function() {
    return {
      auction: {},
      integerRules: [
        v => (v == parseInt(v) && v > 0) || "Integer greater than 0 required"
      ],
      bidValid: false,
      bidAmount: 0,
      auctionAccountId: "",
      bidDialog: false,
    };
  },
  methods: {
    bid() {
      const bid = {"bid":this.bidAmount, "auctionaccountid": this.auctionAccountId};
      EventBus.$emit(SENDBID, bid);
      this.bidDialog = false;
    }
  },
  created() {
    EventBus.$on(SHOW_BID_DLG_EVENT, bidRequest => {
      this.bidAmount = bidRequest.bid;
      this.auctionAccountId = bidRequest.auctionaccountid;
      this.bidDialog = true;
    });

    if (this.wallet === null) {
      document.addEventListener("hederaWalletLoaded", async () => {
        this.wallet = window.wallet;
        this.provider = this.wallet.getTransactionSigner();
      });
    }
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
