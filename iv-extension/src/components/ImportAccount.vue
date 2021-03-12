<template>
	<section class="import-account">

		<figure class="darkener"></figure>

		<section class="container">
			<section class="details">
				<Dropdown :selected="importingNetwork" :options="['Mainnet', 'Testnet', 'Previewnet']" v-on:selected="x => importingNetwork = x" />
				<input type="password" class="square" v-model="mnemonicOrPrivateKey" placeholder="Enter your phrase or private key" />
				<input v-if="importingNetwork !== 'Testnet'" class="square" v-model="manualAccount" placeholder="Enter your account ID" />
				<figure class="account-length" v-if="importingAccounts.length">Importing <b>{{importingAccounts.length}}</b> account{{importingAccounts.length === 1 ? '' : 's'}} for {{importingNetwork}}</figure>
				<figure class="account-length" v-if="!importingAccounts.length">No accounts found for {{importingNetwork}}</figure>
			</section>

			<section class="actions">
				<input v-if="showPasswordField" placeholder="Enter a strong password" type="password" v-model="password" />
				<button @click="importAccount">Import account</button>
				<div>
					On second thought, <span @click="$emit('done', false)">nevermind.</span>
				</div>
			</section>
		</section>
	</section>
</template>

<script>
	import {Mnemonic, PrivateKey} from '@hashgraph/sdk';
	import ApiService from "../services/ApiService";
	import InternalMessage from "../messages/InternalMessage";
	import * as InternalMessageTypes from "../messages/InternalMessageTypes";

	export default {
		props:['showPasswordField'],
		data(){return {
			importingNetwork:'Mainnet',
			mnemonicOrPrivateKey:'',
			importingAccounts:[],
			manualAccount:'',

			password:'',
		}},
		methods:{
			goBack(){

			},
			async checkAccounts(key){
				if(this.importingNetwork !== 'Testnet') return;
				this.importingAccounts = await ApiService.getAccountsFromPublicKey(this.importingNetwork, key.publicKey);
			},
			async importAccount(){
				// Restoring entire instance
				if(this.showPasswordField){
					const restored = await InternalMessage.payload(InternalMessageTypes.RESTORE, {
						mnemonicOrPrivateKey:this.mnemonicOrPrivateKey,
						network:this.importingNetwork,
						accounts:this.importingAccounts,
						password:this.password,
					}).send();

					// TODO: Error handling
					if(!restored) return //console.error("Did not restore")
					this.$router.push('/main');

					return;
				}


				let key = null;

				const isMnemonic = this.mnemonicOrPrivateKey.indexOf(' ') > -1;
				if(isMnemonic){
					try {
						const mnemonic = await Mnemonic.fromString(this.mnemonicOrPrivateKey);
						key = await mnemonic.toPrivateKey();
					} catch(e){
						// TODO: Error handling
						//console.error("Error restoring mnemonic", e);
						return false;
					}
				} else {
					try {
						// Private key imported
						key = PrivateKey.fromString(this.mnemonicOrPrivateKey);
					} catch(e){
						// TODO: Error handling
						//console.error("Error restoring private key", e);
						return false
					}
				}

				let accounts = [];
				if(this.importingNetwork === 'Testnet') accounts = await ApiService.getAccountsFromPublicKey('Testnet', key.publicKey);
				else accounts.push(this.manualAccount);
				accounts = accounts.map(account => {
					return {
						network:this.importingNetwork,
						name:account,
					}
				})

				const inserted = await InternalMessage.payload(InternalMessageTypes.INSERT_ACCOUNTS, {accounts, privateKey:key.toString()}).send();
				if(inserted){
					await this.regenerateIVData();
					this.$emit('done', true);
				} else {
					// TODO: Error handling!
					//console.error('Could not import accounts? Check BG script.');
				}

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
	@import '../styles/variables';

	.import-account {
		position:fixed;
		top:50px;
		bottom:50px;
		left:30px;
		right:30px;
		z-index:10;

		@keyframes fade-in {
			0% { opacity:0; }
			100% { opacity:1; }
		}

		@keyframes fade-in-slide {
			0% { opacity:0; transform:translateY(-100%); }
			100% { opacity:1; transform:translateY(0); }
		}

		.darkener {
			position:fixed;
			top:0;
			bottom:0;
			left:0;
			right:0;
			background:rgba(0,0,0,0.4);
			z-index:-1;
			opacity:0;

			animation: fade-in 0.2s ease forwards;
		}

		.container {
			position:relative;
			z-index:1;

			background:white;
			padding:40px 26px 40px;
			display:flex;
			justify-content: space-between;
			align-items: center;
			flex-direction: column;
			box-shadow:0 1px 3px rgba(0,0,0,0.1), 0 3px 8px rgba(0,0,0,0.03);
			border-radius:8px;
			height:100%;
			opacity:0;

			animation: fade-in-slide 0.2s ease forwards;
			animation-delay: 0.1s;
		}


		.account-length {
			font-size: 11px;
			margin-top:10px;
		}

		.details {
			color:$black;
			font-size: 18px;
			font-weight: 300;
			width:100%;

			input {
				padding:0 10px;
			}
		}

		.actions {
			width:100%;

			button {
				height:50px;
				font-size: 16px;
			}

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
