<template>
	<div class="main-screen">
		<Navbar />

		<!--<section class="details">-->
			<!--It looks like even though you have IV set up, you don't have an account with a balance.-->

			<!--<br />-->
			<!--<p>-->
				<!--You can still use IV, but it will make your life easier if you go through the process of-->
				<!--registering this wallet and filling it with some HBAR.-->
			<!--</p>-->
		<!--</section>-->

		<section class="actions" v-if="hasAccount">
			<figure class="action" v-if="balance" @click="$router.push('/transfer')" v-tooltip="'Transfer'">
				<i class="fas fa-exchange-alt"></i>
			</figure>
			<figure class="action" @click="$router.push('/associate')" v-tooltip="'Approve Token'">
				<i class="fas fa-hand-holding-medical"></i>
			</figure>
			<figure class="action" v-if="balance" @click="$router.push('/tokens')" v-tooltip="'View Tokens'">
				<i class="fas fa-coins"></i>
			</figure>
			<!--<figure class="action" @click="test">-->
				<!--<i class="fas fa-cog"></i>-->
			<!--</figure>-->
			<figure class="action" @click="lock" v-tooltip="'Lock'">
				<i class="fas fa-lock"></i>
			</figure>
		</section>

		<section class="actions" v-else>
			<figure class="action" @click="registerAccount" v-tooltip="'Create an account'">
				<i class="fas fa-user"></i>
			</figure>
			<figure class="action" @click="lock" v-tooltip="'Lock'">
				<i class="fas fa-lock"></i>
			</figure>
		</section>

		<PlasmaBall text="Listening for incoming gossip" />

	</div>
</template>

<script>
	import InternalMessage from "../messages/InternalMessage";
	import * as InternalMessageTypes from "../messages/InternalMessageTypes";
	import PromptService from "../services/PromptService";
	import Prompt from "../models/prompts/Prompt";
	import * as PromptTypes from "../models/prompts/PromptTypes";

	export default {
		async mounted(){
			await this.regenerateIVData();
			this.checkActiveAccount();
			this.getTokenMeta();
			this.getAccountInfo();
		},
		data(){return {

		}},
		computed:{

		},
		methods:{
			async lock(){
				await InternalMessage.signal(InternalMessageTypes.LOCK).send();
				this.$router.push('/login');
			},
			registerAccount(){
				window.open('https://portal.hedera.com/');
			},
		}
	}
</script>

<style lang="scss" scoped>
	@import "../styles/variables";

	.main-screen {
		padding:0 26px 40px;
		display:flex;
		justify-content: space-between;
		align-items: center;
		flex-direction: column;
		width:100%;
		height:100%;



		.actions {
			display:flex;
			align-items: center;
			justify-content: center;
			flex-wrap: wrap;
			padding:20px 30px;

			.action {
				margin:10px;
				border-radius:4px;
				box-shadow:0 1px 3px rgba(0,0,0,0.1), 0 3px 8px rgba(0,0,0,0.03);
				color:rgba(0,0,0,0.45);
				padding:22px;
				cursor: pointer;
				transition: all 0.2s ease;
				transition-property: box-shadow, color, transform;

				i {
					font-size: 18px;
				}

				div {
					margin-top:10px;
					font-size: 13px;
					display:none;
				}

				&:hover {
					box-shadow:0 6px 13px rgba(0,0,0,0.1), 0 12px 34px rgba(0,0,0,0.05);
					transform:translateY(-2px);
					color:rgba(0,0,0,0.7);
				}

				&:active {
					box-shadow:0 1px 2px rgba(0,0,0,0.12), 0 3px 5px rgba(0,0,0,0.07);
					transform:translateY(2px);
					color:rgba(0,0,0,0.1);
				}
			}
		}

	}
</style>
