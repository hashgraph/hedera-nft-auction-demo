<template>
  <div>
    <v-card class="mx-auto" outlined width="650px">
      <v-toolbar dark>
        <v-toolbar-title class="white--text"
          >Auction for token <a :href="tokenURL" style="text-decoration: none; color: inherit;" target="_blank"><b>{{ tokenid }}</b></a></v-toolbar-title
        >
      </v-toolbar>
      <v-row align="center">
        <v-overlay v-if="status === 'CLOSED'"
            color="red"
            :absolute=true
        >
        </v-overlay>
        <v-overlay v-if="status === 'PENDING'"
                   color="grey"
                   :absolute=true
        >
        </v-overlay>
        <v-col align="center">
          <div class="text-xs-center">
            <v-img v-if="tokenimage" :src="tokenimage" class="ma-2" width="90%"></v-img>
          </div>
        </v-col>
        <v-col align="left">
          <v-row>
            <v-col><a :href="accountURL" style="text-decoration: none; color: inherit;" target="_blank"><b>Auction account: {{ auctionaccountid }}</b></a></v-col>
          </v-row>
          <v-row>
            <v-col>Reserve: {{ reserve }}</v-col>
          </v-row>
          <v-row>
            <v-col>Minimum bid increase: {{ minimumbid }}</v-col>
          </v-row>
          <v-row>
            <v-col>Ends on : {{ timeFromSeconds(endtimestamp) }}</v-col>
          </v-row>
<!--          <v-row>-->
<!--            <v-col><Countdown deadline="August 22, 2022"></Countdown></v-col>-->
<!--          </v-row>-->
          <v-row>
            <v-col>Status : {{ status }}</v-col>
          </v-row>
        </v-col>
      </v-row>
    </v-card>
  </div>
</template>

<script>
import {timeFromSeconds} from "@/utils";
import { getAccountUrl, getTokenUrl } from "@/utils";
// import Countdown from 'vuejs-countdown'
export default {
  name: "Auction",
  props: ['tokenid', 'auctionaccountid', 'reserve', 'endtimestamp', 'status', 'mirror', 'tokenimage', 'minimumbid'],
  // components: {
  //   Countdown
  // },
  data: function() {
    return {
    };
  },
  methods: {
    timeFromSeconds(timestamp) {
      return timeFromSeconds(timestamp);
    },
  },
  computed: {
    accountURL() {
      return getAccountUrl(this.mirror, this.auctionaccountid);
    },
    tokenURL() {
      return getTokenUrl(this.mirror, this.tokenid);
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
