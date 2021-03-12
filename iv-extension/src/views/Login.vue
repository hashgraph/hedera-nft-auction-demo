<template>
	<div class="login">
		<img alt="logo" src="../assets/logo.png">

		<ImportAccount v-if="restoring" v-on:done="restoring = false" :show-password-field="true" />

		<transition mode="out-in">
			<section class="details" v-if="state === STATES.LOGIN">
				Ready to continue your journey?
			</section>

			<section class="details" v-if="state === STATES.DESTROY">
				There is no way to recover your password itself, but if you have your 24 words or a private key
				you can just re-create your IV.
				<br />
				<br />
				<b>Beware, this action is not reversible and all data will be lost.</b>
			</section>

			<section class="details" v-if="state === STATES.CREATE_NEW_INSTANCE">
				You never know where you are on a journey until you reach further.
				<br />
				<br />
				<b>Welcome to further.</b>
			</section>

			<section class="details" v-if="state === STATES.WIZARD_ONE">
				Let’s make sure you’re safe before you continue on your journey.
			</section>

			<section class="details" v-if="state === STATES.WIZARD_TWO">
				<section class="mnemonic">
					<figure :key="i" v-for="(word, i) in mnemonic">{{word}}</figure>
				</section>
			</section>

			<section class="details" v-if="state === STATES.WIZARD_THREE">
				<section style="display:flex; align-items: center; justify-content: center;">
					<PlasmaBall text="Setting up your wallet" />
				</section>
			</section>
		</transition>



		<transition mode="out-in">
			<section class="actions" v-if="state === STATES.LOGIN">
				<input placeholder="Enter your password" type="password" v-model="password" />
				<button @click="unlock">Unlock</button>
				<div>
					Did you <span @click="state = STATES.DESTROY">lose your password?</span>
				</div>
			</section>

			<section class="actions" v-if="state === STATES.DESTROY">
				<button @click="destroy">Nuke it</button>
				<div>
					Oh, I <span @click="state = STATES.LOGIN">don't want that.</span>
				</div>
			</section>

			<section class="actions" v-if="state === STATES.CREATE_NEW_INSTANCE">
				<button @click="setState(STATES.WIZARD_ONE)">Get started</button>
				<div>
					Or perhaps you <span @click="restoring = true">want to import?</span>
				</div>
			</section>

			<section class="actions" v-if="state === STATES.WIZARD_ONE">
				<input placeholder="Enter a strong password" type="password" v-model="password" />
				<input placeholder="Verify your password" type="password" v-model="passwordConfirmation" />
				<button @click="createInstance">Continue</button>
				<div>Make a mistake? <span @click="setState(STATES.CREATE_NEW_INSTANCE)">Go back</span></div>
			</section>

			<!--<section class="actions" v-if="state === STATES.WIZARD_TWO">-->

				<!--<section class="approve-me">-->
					<!--<input type="checkbox" />-->
					<!--<div>I promise to write these down, and <b>never to give them to anyone</b>.</div>-->
				<!--</section>-->

				<!--<button @click="approveMnemonic">Okay, I'm done</button>-->
				<!--<div>Make a mistake? <span @click="setState(STATES.WIZARD_ONE)">Go back</span></div>-->
			<!--</section>-->

			<section class="actions" v-if="state === STATES.WIZARD_THREE">

			</section>
		</transition>


	</div>
</template>

