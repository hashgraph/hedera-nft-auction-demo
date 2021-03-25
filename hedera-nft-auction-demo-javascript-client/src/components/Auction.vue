<template>
  <div>
    <v-card :color=cardColor class="mx-auto ma-1" outlined width="650px">
      <v-toolbar :color=cardColor dark>
        <v-toolbar-title v-if="tokenURL" class="white--text">Auction for token <a :href="tokenURL" style="text-decoration: none; color: inherit;" target="_blank"><b>{{ tokenid }}</b></a></v-toolbar-title>
        <v-toolbar-title v-else class="white--text">Auction for token <b>{{ tokenid }}</b></v-toolbar-title>
      </v-toolbar>
      <v-row  align="center" v-if="status === 'ACTIVE'">
        <flip-countdown  class="ma-2" :deadline="timeFromSeconds(endtimestamp)"></flip-countdown>
      </v-row>
      <v-row align="center" class="ma-2">
        <v-col align="center">
          <div class="text-xs-center">
            <v-img v-if="tokenimage" :src="tokenimage" class="ma-2" width="90%"></v-img>
          </div>
        </v-col>
        <v-col align="left">
          <v-row>
            <v-col>
              <a v-if="accountURL" :href="accountURL" style="text-decoration: none; color: inherit;" target="_blank"><b>Auction account: {{ auctionaccountid }}</b></a>
              <a v-else style="text-decoration: none; color: inherit;">Auction account: <b>{{ auctionaccountid }}</b></a>
            </v-col>
          </v-row>
          <v-row>
            <v-col>Reserve: {{ reserve }}</v-col>
          </v-row>
            <v-row>
                <v-col>Minimum bid increase: {{ minimumbid }}</v-col>
            </v-row>
            <v-row>
                <v-col>{{ auctionStatusText }}</v-col>
            </v-row>

        </v-col>
      </v-row>
    </v-card>
  </div>
</template>

<script>
import { getAccountUrl, getTokenUrl } from "@/utils";
import FlipCountdown from 'vue2-flip-countdown'

export default {
  name: "Auction",
  props: ['tokenid', 'auctionaccountid', 'reserve', 'endtimestamp', 'status', 'mirror', 'tokenimage', 'minimumbid'],
  components: {
    FlipCountdown
  },
  data: function() {
    return {
    };
  },
  methods: {
    timeFromSeconds(timestamp) {
      const seconds = timestamp.substr(0, timestamp.indexOf("."));
      const endDate = new Date(seconds * 1000);

      const timeToEnd = endDate.getFullYear() + "-" + (endDate.getMonth()+1) + "-" + endDate.getDate()
        + " " + endDate.getHours() + ":" + endDate.getMinutes() + ":"  + endDate.getSeconds();

      return timeToEnd;
    },
  },
  computed: {
    accountURL() {
      return getAccountUrl(this.mirror, this.auctionaccountid);
    },
    tokenURL() {
      return getTokenUrl(this.mirror, this.tokenid);
    },
    cardColor() {
      if (this.status === 'CLOSED') {
        return 'red';
      } else if (this.status === 'PENDING') {
        return 'grey';
      } else {
        return 'black';
      }
    },
      auctionStatusText() {
          if (this.status === 'CLOSED') {
              return 'This auction is closed';
          } else if (this.status === 'PENDING') {
              return 'This auction is pending';
          } else {
              return '';
          }
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
