import '../styles/styles.scss'
import VueInitializer from '../vue/VueInitializer';
import {Routing, RouteNames, promptPrefix} from '../vue/Routing';
import * as InternalMessageTypes from "../messages/InternalMessageTypes";
import InternalMessage from "../messages/InternalMessage";
import {apis} from '../util/BrowserApis';
import PromptService from "../services/PromptService";
import store from '../store'

import Dropdown from '../components/Dropdown.vue'
import Swiper from '../components/Swiper.vue' // Swiper, no swiping!
import Navbar from '../components/Navbar.vue'
import PlasmaBall from '../components/PlasmaBall.vue'
import AnimatedNumber from '../components/AnimatedNumber.vue'
import ImportAccount from '../components/ImportAccount.vue'
import ViewPrivateKey from '../components/ViewPrivateKey.vue'

class Popup {

	constructor(){
		let prompt = window.data || apis.extension.getBackgroundPage().injectedPrompt || null;

		const components = [
			{tag:'Dropdown', vue:Dropdown},
			{tag:'Swiper', vue:Swiper},
			{tag:'Navbar', vue:Navbar},
			{tag:'PlasmaBall', vue:PlasmaBall},
			{tag:'AnimatedNumber', vue:AnimatedNumber},
			{tag:'ImportAccount', vue:ImportAccount},
			{tag:'ViewPrivateKey', vue:ViewPrivateKey},
		];

		(async() => {
			const isUnlocked = prompt ? true : await InternalMessage.signal(InternalMessageTypes.IS_UNLOCKED).send();

			const middleware = async (to, next, store) => {
				// '/' is a non-route, and always the first to land on upon instantiating vue (by vue default)
				if(to.path === '/') return next();
				// Prompts can only travel to prompt routes
				if(prompt) return to.path.indexOf(promptPrefix) > -1 ? next() : PromptService.close();
				// Checking for restricted routes when locked
				if(Routing.isRestricted(to.path))
					return await InternalMessage.signal(InternalMessageTypes.IS_UNLOCKED).send()
						? next()
						: next({name:RouteNames.Login});
				// Ok
				next();
			};

			const router = new VueInitializer(Routing.routes(), components, middleware);
			if(prompt) {
				store.dispatch('setPrompt', prompt);
				router.push(`/${promptPrefix}${prompt.type}`);
			}
			else router.push(isUnlocked ? '/main' : '/login');
		})();
	}

}

new Popup();
