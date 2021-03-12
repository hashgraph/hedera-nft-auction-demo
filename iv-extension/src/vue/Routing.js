import * as PromptTypes from "../models/prompts/PromptTypes";
import Login from '../views/Login';
import MainScreen from '../views/MainScreen';
import PromptLogin from '../views/PromptLogin';
import PromptSignature from '../views/PromptSignature';
import PromptAddAccount from '../views/PromptAddAccount';
import Blank from '../views/Blank';
import Transfer from '../views/Transfer';
import AssociateToken from '../views/AssociateToken';
import ViewTokens from '../views/ViewTokens';


export const promptPrefix = 'prompt-';

export const RouteNames = {
	Blank:'blank',
	Login:'login',
	MainScreen:'main',
	Transfer:'transfer',
	AssociateToken:'associate',
	ViewTokens:'tokens',

	PromptLogin:`${promptPrefix}${PromptTypes.REQUEST_LOGIN}`,
	PromptSignature:`${promptPrefix}${PromptTypes.REQUEST_SIGNATURE}`,
	PromptAddAccount:`${promptPrefix}${PromptTypes.REQUEST_ADD_ACCOUNT}`,
};

const RouteViews = {
	[RouteNames.Blank]:Blank,
	[RouteNames.Login]:Login,
	[RouteNames.MainScreen]:MainScreen,
	[RouteNames.Transfer]:Transfer,
	[RouteNames.AssociateToken]:AssociateToken,
	[RouteNames.ViewTokens]:ViewTokens,

	[RouteNames.PromptLogin]:PromptLogin,
	[RouteNames.PromptSignature]:PromptSignature,
	[RouteNames.PromptAddAccount]:PromptAddAccount,
};

export class Routing {

	static builder(){
		const routeNames = Object.keys(RouteNames).map(key => RouteNames[key]);

		let routesBuilder = {};
		routeNames.map(routeName => {
			routesBuilder[routeName] = {
				path:routeName === RouteNames.Blank ? '/' : '/'+routeName,
				name:routeName,
				component: RouteViews[routeName]
			}
		});

		return routesBuilder;
	}

	static routes(){
		return Object.keys(Routing.builder())
			.map(routeName => Routing.builder()[routeName]);
	}

	static isRestricted(routeName) {
		if(routeName.indexOf(promptPrefix) > -1) return false;
		return ![`/login`, '/'].includes(routeName);
	}

}
