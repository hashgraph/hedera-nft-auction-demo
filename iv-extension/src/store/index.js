import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

export default new Vuex.Store({
	state: {
		prompt:null,
		iv:null,
		selectedNetwork:1,
		balance:null,
		activeAccount:null,
		tokenMeta:{},
	},
	mutations: {
		setPrompt(state, x){ state.prompt = x; },
		setIV(state, x){ state.iv = x; },
		setSelectedNetwork(state, x){ state.selectedNetwork = x; },
		setBalance(state, x){ state.balance = x; },
		setActiveAccount(state, x){ state.activeAccount = x; },
		setTokenMeta(state, x){ state.tokenMeta = x; },
	},
	actions: {
		setPrompt({commit}, x){ commit('setPrompt', x)},
		setIV({commit}, x){ commit('setIV', x)},
		setSelectedNetwork({commit}, x){ commit('setSelectedNetwork', x)},
		setBalance({commit}, x){ commit('setBalance', x)},
		setActiveAccount({commit}, x) { commit('setActiveAccount', x); },
		setTokenMeta({commit}, x){ commit('setTokenMeta', x)},
	},
	modules: {

	}
})
