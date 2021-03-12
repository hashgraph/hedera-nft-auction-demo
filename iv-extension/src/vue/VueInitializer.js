import Vue from 'vue'
import {mapState, mapActions} from 'vuex';

import VTooltip from 'v-tooltip'


import VueRouter from 'vue-router'
import App from '../popup/App.vue'
import {RouteNames, Routing} from './Routing';
import store from "../store";
import InternalMessage from "../messages/InternalMessage";
import * as InternalMessageTypes from "../messages/InternalMessageTypes";
import {NETWORKS} from "../models/Networks";
const {
	Client, AccountBalanceQuery
} = require("@hashgraph/sdk");

Vue.config.productionTip = false;

export let router;

/***
 * Sets up an instance of Vue.
 * This is shared between the popup.js and prompt.js scripts.
 */
export default class VueInitializer {

    constructor(routes,
                components,
                middleware = () => {}){

        this.setupVuePlugins();
        this.registerComponents(components);
        router = this.setupRouting(routes, middleware);

	    Vue.mixin({
		    data(){ return {
			    RouteNames,
			    NETWORKS,
		    }},
		    computed:{
			    ...mapState([
					'iv',
				    'selectedNetwork',
				    'balance',
				    'activeAccount',
				    'tokenMeta',
			    ]),
			    hbarBalance(){
			    	if(!this.balance) return null;
				    if(!parseFloat(this.balance.hbars.toTinybars().toString())){
					    return parseFloat(0).toFixed(8);
				    } else {
					    return parseFloat(this.balance.hbars.toTinybars() / 100000000).toFixed(8);
				    }
			    },
			    tokenBalances(){
			    	if(!this.balance) return null;
				    const json = JSON.parse(this.balance.tokens.toString());
				    return Object.keys(json).map(tokenId => {
					    // Merging in meta data with balances
					    const meta = this.tokenMeta.hasOwnProperty(tokenId)
						    ? this.tokenMeta[tokenId]
						    : { symbol:null, name:null, decimals:null };
					    return Object.assign({id:tokenId, balance:json[tokenId]}, meta);
				    });
			    },
			    hasAccount(){
				    return !!this.activeAccount;
			    }
		    },
		    mounted(){
			    this.sanitizeVuex();
		    },
		    methods: {
		    	async getTokenMeta(){
		    	    store.dispatch('setTokenMeta', await InternalMessage.signal(InternalMessageTypes.GET_TOKEN_META).send());
			    },
			    async setTokenMeta(id, query){
		    		const meta = {
					    name:query.name,
					    symbol:query.symbol,
					    decimals:query.decimals,
				    }
				    // Soft save (prevents having to go back to IPC to update local refs
				    const clone = JSON.parse(JSON.stringify(this.tokenMeta));
				    clone[id] = meta;
				    store.dispatch('setTokenMeta', clone);
		    		this.$forceUpdate();
		    		// Hard save
		    	    return InternalMessage.payload(InternalMessageTypes.SET_TOKEN_META, {id, meta}).send();
			    },
		    	async checkActiveAccount(){
		    		if(this.activeAccount) return;
		    		if(!this.iv) return;

				    let account = await InternalMessage.signal(InternalMessageTypes.GET_ACTIVE_ACCOUNT).send();
				    if(!account) {
					    account = this.iv.keychain.accounts[0];
					    if(account) await InternalMessage.payload(InternalMessageTypes.SET_ACTIVE_ACCOUNT, account).send();
				    }
				    if(!account) return;

				    //console.log('setting active account', account);

				    store.dispatch('setActiveAccount', account);
			    },
		    	async setSelectedNetwork(network){
		    		store.dispatch('setSelectedNetwork', network);
			    },
		    	async regenerateIVData(){
		    	    return store.dispatch('setIV', await InternalMessage.signal(InternalMessageTypes.GET_DATA).send());
			    },
			    sanitizeVuex(){
				    // Removes pesky __vue__ exposure from elements.
				    const all = document.querySelectorAll("*");
				    for (let i=0, max=all.length; i < max; i++) {
					    if(all[i].hasOwnProperty('__vue__')) delete all[i].__vue__;
				    }
			    },
			    getClient(){
				    if(!this.activeAccount) return;
				    const client = Client[`for${this.activeAccount.network}`]();
				    client.setOperatorWith(
					    this.activeAccount.name,
					    this.activeAccount.publicKey,
					    async buf => {
						    const signature = await InternalMessage.payload(InternalMessageTypes.INTERNAL_SIGN, {data:buf, account:this.activeAccount}).send();
						    return Buffer.from(signature, 'hex');
					    }
				    );

				    return client;
			    },
			    async getAccountInfo(){
				    if(!this.activeAccount) return;

				    const client = Client[`for${this.activeAccount.network}`]();
				    const accountBalance = await new AccountBalanceQuery()
					    .setAccountId(this.activeAccount.name)
					    .execute(client).catch(err => {
						    //console.error("Error getting account balance", err);
						    return null;
					    });

				    if(accountBalance){
					    store.dispatch('setBalance', accountBalance);
					    this.$forceUpdate();
				    }

			    },
			    formatNumber(num){
				    if(!num) return 0;

				    let precision = num.toString().split('.')[1];
				    precision = precision ? precision.length : 0;

				    const [whole, decimal] = num.toString().split('.');
				    const formatted = whole.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
				    if(!decimal || parseInt(decimal) === 0) return formatted;
				    return `${formatted}.${decimal}`;
			    },
		    },
	    })

	    this.setupVue(router);



        return router;
    }

    setupVuePlugins(){
        Vue.use(VueRouter);
	    Vue.use(VTooltip, {
		    defaultOffset:5,
		    defaultDelay: 0,
		    defaultContainer: '#app',
	    });
    }

    registerComponents(components){
        components.map(component => {
            Vue.component(component.tag, component.vue);
        });
    }

    setupRouting(routes, middleware){
        const router = new VueRouter({
	        routes,
	        // mode: 'history',
	        linkExactActiveClass: 'active',
        });

        router.beforeEach((to, from, next) => {
            return middleware(to, next)
        });

        return router;
    }

    setupVue(router){
        const app = new Vue({
	        router, store,
	        render: h => h(App)
        });
        app.$mount('#app');
    }

}
