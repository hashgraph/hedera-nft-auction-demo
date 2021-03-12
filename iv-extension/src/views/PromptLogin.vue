<template>
	<section class="prompt-login">
		<section>
			<img alt="logo" src="../assets/logo.png">
			<figure class="account-details">
				{{prompt.data.network}}<br />
				<b v-if="availableAccounts.length === 1">{{selectedAccount}}</b>
			</figure>
		</section>
		<section class="details">
			<figure class="pre">You're logging into</figure>
			<figure class="domain">{{prompt.data.domain}}</figure>
			<section v-if="availableAccounts.length > 1">
				<label>Select an account</label>
				<Dropdown :selected="selectedAccount" :options="availableAccounts.map(x => x.name)" v-on:selected="x => selectedAccount = x" />
			</section>
			<figure class="disclaimer">
				If you approve this login request then this
				website will be able to see your account
				details, and request transaction
				signatures from you.
			</figure>
		</section>
		<section class="actions">
			<Swiper v-on:approved="approve" v-on:denied="deny" />
		</section>
	</section>
</template>

<script>

	import PromptService from "../services/PromptService";
	import {mapState} from "vuex";

	export default {
		async mounted(){
			this.selectedAccount = this.availableAccounts.length ? this.availableAccounts[0].name : null;
		},
		data(){return {
			selectedAccount:null,
		}},
		computed:{
			...mapState([
				'prompt',
			]),
			availableAccounts(){
				return this.prompt.iv.keychain.accounts.filter(x => x.network === this.prompt.data.network);
			},
		},
		methods:{
			approve(){
				this.prompt.responder(this.selectedAccount);
				PromptService.close();
			},
			deny(){
				this.prompt.responder(false);
				PromptService.close();
			}
		}
	}
</script>

<style lang="scss" scoped>
	@import "../styles/variables";

	.prompt-login {
		padding:60px 26px 40px;
		display:flex;
		justify-content: space-between;
		align-items: center;
		flex-direction: column;
		width:100%;
		height:100vh;

		img {
			opacity:0.5;
			width:60px;
		}

		.account-details {
			font-size: 9px;
			color:#818181;
			margin-top: 10px;
			b {
				color:$blue;
			}
		}


		.details {
			color:$black;
			font-size: 18px;
			font-weight: 300;

			label {
				font-size: 9px;
				margin-bottom:5px;
			}

			.pre {
				font-size: 14px;
				color:#818181;
			}

			.domain {
				font-size: 20px;
				color:$blue;
				margin-top:10px;
				margin-bottom:30px;
				font-weight: bold;
			}

			.disclaimer {
				font-size: 11px;
				color:#818181;
				padding:0 40px;
				margin-top:20px;
			}
		}

		.actions {
			width:100%;
		}
	}
</style>
