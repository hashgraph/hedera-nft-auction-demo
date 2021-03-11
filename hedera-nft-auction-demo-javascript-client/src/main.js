import Vue from "vue";
import App from "./App.vue";
import router from "./router";
import vuetify from "./plugins/vuetify";

Vue.config.productionTip = false;
import VueCookies from "vue-cookies";
Vue.use(VueCookies);

new Vue({
  router,
  vuetify,
  render: h => h(App)
}).$mount("#app");
