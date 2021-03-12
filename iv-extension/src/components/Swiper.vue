<template>
	<section class="swipe-container">
		<figure :style="{'opacity':swipePercentage}" class="fill"></figure>
		<figure class="deny" :class="{'no-hover':noCancel || swipePercentage > PERCENTAGE_TO_SWIPE}" @click="deny">
			<i v-if="!noCancel" :style="{'transform':`translateX(${clamp(-60, 0, 1 - swipePercentage*40)}px)`, 'opacity':1 - swipePercentage - parseFloat(swipePercentage * 0.5)}" class="fas fa-times"></i>
			<i :style="{'transform':`translateX(-${50 - clamp(0, 50, swipePercentage*80)}px)`, 'opacity':swipePercentage}" class="fas fa-check"></i>
		</figure>
		<figure class="swiper" ref="swiper">
			<span :style="{'color':`rgba(${clampedTextColor}, ${clampedTextColor}, ${clampedTextColor}, ${clamp(0.5, 1, swipePercentage)})`}">{{text || 'Slide to approve'}}</span>
			<figure :class="{'quick-move':dragging, 'pop':!dragging && swipePercentage > PERCENTAGE_TO_SWIPE}" :style="{left:`${sliderLeft + 2}px`}" ref="slider" class="slider" @mousedown="dragStart">
				<i :style="{'opacity':1 - swipePercentage - parseFloat(swipePercentage * 0.5)}" class="fas fa-arrow-right"></i>
				<!--<i :style="{'opacity':swipePercentage}" class="fas fa-check"></i>-->
			</figure>
		</figure>
	</section>
</template>

<script>
	const PERCENTAGE_TO_SWIPE = 0.5;

	export default {
		props:['text', 'noCancel'],
		data(){return {
			dragging:false,
			sliderLeft:0,
			swipePercentage:0,
			PERCENTAGE_TO_SWIPE,
			done:false,
		}},
		mounted(){
			document.addEventListener('mouseup', this.dragStop);
			document.addEventListener('mousemove', this.dragMove);
		},
		destroyed(){
			document.removeEventListener('mouseup', this.dragStop);
			document.removeEventListener('mousemove', this.dragMove);
		},
		computed:{
			clampedTextColor(){
				return this.clamp(100, 255, this.swipePercentage*400);
			},
			swiperBox(){
				return this.$refs.swiper.getBoundingClientRect();
			},
			swiperStart(){
				return this.swiperBox.x;
			},
			swiperEnd(){
				const box = this.swiperBox;
				return box.x + box.width;
			}
		},
		methods:{
			clamp(min, max, val){
				if(val > max) val = max;
				if(val < min) val = min;
				return val;
			},
			dragStart(event){
				if(this.done) return;
				this.dragging = true;
			},
			async dragStop(event){
				if(this.done) return;
				this.dragging = false;
				if(this.swipePercentage > PERCENTAGE_TO_SWIPE){
					this.swipePercentage = 1;
					this.sliderLeft = this.swiperEnd - this.$refs.slider.getBoundingClientRect().width - 31;
					await new Promise(r => setTimeout(r, 500));
					this.$emit('approved', true);
					this.done = true;
				} else {
					this.swipePercentage = 0;
					this.sliderLeft = 0;
				}
			},
			dragMove(event){
				if(this.done) return;
				if(!this.dragging) return;
				const mousePosX = event.clientX;
				this.swipePercentage = (mousePosX - this.swiperStart) / this.swiperEnd;
				this.sliderLeft = mousePosX - this.swiperStart - 26;
				if(this.sliderLeft < 0) this.sliderLeft = 0;
				if(this.sliderLeft > this.swiperEnd - this.$refs.slider.getBoundingClientRect().width - 31)
					this.sliderLeft = this.swiperEnd - this.$refs.slider.getBoundingClientRect().width - 31;
			},
			deny(){
				if(this.done) return;
				this.$emit('denied', true);
				this.done = true;
			}
		}
	}
</script>

<style lang="scss" scoped>
	@import "../styles/variables";


	$height:60px;

	.swipe-container {
		border-radius:150px;
		background:transparent;
		background:$red;
		font-size: 14px;
		outline:0;
		width:100%;
		height:$height;
		display:flex;
		justify-content: center;
		align-items: center;
		padding:0 40px;
		text-align:center;
		position: relative;

		max-width: 500px;
		margin: 0 auto 5px;

		transition:all 0.2s ease;
		transition-property: border;


		.fill {
			cursor: pointer;
			position:absolute;
			top:0;
			left:0;
			bottom:0;
			width:100%;
			background:$blue;
			border-radius:150px;
			transition: all 0.2s ease;
			transition-property: background;
			opacity:0;
		}

		.deny {
			position:absolute;
			top:3px;
			right:3px;
			bottom:3px;
			width:calc(#{$height} + 20px);
			display:flex;
			align-items: center;
			justify-content: center;
			color:white;
			background:transparent;
			border-radius:150px;
			border-top-left-radius:0;
			border-bottom-left-radius:0;
			transition: all 0.2s ease;
			transition-property: background;
			font-size: 22px;

			i {
				padding-left:25px;
				position:absolute;
				top:0;
				bottom:0;
				left:0;
				right:0;
				display:flex;
				align-items: center;
				justify-content: center;

				transition:all 0.2s ease;
				transition-property: transform;
			}

			&:not(.no-hover){
				cursor: pointer;
				&:hover {
					background:rgba(255,255,255,0.2);
				}

				&:active {
					background:rgba(0,0,0,0.1);
				}
			}

		}

		.swiper {
			position:absolute;
			top:1px;
			left:1px;
			bottom:1px;
			border-radius:150px;
			background:white;
			font-size: 14px;
			outline:0;
			width:calc(100% - 52px);
			height:calc(#{$height} - 2px);
			display:flex;
			justify-content: center;
			align-items: center;
			padding:0 40px;
			text-align:center;
			margin-bottom:5px;
			box-shadow: 10px 0 15px rgb(0 0 0 / 10%), inset 0 0 0 2px white, inset 0 0 31px 10px rgb(0 0 0 / 8%);

			transition:all 0.2s ease;
			transition-property: border;

			span {
				user-select: none;
				font-size: 13px;
				padding-left: 34px;
			}

			.slider {
				cursor: pointer;
				position:absolute;
				top:2px;
				left:2px;
				bottom:2px;
				width:calc(#{$height} - 4px);
				display:flex;
				align-items: center;
				justify-content: center;
				color:white;
				background:$blue;
				border-radius:50%;
				transition: all 0.2s ease;
				transition-property: background, transform, left;

				&.quick-move {
					transition-property: background, transform;
				}

				i {
					user-select: none;
					position:absolute;
					top:0;
					bottom:0;
					left:0;
					right:0;
					display:flex;
					align-items: center;
					justify-content: center;
					font-size: 22px;
				}

				&:hover {
					background: $slightlightblue;
				}

				&:active {
					background: $lightblue;
				}

				@keyframes pop-slider {
					0% { transform:scale(1); opacity:1; }
					50% { transform:scale(0.7); opacity:0.8; }
					100% { transform:scale(1.2); opacity:0; }
				}

				&.pop {
					background: $lightblue;
					animation: pop-slider 0.3s  forwards;
					animation-delay: 0.08s;
				}
			}
		}
	}

</style>
