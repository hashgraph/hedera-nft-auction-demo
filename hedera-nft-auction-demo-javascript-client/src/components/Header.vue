<template>
  <v-app-bar app color="primary" dark absolute>
    <div class="d-flex align-center">
      <v-img
        alt="Hedera Logo"
        class="shrink mr-2"
        contain
        src="../assets/logo.svg"
        transition="scale-transition"
        width="40"
      />
    </div>
    <div>Hedera NFT Auction Service Demo {{ nodeOwner }}</div>
    <v-spacer></v-spacer>
    <div class="ma-2">
      <v-select
        v-model="mirror"
        :items="mirrors"
        dense
        @change="chooseMirror()"
      />
    </div>
    <v-chip class="ma-2" v-if="topicUrl" color="green"
      ><a
        :href="topicUrl"
        style="text-decoration: none; color: inherit;"
        target="_blank"
        ><b>Auctions Topic Id: {{ topicId }}</b></a
      ></v-chip
    >
    <v-chip class="ma-2" v-else color="green"
      ><b>Auctions Topic Id: {{ topicId }}</b></v-chip
    >
  </v-app-bar>
</template>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<script>
import { EventBus, MIRROR_SELECTION } from "@/eventBus";
import { getTopicURL } from "@/utils";

export default {
  name: "Header",
  data: function() {
    return {
      mirrors: ["Kabuto", "Dragonglass"],
      topicUrl: "",
      topicId: process.env.VUE_APP_TOPIC_ID,
      mirror: "Kabuto",
      nodeOwner:
        process.env.VUE_APP_NODE_OWNER !== ""
          ? "(".concat(process.env.VUE_APP_NODE_OWNER).concat(")")
          : ""
    };
  },
  methods: {
    chooseMirror() {
      EventBus.$emit(MIRROR_SELECTION, this.mirror);
      this.topicUrl = getTopicURL(this.mirror, this.topicId);
    }
  },
  created() {
    this.chooseMirror();
  }
};
</script>
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
