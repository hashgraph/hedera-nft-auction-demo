<template>
	<section class="view-private-key">

		<figure class="darkener"></figure>

		<section class="container">
			<section class="details">
				<textarea style="height:360px; resize: none;" v-model="privateKey"></textarea>
			</section>

			<section class="actions">
				<div>
					Alright, <span @click="$emit('done', false)">I'm done.</span>
				</div>
			</section>
		</section>
	</section>
</template>

<script>
	import {Mnemonic, PrivateKey} from '@hashgraph/sdk';
	import InternalMessage from "../messages/InternalMessage";
	import * as InternalMessageTypes from "../messages/InternalMessageTypes";

	export default {
		props:['account'],
		data(){return {
			privateKey:null,
		}},
		mounted(){
			this.getPrivateKey();
		},
		destroyed(){
			this.privateKey = null;
		},
		methods:{
			async getPrivateKey(){
				this.privateKey = await InternalMessage.payload(InternalMessageTypes.GET_PRIVATE_KEY, {account:this.account}).send();
			},
		}
	}
</script>

<style lang="scss" scoped>
	@import '../styles/variables';

	.view-private-key {
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
