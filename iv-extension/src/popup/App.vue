<template>
	<div id="app" :class="{'confine':!prompt}">

		<transition :name="routerTransition" mode="out-in">
			<router-view/>
		</transition>
	</div>
</template>

<script>
	import {mapState} from "vuex";

	export default {
		data(){return {
			routerTransition:null,
		}},
		mounted(){
			setTimeout(() => {
				this.routerTransition = 'router-move';
			}, 500);
		},
		computed:{
			...mapState([
				'prompt'
			])
		}
	}
</script>

<style lang="scss">
	@import "../styles/variables";

	#app {
		font-family: Avenir, Helvetica, Arial, sans-serif;
		-webkit-font-smoothing: antialiased;
		-moz-osx-font-smoothing: grayscale;
		text-align: center;
		color: #2c3e50;
		position: relative;
		overflow: hidden;

		&.confine {
			width:350px;
			height:600px;
		}
	}
</style>