<script>
	import StorageService from "../services/StorageService";
	import InternalMessage from "../messages/InternalMessage";
	import * as InternalMessageTypes from "../messages/InternalMessageTypes";
	import {PrivateKey} from '@hashgraph/sdk'
	import ApiService from "../services/ApiService";
	import PlasmaBall from "../components/PlasmaBall";

	const STATES = {
		LOGIN:'login',
		CREATE_NEW_INSTANCE:'landing',
		DESTROY:'destroy',
		WIZARD_ONE:'password',
		WIZARD_TWO:'mnemonic',
		WIZARD_THREE:'setupAccounts',
	};

	export default {
		components: {PlasmaBall},
		async mounted(){
			const hasData = await InternalMessage.signal(InternalMessageTypes.HAS_DATA).send();
			if(hasData) this.state = STATES.LOGIN;
			else {
				// this.state = (await InternalMessage.payload(InternalMessageTypes.GET_SETUP_STATE, null).send()) || STATES.CREATE_NEW_INSTANCE;
				// if(!Object.values(STATES).includes(this.state)) this.state = STATES.CREATE_NEW_INSTANCE;
				this.state = STATES.CREATE_NEW_INSTANCE;
			}

		},
		data(){return {
			state:null,
			STATES,

			password:'',
			passwordConfirmation:'',

			mnemonic:[],
			mnemonicOrPrivateKey:'',
			importingNetwork:'Mainnet',
			importingAccounts:[],

			restoring:false,
		}},
		methods:{
			setState(state){

				this.state = state;
			},
			async unlock(){
				const unlocked = await InternalMessage.payload(InternalMessageTypes.UNLOCK, this.password).send();

				if(unlocked){
					this.openWallet();
				}
			},
			async createInstance(){
				const mnemonic = await InternalMessage.payload(InternalMessageTypes.SETUP, {
					state:this.state,
					data:this.password,
					nextState:STATES.WIZARD_THREE
				}).send();

				// this.mnemonic = mnemonic;

				this.state = STATES.WIZARD_THREE;
				this.setupAccounts();
			},
			// async approveMnemonic(){
			// 	await InternalMessage.payload(InternalMessageTypes.SETUP, {
			// 		state:this.state,
			// 		nextState:STATES.WIZARD_THREE
			// 	}).send();
			//
			// 	this.state = STATES.WIZARD_THREE;
			// 	this.setupAccounts();
			// },
			async setupAccounts(){
				await new Promise(r => setTimeout(r, 2000));
				await InternalMessage.payload(InternalMessageTypes.SETUP, {
					state:'finishedSetup'
				}).send();

				this.openWallet();
			},
			async destroy(){
				await InternalMessage.signal(InternalMessageTypes.DESTROY).send();
				this.state = STATES.CREATE_NEW_INSTANCE;
			},
			async checkAccounts(key){
				this.importingAccounts = await ApiService.getAccountsFromPublicKey(this.importingNetwork, key.publicKey);
			},
			openWallet(){
				this.$router.push('/main')
			},
		},
		watch:{
			mnemonicOrPrivateKey(){
				try {
					this.checkAccounts(PrivateKey.fromString(this.mnemonicOrPrivateKey));
				} catch(e){}
			},
			importingNetwork(){
				try {
					this.checkAccounts(PrivateKey.fromString(this.mnemonicOrPrivateKey));
				} catch(e){}
			}
		}
	}
</script>

<style lang="scss" scoped>
	@import "../styles/variables";

	.login {
		padding:80px 26px 40px;
		display:flex;
		justify-content: space-between;
		align-items: center;
		flex-direction: column;
		width:100%;
		height:100%;

		img {
			opacity:0.5;
		}

		.account-length {
			font-size: 11px;
			margin-top:10px;
		}

		.approve-me {
			display:flex;
			text-align:left;
			justify-content: center;
			align-items: center;
			margin:0 25px 5px;

			input {
				width:30px;
				margin:0;
			}
			> div {
				flex:1;
				font-size: 13px;
				margin-left:10px;
			}
		}

		.details {
			color:$black;
			font-size: 18px;
			font-weight: 300;
			width:100%;

			.mnemonic {
				display:flex;
				flex-wrap: wrap;

				figure {
					font-size: 13px;
					padding:5px 10px;
					border-radius:4px;
					margin:2px;
					background:#f3f3f3;
					color:$black;
					font-weight: bold;
				}
			}
		}

		.actions {
			width:100%;

			> div {
				font-size: 14px;
				margin-top:20px;

				span {
					color:$blue;
					font-weight: bold;
					cursor: pointer;
					text-decoration: underline;
				}
			}
		}
	}
</style>
