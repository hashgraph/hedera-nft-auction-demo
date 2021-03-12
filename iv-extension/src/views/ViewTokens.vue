<template>
	<section class="associate-token">
		<Navbar :show-go-back="true" />

		<transition name="pop-between" mode="out-in">
			<section key="1" class="container" v-if="status === STATUS.TOKENS">
				<section class="no-tokens" v-if="!tokenBalances.length">
					<span>You don't have any tokens yet!</span>
					Before you can get any tokens, you'll need to approve / associate that token with your account.
					Go back to the main menu and click the "Approve token" button to get started.
				</section>
				<section class="tokens" v-if="tokenBalances.length">
					<section :key="token.id" class="token" v-for="token in tokenBalances">
						<section class="actions" v-if="!token.symbol">
							<figure class="action" @click="loadTokenInfo(token.id)" v-tooltip.right="'Load token info <br />(costs $0.0001)'">
								<i v-if="loadingToken !== token.id" class="fas fa-info-circle"></i>
								<i v-else class="fas fa-spinner fa-spin"></i>
							</figure>
						</section>
						<section class="info" :class="{'no-pad':token.symbol}">
							<figure class="id">{{token.id}}</figure>
							<figure class="name" v-if="token.name">{{token.name}}</figure>
							<figure class="balance">{{token.balance}} <b v-if="token.symbol">{{token.symbol}}</b></figure>
						</section>
						<section class="actions">
							<figure class="action" @click="removeToken(token)" v-tooltip.left="'Remove'"><i class="fas fa-ban"></i></figure>
							<figure class="action" @click="transferToken(token)" v-tooltip.left="'Transfer'"><i class="fas fa-exchange-alt"></i></figure>
						</section>
					</section>
				</section>
			</section>

			<section key="2" class="container centered" v-if="status === STATUS.LOADING">
				<PlasmaBall text="Reading the daily gossip." />
			</section>

			<section key="3" class="container centered" v-if="status === STATUS.ERROR">
				<section class="transfer-panel red">
					<figure class="check-circle">
						<i class="fas fa-times"></i>
					</figure>
					<figure class="title">Oh no!</figure>
					<figure class="text">
						{{error}}
					</figure>
					<button @click="status = STATUS.TOKENS">Try again</button>
				</section>
			</section>
		</transition>

	</section>
</template>

<script>

	const { Client, TokenInfoQuery, AccountBalanceQuery, TokenDissociateTransaction } = require("@hashgraph/sdk");

	const STATUS = {
		TOKENS:0,
		LOADING:1,
		ERROR:2,
	}

	export default {
		data(){return {
			status:STATUS.TOKENS,
			STATUS,
			selectedToken:null,
			error:null,

			loadingToken:null,
		}},
		async mounted(){

		},
		computed:{

		},
		methods:{
			async removeToken(token){
				this.status = STATUS.LOADING;
				const client = this.getClient();
				const tx = await new TokenDissociateTransaction()
					.setAccountId(this.activeAccount.name)
					.setTokenIds([token.id])
					.execute(client).catch(err => {
						//console.error("Token dissociate error", err);
						this.status = STATUS.ERROR;
						this.error = err;
						return null;
					});

				if(tx) {
					if(await tx.getReceipt(client).catch(err => {
						//console.error("Error sending", err);
						this.status = STATUS.ERROR;
						this.error = err;
						return null;
					})){
						await new Promise(r => setTimeout(r, 1000));
						await this.getAccountInfo();
						this.status = STATUS.TOKENS;
					}
				}

			},
			transferToken(token){
				this.$router.push({path:'/transfer', query:{tokenId:token.id}})
			},
			async loadTokenInfo(tokenId){
				this.loadingToken = tokenId;
				const client = this.getClient();
				let query = await new TokenInfoQuery()
					.setTokenId(tokenId)
					.execute(client).catch(err => {
						//console.error("Token get info error", err);
						this.status = STATUS.ERROR;
						this.error = err;
						this.loadingToken = null;
						return null;
					});


				if(query){
					await this.setTokenMeta(tokenId, query);
					this.loadingToken = null;

					setTimeout(() => {
						this.getAccountInfo();
					}, 1000);
				}
			},
		},
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

		.no-tokens {
			display:flex;
			width:100%;
			height:100%;
			justify-content: center;
			align-items: center;
			flex-direction: column;
			padding: 0 20px;

			span {
				font-size: 18px;
				display:block;
				margin-bottom:15px;
			}
		}

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

		.tokens {
			width:100%;
			max-height: calc(100vh - 120px);
			overflow-y: auto;

			.token {
				display:flex;
				align-items: flex-start;
				border-bottom:1px solid rgba(0,0,0,0.04);
				padding:10px 5px 10px 0;

				.info {
					flex:1;
					text-align:left;
					padding:0 10px;

					&.no-pad {
						padding-left:0;
					}

					.id {
						font-size: 11px;
					}

					.name {
						font-size: 13px;
						font-weight: bold;
						color:$blue;
					}

					.balance {
						font-size: 16px;
						font-weight: bold;
					}
				}

				.actions {
					flex: 0 0 auto;
					display:flex;

					.action {
						margin:0 2px;
						border-radius:4px;
						box-shadow:0 1px 3px rgba(0,0,0,0.1), 0 3px 8px rgba(0,0,0,0.03);
						color:rgba(0,0,0,0.45);
						height:30px;
						width:30px;
						display:flex;
						align-items: center;
						justify-content: center;
						cursor: pointer;
						transition: all 0.2s ease;
						transition-property: box-shadow, color, transform;

						i {
							font-size: 14px;
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
		}
	}
</style>
