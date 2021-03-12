<template>
	<section class="prompt-signature">
		<section>
			<img alt="logo" src="../assets/logo.png">
			<figure class="account-details">
				{{prompt.data.network}}<br />
				<b>{{prompt.data.account.name}}</b>
			</figure>
		</section>
		<section class="details">

			<!-- TRANSFER-ONLY DISPLAY -->
			<section class="transfer-hbar" v-if="isTransfer">
				<figure class="transaction-type">
          Transfer{{transferToCount > 1 ? 's' : ''}}
          <span v-if="transferToCount > 1">{{transferToCount}}</span>
        </figure>

				<!-- HBAR -->
				<section v-if="hasHbarTransfers">
					<figure class="transfer-total">{{totalHbarFrom}} <b>HBAR</b></figure>
					<figure class="transfer-details" :key="n" v-for="(x,n) in hbarTransfersTo">
						+{{parseFloat(parseFloat(x.amount.toString()) / 100000000).toFixed(8)}} <b>HBAR</b> to <span class="blue-text">{{accountIdToString(x.accountID)}}</span>
					</figure>
				</section>

				<!-- TOKENS -->
				<figure class="transfer-total" :key="n" v-for="(x,n) in tokenTransfersFrom">{{x.amount}} <b>{{x.token.symbol || x.token.id}}</b></figure>
				<figure class="transfer-details" :key="n" v-for="(x,n) in tokenTransfersTo">
					+{{x.amount}} <b>{{x.token.symbol || x.token.id}}</b> to <span class="blue-text">{{x.account}}</span>
				</figure>
			</section>

			<!-- ALL OTHER DISPLAYS -->
			<section v-else>
				<figure class="transaction-type">{{humanReadableTransactionType}}</figure>
				<section class="key-values">
					<figure class="param" :key="key" v-for="(value, key) in params">
						<figure class="key">{{camelToTitle(key)}}</figure>
						<figure class="value" v-if="typeof value !== 'object'">{{value}}</figure>
						<figure class="value" v-else><VueJsonViewer theme="jstheme" :value="value" /></figure>
					</figure>

				</section>
			</section>

		</section>
		<section class="actions">
			<figure class="fee"><span>Fee</span> {{transactionFee}} <b>HBAR</b></figure>
			<Swiper v-on:approved="approve" v-on:denied="deny" />
		</section>
	</section>
</template>

