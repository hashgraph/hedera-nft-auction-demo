<template>
	<section class="transfer" @mouseup="endErase">
		<Navbar :show-go-back="true" />

		<transition name="pop-between" mode="out-in">
			<section key="1" class="container" v-if="status === STATUS.DETAILS">
				<Dropdown :selected="tokenId" :options="['HBAR'].concat(tokenBalances.map(x => x.id))" v-on:selected="x => tokenId = x"  />
				<input class="square" v-model="recipient" placeholder="Enter an account ID" />

				<section style="width:100%;">
					<section class="amount-display">
						<input v-model="amount" />
						<figure v-show="!amount || !amount.length" class="placeholder">How much are you sending?</figure>
					</section>
					<section class="numpad">
						<figure @click="clickNumpad(x)" :key="x" v-for="x in [1,2,3,4,5,6,7,8,9,'.',0]">{{x}}</figure>
						<figure @mousedown="startErase"><i class="fas fa-chevron-left"></i></figure>
					</section>
				</section>

				<section style="width:100%;">
					<input placeholder="Add an optional memo" v-model="memo" class="memo" />
					<Swiper text="Slide to send" :no-cancel="true" v-on:approved="transfer" />
				</section>
			</section>

			<section key="2" class="container centered" v-if="status === STATUS.SENDING">
				<PlasmaBall text="Gossiping like nuns visiting the pope." />
			</section>

			<section key="3" class="container centered" v-if="status === STATUS.SENT">
				<section class="transfer-panel">
					<figure class="check-circle">
						<i class="fas fa-check"></i>
					</figure>
					<figure class="title">Success</figure>
					<figure class="text">
						You sent <b>{{amount}} HBAR</b> to <b>{{recipient}}</b>
						<br />
						<br />
						<span style="font-size: 9px">{{txid}}</span>
					</figure>
				</section>
			</section>

			<section key="4" class="container centered" v-if="status === STATUS.ERROR">
				<section class="transfer-panel red">
					<figure class="check-circle">
						<i class="fas fa-times"></i>
					</figure>
					<figure class="title">Oh no!</figure>
					<figure class="text">
						{{error}}
					</figure>
					<button @click="status = STATUS.DETAILS">Try again</button>
				</section>
			</section>
		</transition>

	</section>
</template>

<script>
	import InternalMessage from "../messages/InternalMessage";
	import * as InternalMessageTypes from "../messages/InternalMessageTypes";

	const { Client, TransferTransaction, Hbar, HbarUnit, TokenInfoQuery } = require("@hashgraph/sdk");

	const STATUS = {
		DETAILS:0,
		SENDING:1,
		SENT:2,
		ERROR:3,
	}

	let eraseTimeout;
	export default {
		async mounted(){
			if(this.$route.query.tokenId){
				this.tokenId = this.$route.query.tokenId;
			}
		},
		destroyed(){
			clearTimeout(eraseTimeout);
		},
		data(){return {
			status:STATUS.DETAILS,
			STATUS,

			recipient:'',
			amount:'',
			memo:'',

			eraseCount:null,
			erasing:false,
			sent:false,
			txid:'',
			error:null,

			tokenId:'HBAR',
		}},
		computed:{

		},
		methods:{
			clickNumpad(char){
				if(this.amount.indexOf('.') > -1 && char === '.') return;
				this.amount = this.amount.toString() + char;
			},
			startErase(){
				this.erasing = true;
				this.eraseOne();
			},
			endErase(){
				this.erasing = false;
				this.eraseCount = 0;
				clearTimeout(eraseTimeout);
			},
			async eraseOne(){
				if(!this.erasing) return;
				if(this.amount.length === 0) {
					this.erasing = false;
					return;
				}

				this.amount = this.amount.substr(0, this.amount.length-1);
				this.eraseCount++;
				eraseTimeout = setTimeout(() => this.eraseOne(), 500 / (1 + this.eraseCount));
			},
			async transfer(){
				try {
					// TODO: Error handling
					this.status = STATUS.SENDING;
					const amount = this.amount;

					const client = this.getClient();

					let tx = null;

					if(this.tokenId === 'HBAR'){
						tx = new TransferTransaction()
							.addHbarTransfer(this.activeAccount.name, Hbar.fromString(amount, HbarUnit.Hbar).negated()) //
							.addHbarTransfer(this.recipient, Hbar.fromString(amount, HbarUnit.Hbar))
					} else {
						const meta = await new TokenInfoQuery()
							.setTokenId(this.tokenId)
							.execute(client).catch(err => {
								// TODO: Error handling
								//console.error("Fetch token meta query error", err);
								return null;
							});

						if(!meta) return;

						const correctedAmount = parseFloat(this.amount) ** parseInt(meta.decimals);
						tx = new TransferTransaction()
							.addTokenTransfer(this.tokenId, this.activeAccount.name, -correctedAmount)
							.addTokenTransfer(this.tokenId, this.recipient, correctedAmount)
					}

					if(this.memo.length) tx.setTransactionMemo(this.memo);

					await tx.signWithOperator(client);

					const response = await tx.execute(client).catch(err => {
						//console.error("Error sending", err);
						this.status = STATUS.ERROR;
						this.error = err;
						return null;
					});

					if(response){
						// Time for animations!
						await new Promise(r => setTimeout(r, 2000));

						this.status = STATUS.SENT;
						this.txid = response.transactionId.toString()
						if(await response.getReceipt(client).catch(err => {
							//console.error("Error sending", err);
							this.status = STATUS.ERROR;
							this.error = err;
							return null;
						})){
							this.status = STATUS.SENT;
							this.sent = true;
							setTimeout(() => {
								this.getAccountInfo();
							}, 1000);
						}


						this.sent = true;
					}
				} catch(e){
					//console.error("Transfer error", e);
					this.error = e;
					this.status = STATUS.ERROR;
				}
			}
		},
		watch:{
			amount(){
				const chars = [1,2,3,4,5,6,7,8,9,0,'.'].map(x => x.toString());
				this.amount = this.amount.split('').filter(x => chars.includes(x)).join('');
			}
		}
	}
