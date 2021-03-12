<template>
	<section class="associate-token">
		<Navbar :show-go-back="true" />

		<transition name="pop-between" mode="out-in">
			<section key="1" class="container" v-if="status === STATUS.DETAILS">
				<section style="width:100%; display:flex; align-items: center;">
					<input v-model="tokenId" placeholder="Enter a token ID" />
					<button style="width:auto; flex:0 0 auto; padding:0 20px; margin-bottom:5px; margin-left:5px" class="secondary" @click="getTokenInfo" v-tooltip="'Load token info <br />(costs $0.0001)'">
						<i class="fas fa-info-circle"></i>
					</button>
				</section>

				<section class="details" v-if="!tokenInfo">
					Accepting / associating a token with your account means you are allowing people to send it to you.
					<br />
					<br />
					<b style="font-size: 11px;">
						Associating a token with your account costs <b>$0.05</b>.
					</b>
				</section>

				<section class="details" v-if="tokenInfo">
					<label>Token Name</label>
					{{tokenInfo.name}}<br />

					<label>Total Supply</label>
					<AnimatedNumber :number="tokenInfo.totalSupply" :decimals="tokenInfo.decimals" /> <b>{{tokenInfo.symbol}}</b>
				</section>

				<section style="width:100%;">
					<Swiper text="Slide to allow" :no-cancel="true" v-on:approved="associate" />
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
						You allowed accepted <b>{{tokenId}}</b> tokens for <b>{{activeAccount.name}}</b>
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

	import {mapActions} from "vuex";

	const { Client, TokenAssociateTransaction, TokenInfoQuery } = require("@hashgraph/sdk");

	const STATUS = {
		DETAILS:0,
		SENDING:1,
		SENT:2,
		ERROR:3,
	}

	export default {
		data(){return {
			status:STATUS.DETAILS,
			STATUS,

			tokenId:'',
			sent:false,
			txid:'',
			error:null,
			tokenInfo:null,
		}},
		computed:{

		},
		methods:{
			async getTokenInfo(){
				this.tokenInfo = null;

				const client = this.getClient();
				let query = await new TokenInfoQuery()
					.setTokenId(this.tokenId)
					.execute(client).catch(err => {
						//console.error("Token get info error", err);
						this.status = STATUS.ERROR;
						this.error = err;
						return null;
					});

				if(query){
					this.setTokenMeta(this.tokenId, query);
					let totalSupply = parseFloat(parseFloat(query.totalSupply.toString()) / parseInt(query.decimals === 0 ? 1 : query.decimals));
					this.tokenInfo = {
						name:query.name,
						decimals:query.decimals,
						totalSupply:0,
						symbol:query.symbol,
					};

					setTimeout(() => {
						this.tokenInfo.totalSupply = totalSupply;
					}, 10);

					setTimeout(() => {
						this.getAccountInfo();
					}, 1000);
				}
			},
			async associate(){
				try {
					// TODO: Error handling
					this.status = STATUS.SENDING;

					const client = this.getClient();

					const tx = new TokenAssociateTransaction()
						.setAccountId(this.activeAccount.name)
						.setTokenIds([this.tokenId]);

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


					}
				} catch(e){
					//console.error("Associate token error", e);
					this.error = e;
					this.status = STATUS.ERROR;
				}
			},
		},
		watch:{

		}
	}
</script>

<style lang="scss" scoped>
	@import "../styles/variables";

	.associate-token {
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

		.details {
			color: $black;
			font-size: 16px;
			font-weight: 300;
			width: 100%;

			label {
				margin-top:10px;
				font-size: 9px;
				font-weight: bold;
				color:$grey;
				display:block;
			}
		}
	}
</style>