<script>
	import VueJsonViewer from "vue-json-viewer";
	import PromptService from "../services/PromptService";
	import {mapState} from "vuex";
	import StringHelpers from "../util/StringHelpers";

	const PERCENTAGE_TO_SWIPE = 0.5;

	import {
		PublicKey
	} from '@hashgraph/sdk'

	export default {
		components: {
			VueJsonViewer,
		},
		async mounted(){
			this.getTokenMeta();
			if(!this.isTransfer) this.formatKeyValues(this.prompt.data.details[this.transactionType]);
		},
		data(){return {

		}},
		computed:{
			...mapState([
				'prompt',
			]),
			isTransfer(){
				return this.transactionType === 'cryptoTransfer';
			},
			transferToCount(){
				return this.hbarTransfersTo.length + this.tokenTransfersTo.length;
			},
			transactionFee(){
				return parseFloat(parseFloat(this.prompt.data.details.transactionFee.toString()) / 100000000).toFixed(8);
			},
			hasHbarTransfers(){
				return !!this.prompt.data.details.cryptoTransfer.transfers.accountAmounts.length;
			},
			hbarTransfersFrom(){
				return this.prompt.data.details.cryptoTransfer.transfers.accountAmounts.filter(x => {
					return this.accountIdToString(x.accountID) === this.prompt.data.account.name;
				})
			},
			totalHbarFrom(){
				const total = this.hbarTransfersFrom.reduce((acc, x) => parseFloat(acc + parseFloat(x.amount.toString())), 0);
				if(!total) return '0.00000000';
				return parseFloat(total / 100000000).toFixed(8);
			},
			hbarTransfersTo(){
				return this.prompt.data.details.cryptoTransfer.transfers.accountAmounts.filter(x => {
					return this.accountIdToString(x.accountID) !== this.prompt.data.account.name;
				})
			},
			tokenTransfers(){
				return this.prompt.data.details.cryptoTransfer.tokenTransfers.reduce((acc,x) => {
					x.transfers.map(transfer => {
						const tokenId = this.tokenIdToString(x.token);
						const meta = this.tokenMeta.hasOwnProperty(tokenId)
							? this.tokenMeta[tokenId]
							: { symbol:null, name:null, decimals:null };
						acc.push({token:Object.assign({
							id:tokenId,
						}, meta), account:this.accountIdToString(transfer.accountID), amount:transfer.amount.toString()});
					});
					return acc;
				}, []);
			},
			tokenTransfersFrom(){
				return this.tokenTransfers.filter(x => {
					return x.account === this.prompt.data.account.name;
				})
			},
			tokenTransfersTo(){
				return this.tokenTransfers.filter(x => {
					return x.account !== this.prompt.data.account.name;
				})
			},
			cryptoTransfers(){
				return this.prompt.data.details.cryptoTransfer.transfers.accountAmounts.filter(x => {
					return this.accountIdToString(x.accountID) !== this.prompt.data.account.name;
				})
			},
			transactionType(){
				return this.prompt.data.details.data;
			},
			humanReadableTransactionType(){
				return StringHelpers.camelToTitle(this.prompt.data.details.data);
			},
			params(){
				return this.prompt.data.details[this.transactionType];
			},
		},
		methods:{
			camelToTitle(text){
				return StringHelpers.camelToTitle(text);
			},
			formatKeyValues(obj){
				for (let property in obj) {
					if (obj.hasOwnProperty(property)) {
						//console.log(typeof obj[property], obj[property])
						if (typeof obj[property] === "object") {
							if(this.isLong(obj[property])) obj[property] = obj[property].toString();
							else if(this.isAccountIdString(obj[property])) obj[property] = this.accountIdToString(obj[property]);
							else if(this.isTokenIdString(obj[property])) obj[property] = this.tokenIdToString(obj[property]);
							else if(obj[property].hasOwnProperty('ed25519')) {
								obj[property] = PublicKey.fromBytes(obj[property].ed25519).toString();
							}
							else if(obj[property].hasOwnProperty('seconds')) {
								obj[property] = obj[property].seconds.toString() + ' seconds'
							}

							this.formatKeyValues(obj[property]);
						}
					}
				}
			},
			isLong(obj){
				return obj.hasOwnProperty('low') && obj.hasOwnProperty('high') && obj.hasOwnProperty('unsigned')
			},
			isAccountIdString(obj){
				return obj.hasOwnProperty('shardNum') && obj.hasOwnProperty('realmNum') && obj.hasOwnProperty('accountNum')
			},
			isTokenIdString(obj){
				return obj.hasOwnProperty('shardNum') && obj.hasOwnProperty('realmNum') && obj.hasOwnProperty('tokenNum')
			},
			accountIdToString(accountId){
				return `${accountId.shardNum.toString()}.${accountId.realmNum.toString()}.${accountId.accountNum.toString()}`
			},
			tokenIdToString(tokenId){
				return `${tokenId.shardNum.toString()}.${tokenId.realmNum.toString()}.${tokenId.tokenNum.toString()}`
			},
			approve(){
				this.prompt.responder(true);
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

	.prompt-signature {
		padding:40px 26px 40px;
		display:flex;
		justify-content: space-between;
		align-items: center;
		flex-direction: column;
		width:100%;
		height:100vh;

		.jstheme {
			background:transparent;
		}

		.fee {
			font-size: 9px;
			margin-bottom:10px;
			margin-top:10px;

			span {
				background:$blue;
				color:white;
				padding:1px 3px;
				border-radius:4px;
				margin-right:5px;
			}
		}

		pre {
			font-size: 9px;
			word-break: break-all;
		}

		img {
			opacity:0.5;
			width:40px;
		}

		.transaction-type {
			font-size: 18px;
			font-weight: bold;
			color:$blue;
			margin-bottom:10px;
			display: flex;
			align-items: center;
			justify-content: center;

			span {
				background:$blue;
				color:white;
				padding:2px 6px;
				border-radius:6px;
				margin-left:10px;
				font-size: 11px;
			}
		}

		.transfer-details {
			font-size: 13px;
		}

		.blue-text {
			color:$blue;
		}

		.account-details {
			font-size: 9px;
			color:#818181;
			margin-top: 10px;
			b {
				color:$blue;
			}
		}

		.key-values {
			display: block;
			text-align: left;
			overflow: auto;
			max-height: 300px;
			width: 100%;
			max-width: 900px;
			margin: 0 auto;

			.param {
				font-size: 13px;
				display:flex;
				padding:5px;
				border-radius: 6px;

				&:nth-child(odd){
					background: #f0f4ff;
				}

				.key {
					flex:1;
				}

				.value {
					flex:1;
					font-weight: bold;
				}
			}

		}

		.details {
			color:$black;
			font-size: 18px;
			font-weight: 300;
			width:100%;

			.transfer-hbar {
				margin-top:20px;
				overflow-y:auto;
				max-height:250px;
				padding-bottom:20px;
				padding-top:6px;
			}

			.disclaimer {
				font-size: 11px;
				color:#818181;
				padding:0 50px;
			}
		}

		.actions {
			width:100%;


		}
	}
</style>