</script>

<style lang="scss" scoped>
	@import "../styles/variables";

	.transfer {
		padding:0 26px 40px;
		display:flex;
		justify-content: space-between;
		align-items: center;
		flex-direction: column;
		width:100%;
		height:100%;


		.transfer-panel {
			width:100%;
			display:flex;
			justify-content: center;
			align-items: center;
			flex-direction: column;

			.check-circle {
				border:10px solid $blue;
				border-radius: 50%;
				font-size: 64px;
				display: flex;
				justify-content: center;
				align-items: center;
				height: 120px;
				width: 120px;
				color:$blue;
			}

			.title {
				font-size: 24px;
				font-weight: bold;
				color:$blue;
			}

			.text {
				font-size: 13px;
				color:$grey;
				margin-top:15px;
			}

			&.red {
				.check-circle {
					color:$red;
					border:10px solid $red;
				}

				.title {
					color:$red;
				}

			}

			button {
				margin-top:30px;
				height:40px;
				font-size: 14px;
				width: auto;
			}
		}

		.container {
			display:flex;
			justify-content: space-between;
			align-items: center;
			flex-direction: column;
			width:100%;
			height:100%;

			&.centered {
				justify-content: center;
			}


		}

		.title {
			color:$grey;
			font-size: 11px;
		}

		.memo {
			font-size: 11px;
			height:36px;
			width:calc(100% - 40px);
			margin-left:20px;
		}

		.amount-display {
			width:100%;
			position: relative;

			.placeholder {
				position:absolute;
				top:0;
				bottom:0;
				left:0;
				right:0;
				display:flex;
				justify-content: center;
				align-items: center;
				color:rgba(0,0,0,0.2);
				font-size: 14px;
				pointer-events: none;
			}

			input {
				border:0;
				font-size: 36px;
				color:$blue;
				padding:0;
			}
		}

		.numpad {
			display:flex;
			align-items: center;
			justify-content: space-between;
			flex-wrap: wrap;
			width:100%;
			padding:0 30px;
			margin-bottom:30px;

			figure {
				width:33.3%;
				font-size: 22px;
				color:$lightgrey;
				padding:12px 15px;
				border-radius:8px;
				cursor: pointer;

				/*
				&:nth-child(3n+1){
					text-align:left;
				}

				&:nth-child(3n+3){
					text-align:right;
				}
				*/

				box-shadow:0 0 0 rgba(0,0,0,0), 0 0 0 rgba(0,0,0,0);
				transition: all 0.2s ease;
				transition-property: box-shadow, color, transform;

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
