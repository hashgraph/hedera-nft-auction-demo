<template>
	<section class="navbar" :class="{'account-selector':accountSelector}">
		<section class="account-selector" v-if="iv" :class="{'show':accountSelector}">
			<ImportAccount v-if="importingAccount" v-on:done="importingAccount = false" />
			<ViewPrivateKey v-if="showingPrivateKey" :account="showingPrivateKey" v-on:done="showingPrivateKey = null" />
			<section class="accounts">
				<section @click="importingAccount = true" class="account">
					<figure class="identicon" @click="accountSelector = !accountSelector">
						<img :src="`data:image/png;base64, ${getIdenticon('Import account')}`" />
					</figure>
					<section class="account-info">
						<figure class="id">Import an account</figure>
						<figure class="network">You can select this option if you want to add a new account to your wallet, however it must already exist.</figure>
					</section>
				</section>
			</section>
			<section class="accounts">
				<section :class="{'active':activeAccount && activeAccount.id === account.id, 'pressed':accountOptions === account.id}"
				         @click="openAccountActions(account)" :key="`${account.id}:${account.network}`" class="account" v-for="account in iv.keychain.accounts">
					<figure class="identicon" @click="selectAccount(account)">
						<img :src="`data:image/png;base64, ${getIdenticon(account.name + account.network)}`" />
					</figure>
					<section style="width:100%; position:relative; flex:1;">
						<transition name="slide-left" mode="out-in">
							<section key="account-info" class="account-info" v-if="accountOptions !== account.id">
								<figure class="id">{{account.name}}</figure>
								<figure class="network">{{account.network}}</figure>
							</section>
						</transition>
						<transition name="slide-left" mode="out-in">
							<section key="account-actions" class="account-actions" v-if="accountOptions === account.id && !removingAccount">
								<figure v-if="iv.keychain.accounts.length > 1 && (activeAccount && activeAccount.id !== account.id)" class="action"
								        @click="removeAccount(account)"
								        v-tooltip.left="{offset:5, content:activeAccount && activeAccount.id === account.id ? null : 'Remove'}"><i class="fas fa-times"></i></figure>
								<figure class="action" @click="showingPrivateKey = account"
								        v-tooltip.left="{offset:5, content:'View private key'}"><i class="fas fa-key"></i></figure>
								<figure v-if="activeAccount && activeAccount.id !== account.id" class="action" @click="selectAccount(account)"
								        v-tooltip.left="{offset:5, content:'Select'}"><i class="fas fa-check"></i></figure>
							</section>
							<section key="removing-account" class="account-actions" v-if="accountOptions === account.id && removingAccount">
								<figure class="action warn" @click="cancelAccountRemoval" style="flex:0 0 auto; width:auto;"><span>Cancel ({{removeAccountTimerCounter}})</span></figure>
							</section>
						</transition>
					</section>
				</section>
			</section>
		</section>
		<section class="nav-container">
			<img v-if="!showGoBack" alt="logo" src="../assets/logo.png">
			<figure v-else @click="$router.back()" class="go-back">
				<i class="fas fa-arrow-left"></i>
			</figure>

			<figure class="info" v-if="hasAccount">
				<section class="details">
					<figure class="account" v-if="activeAccount"><b>{{activeAccount.name}}</b></figure>
					<figure class="account" v-else>No account for this network</figure>
					<figure class="balance" @click="reloadBalance" v-tooltip.bottom="'Reload Balance'" v-if="hbarBalance"><AnimatedNumber :number="hbarBalance" :decimals="8" /> <b>HBAR</b></figure>
					<figure class="balance" v-if="!hbarBalance && activeAccount"><i class="fa fa-spinner fa-spin"></i></figure>
					<figure class="balance" v-if="!hbarBalance && !activeAccount"><!-- placeholder, don't remove --></figure>
					<figure class="network" v-if="activeAccount"><b>{{activeAccount.network}}</b></figure>
				</section>
				<figure class="identicon" @click="accountSelector = !accountSelector">
					<img :src="`data:image/png;base64, ${getIdenticon(activeAccount.name + activeAccount.network)}`" />
				</figure>
			</figure>

			<figure class="info" @click="accountSelector = !accountSelector" v-else>
				<section class="details">
					<figure class="account"><b>You don't have an account yet</b></figure>
					<figure class="network"><b>Click here to import one</b></figure>
				</section>
				<figure class="identicon">
					<img :src="`data:image/png;base64, ${getIdenticon('No account yet')}`" />
				</figure>
			</figure>
		</section>
	</section>
