<template>
	<section class="dropdown">
		<figure class="selected" @click="show = !show">{{selected || 'Select an option'}}</figure>
		<section class="options" :class="{'show':show, 'scrollbars':showScrollbar}">
			<figure :key="n" @click="select(item)" class="option" v-for="(item, n) in options">{{item}}</figure>
		</section>
	</section>
</template>

<script>
	export default {
		props:['options', 'selected'],
		data(){return {
			show:false,
			showScrollbar:false,
		}},
		methods:{
			select(item){
				this.show = false;
				this.$emit('selected', item);
			}
		},
		watch:{
			'show'(){
				if(!this.show) this.showScrollbar = false;
				if(this.show) setTimeout(() => this.showScrollbar = true, 210);
			}
		}
	}
</script>

<style lang="scss" scoped>
	@import "../styles/variables";

	.dropdown {
		width:100%;
		position: relative;
		z-index:2;
		margin-bottom:5px;

		.selected {
			position: relative;
			border-radius:4px;
			padding:10px 20px;
			cursor: pointer;
			width:100%;
			font-size: 14px;
			color:$black;
			background:white;

			i {
				font-size: 18px;
			}

			div {
				margin-top:10px;
				font-size: 13px;
				display:none;
			}

			box-shadow:0 1px 3px rgba(0,0,0,0.1), 0 3px 8px rgba(0,0,0,0.03);
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

		.options {
			position:absolute;
			left:5px;
			right:5px;
			top:calc(100% - 5px);
			background: #fbfbfb;
			border-radius:4px;
			max-height:0;
			z-index:-1;
			box-shadow:0 1px 3px rgba(0,0,0,0.1);
			padding:0;
			overflow-x: hidden;
			overflow-y: hidden;
			opacity:0;
			font-size: 11px;

			transition: all 0.2s ease;
			transition-property: max-height, padding, opacity;

			&.show {
				max-height:150px;
				padding:10px 0 5px;
				opacity:1;
			}

			&.scrollbars {
				overflow-y: auto;
			}

			.option {
				padding:5px 10px;
				cursor: pointer;

				transition: all 0.1s ease;
				transition-property: background;

				&:hover {
					background:rgba(0,0,0,0.04);
				}

				&:active {
					background:rgba(0,0,0,0.07);
				}
			}
		}
	}
</style>