</template>

<script>
	import {mapActions} from "vuex";
	import Identicon from 'identicon.js';
	import Hasher from "../util/Hasher";
	import InternalMessage from "../messages/InternalMessage";
	import * as InternalMessageTypes from "../messages/InternalMessageTypes";

	let lastAccountOptions = null;
	export default {
		props:['showGoBack'],
		data(){return {
			accountSelector:false,
			importingAccount:false,
			accountOptions:null,
			showingPrivateKey:null,
			removingAccount:null,
			removeAccountTimer:null,
		}},
		async mounted(){

		},
		methods:{
			removeAccount(account){
				if(this.activeAccount.id === account.id) return;
				this.removingAccount = account.id;

				this.removeAccountTimerCounter = 3;
				this.removeAccountTimer = setInterval(() => {
					if(!this.removingAccount) clearInterval(this.removeAccountTimer);
					if(this.removeAccountTimerCounter === 0) {
						clearInterval(this.removeAccountTimer);
						this.completeRemoveAccount();
					}
					else {
						this.removeAccountTimerCounter--;
						this.$forceUpdate();
					}
				}, 1000);
			},
			cancelAccountRemoval(){
				clearInterval(this.removeAccountTimer);
				this.removingAccount = null;
				this.removeAccountTimerCounter = null;
			},
			async completeRemoveAccount(){
				const account = this.iv.keychain.accounts.find(x => x.id === this.removingAccount);
				const deleted = await InternalMessage.payload(InternalMessageTypes.DELETE_ACCOUNTS, [account]).send();
				this.regenerateIVData();
				this.removingAccount = null;
				this.removeAccountTimerCounter = null;
			},
			reloadBalance(){
				this.setBalance(null);
				this.getAccountInfo();
			},
			openAccountActions(account){
				if(lastAccountOptions === account.id) return;
				// Account removal only works with 1 account,
				// don't change while removing
				this.cancelAccountRemoval();
				this.accountOptions = account.id;
				lastAccountOptions = account.id;
			},
			async selectAccount(account){
				this.setBalance(null);
				await InternalMessage.payload(InternalMessageTypes.SET_ACTIVE_ACCOUNT, account).send();
				await this.setActiveAccount(account);
				this.accountOptions = null;
				setTimeout(() => {
					this.accountSelector = false;
					lastAccountOptions = null;
				}, 300);
			},
			getIdenticon(text){
				return new Identicon(Hasher.md5hex(text), {
					background: [255, 255, 255, 0],
					margin: 0,
					size: 40,
				}).toString();
			},
			...mapActions([
				'setBalance',
				'setActiveAccount',
			])
		},
		watch:{
			activeAccount(){
				this.getAccountInfo();
			},
		}
	}
</script>

<style lang="scss" scoped>
	@import "../styles/variables";

	.navbar {
		width:100%;

		.account-selector {
			flex:1;
			height:0;
			overflow:hidden;
			background:rgba(0,0,0,0.01);
			margin: 0 -30px;
			width: calc(100% + 60px);
			padding:0 30px;

			transition: all 0.25s ease;
			transition-property: height, padding;
			&.show {
				height:calc(100vh - 80px);
				padding:30px;
			}

			.slide-right-enter-active, .slide-right-leave-active {
				transition: all 0.1s ease;
				transition-property: opacity, transform;
				position:absolute;
			}
			.slide-right-enter, .slide-right-leave-to {
				opacity: 0;
				transform:translateX(50px);
			}

			.slide-left-enter-active, .slide-left-leave-active {
				transition: all 0.1s ease;
				transition-property: opacity, transform;
				position:absolute;
			}
			.slide-left-enter, .slide-left-leave-to {
				opacity: 0;
				transform:translateX(50px);
			}

			.accounts {
				overflow-y:auto;
				max-height:calc(100vh - 225px);
				padding-right:10px;
				margin-right:-10px;
				padding-top:3px;
				overflow-x:hidden;

				.account {
					position: relative;
					background:white;
					margin:0 0 10px;
					padding:10px;
					border-radius:4px;
					box-shadow:0 1px 3px rgba(0,0,0,0.1), 0 3px 8px rgba(0,0,0,0.03);
					color:rgba(0,0,0,0.45);
					display:flex;
					align-items: center;
					width:100%;
					border:1px solid transparent;

					&.active {
						border:1px solid $blue;
					}

					cursor: pointer;
					transition: all 0.2s ease;
					transition-property: box-shadow, color, transform, border;

					&:not(.pressed){
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

					&.pressed {
						box-shadow:0 1px 2px rgba(0,0,0,0.16), 0 2px 3px rgba(0,0,0,0.10);
						transform:translateY(2px);
					}

					.account-info {
						padding:0 10px;
						text-align:left;

						.id {
							font-size: 13px;
							font-weight: bold;
							color:$blue;
						}

						.network {
							font-size: 9px;
							color:$grey;
						}
					}

					.account-actions {
						padding:0 10px;
						display:flex;
						justify-content: flex-end;
						width:100%;
						position: relative;

						.action {
							margin:0 2px;
							border-radius:4px;
							box-shadow:0 1px 3px rgba(0,0,0,0.1), 0 3px 8px rgba(0,0,0,0.03);
							color:rgba(0,0,0,0.45);
							height:34px;
							width:34px;
							display:flex;
							align-items: center;
							justify-content: center;
							cursor: pointer;
							transition: all 0.2s ease;
							transition-property: box-shadow, color, transform;

							i {
								font-size: 14px;
							}

							span {
								font-size: 14px;
								padding:0 10px;
								font-weight: bold;
							}

							&:not(.disabled){
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

							&.warn {
								background:$red;
								color:white !important;
							}

							&.disabled {
								opacity:0.4;
							}
						}
					}
				}
			}
		}

		.nav-container {
			display:flex;
			justify-content: space-between;
			align-items: center;
			align-self: normal;
			padding: 20px 30px 20px;
			margin: 0 -30px;
			width: calc(100% + 60px);
			box-shadow: 0 -20px 40px rgb(0 0 0 / 3%), 0 -2px 4px rgba(0,0,0,0.02);
		}

		.identicon {
			margin-left:10px;
			border-radius:50%;
			overflow:hidden;
			box-shadow:inset 0 0 10px rgba(0,0,0,0.1), inset 0 -2px 8px rgba(0,0,0,0.05), inset 0 0 20px rgba(0,0,0,0.08);
			width:40px;
			height:40px;
			flex:0 0 auto;

			img {
				width:40px;
				height:40px;
				transform:rotateZ(10deg);
			}

			cursor: pointer;
			transition: all 0.2s ease;
			transition-property: box-shadow, transform;

			&:hover {
				box-shadow:inset 0 0 0 rgba(0,0,0,0), inset 0 0 0 rgba(0,0,0,0), inset 0 0 0 rgba(0,0,0,0),
				0 6px 13px rgba(0,0,0,0.1), 0 12px 34px rgba(0,0,0,0.05);
				transform:translateY(-2px) rotateZ(5deg);
			}

			&:active {
				box-shadow:inset 0 0 0 rgba(0,0,0,0), inset 0 0 0 rgba(0,0,0,0), inset 0 0 0 rgba(0,0,0,0),
				0 1px 2px rgba(0,0,0,0.12), 0 3px 5px rgba(0,0,0,0.07);
				transform:translateY(2px) rotateZ(2deg);
			}
		}

		img {
			opacity:0.5;
			width:40px;
		}

		.go-back {
			font-size: 24px;
			color:$grey;
			cursor: pointer;
		}

		.info {
			text-align:right;
			display:flex;
			align-items: center;



			.account {
				font-size: 11px;
				color:$blue;
				font-weight: bold;
			}

			.balance {
				font-size: 11px;
				height:13px;
				cursor: pointer;

				&:hover {
					text-decoration: underline;
				}
			}

			.network {
				color:$lightgrey;
				font-size: 9px;
			}
		}

	}
</style>
