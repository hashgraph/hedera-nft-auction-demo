import {LocalStream} from 'extension-streams';
import * as InternalMessageTypes from './messages/InternalMessageTypes';
import InternalMessage from './messages/InternalMessage';
import StorageService from './services/StorageService'
import {apis} from './util/BrowserApis';
import StoredData, {Account} from "./models/StoredData";
import Hasher from "./util/Hasher";
import {AES} from "aes-oop";
import PromptService from "./services/PromptService";
import Prompt from "./models/prompts/Prompt";
import * as PromptTypes from "./models/prompts/PromptTypes";
import LoginPermission from "./models/LoginPermission";
import Error from './models/errors/Error';

const {
	PrivateKey,
	Mnemonic,
	Client,
	AccountBalanceQuery,
	AccountId,
	Transaction
} = require("@hashgraph/sdk");

const {TransactionBody} = require('@hashgraph/proto');

let setupState = null;
let seed = null;
let storedData = null;
let prompt = null;

export default class Background {
	constructor(){
		this.setupInternalMessaging();
	}

	// Watches the internal messaging system ( LocalStream )
	setupInternalMessaging(){
		LocalStream.watch((request, sendResponse) => {
			const message = InternalMessage.fromJson(request);
			//console.log('got message', message, sendResponse);

			// Only messages allowed are those defined in InternalMessageTypes
			if(!Object.values(InternalMessageTypes).includes(message.type)) return sendResponse(null);
			// Only ones that have function definitions
			if(typeof this[message.type] !== "function") return sendResponse(null);

			return this[message.type](sendResponse, message.payload);
		})
	}

	async [InternalMessageTypes.GET_SETUP_STATE](sendResponse){
		sendResponse(setupState)
	}

	async [InternalMessageTypes.SETUP](sendResponse, rawData){
		const {state, data, nextState} = rawData;

		setupState = nextState;

		if(state === 'password'){
			seed = await Hasher.secureHash(data);

			const mnemonic = await Mnemonic.generate();
			storedData = StoredData.placeholder();
			storedData.keychain.encryptedMnemonic = AES.encrypt(mnemonic.toString(), seed);
			return sendResponse(mnemonic.words);
		}

		if(state === 'mnemonic'){
			return sendResponse(true);
		}

		if(state === 'finishedSetup'){
			// const privateKey = await PrivateKey.generate();
			// storedData.keychain.addAccount('no_account', privateKey, 'Mainnet', seed);
			const saved = await StorageService.save(storedData, seed);
			sendResponse(!!saved);
		}
	}

	async [InternalMessageTypes.RESTORE](sendResponse, {mnemonicOrPrivateKey, password, accounts, network}){
		seed = await Hasher.secureHash(password);

		storedData = StoredData.placeholder();
		let key = null;

		const isMnemonic = mnemonicOrPrivateKey.indexOf(' ') > -1;
		if(isMnemonic){
			try {
				const mnemonic = await Mnemonic.fromString(mnemonicOrPrivateKey);
				storedData.keychain.encryptedMnemonic = AES.encrypt(mnemonic.toString(), seed);
				key = await mnemonic.toPrivateKey();
			} catch(e){
				//console.error("Error restoring mnemonic", e);
				return sendResponse(false);
			}
		} else {
			try {
				// Private key imported
				key = PrivateKey.fromString(mnemonicOrPrivateKey);
			} catch(e){
				//console.error("Error restoring private key", e);
				return sendResponse(false);
			}
		}

		accounts.map(account => storedData.keychain.addAccount(account, key, network, seed));
		sendResponse(await StorageService.save(storedData, seed));
	}

	async [InternalMessageTypes.INSERT_ACCOUNTS](sendResponse, {accounts, privateKey}){
		const key = PrivateKey.fromString(privateKey);
		accounts.map(account => storedData.keychain.addAccount(account.name, key, account.network, seed));
		sendResponse(await StorageService.save(storedData, seed));
	}

	async [InternalMessageTypes.DELETE_ACCOUNTS](sendResponse, accounts){
		accounts.map(account => storedData.keychain.removeAccount(account));
		sendResponse(await StorageService.save(storedData, seed));
	}

	async [InternalMessageTypes.GET_PRIVATE_KEY](sendResponse, {account}){
		const privateKey = storedData.keychain.getPrivateKeyFor(account, seed);
		sendResponse(privateKey.toString());
	}

	async [InternalMessageTypes.UNLOCK](sendResponse, data){
		seed = await Hasher.secureHash(data);
		storedData = await StorageService.get(seed);
		sendResponse(!!storedData);
	}

	async [InternalMessageTypes.LOCK](sendResponse, data){
		seed = null;
		storedData = null;
		sendResponse(true);
	}

	[InternalMessageTypes.IS_UNLOCKED](sendResponse, data){
		sendResponse(seed !== null);
	}

	async [InternalMessageTypes.HAS_DATA](sendResponse){
		sendResponse(!!await StorageService.hasData());
	}

	async [InternalMessageTypes.GET_DATA](sendResponse){
		sendResponse(storedData.safe());
	}

	async [InternalMessageTypes.DESTROY](sendResponse, data){
		await StorageService.remove();
		sendResponse(true);
	}

	async [InternalMessageTypes.INTERNAL_SIGN](sendResponse, data){
		try {
			let {data:buffer, account} = data;
			if(!Array.isArray(buffer) && typeof buffer === 'object') buffer = Object.values(buffer);
			buffer = Buffer.from(buffer);

			sendResponse(storedData.keychain.signWithAccount(buffer, account, seed));
		} catch(e){
			//console.error("There was an error while trying to sign a transaction", e);
			sendResponse(null);
		}
	}

	async [InternalMessageTypes.SET_TOKEN_META](sendResponse, {id, meta}){
		sendResponse(await StorageService.setTokenMeta(id, meta));
	}

	async [InternalMessageTypes.GET_TOKEN_META](sendResponse){
		sendResponse(await StorageService.getTokenMeta());
	}

	async [InternalMessageTypes.SET_ACTIVE_ACCOUNT](sendResponse, account){
		sendResponse(await StorageService.setActiveAccount(account));
	}

	async [InternalMessageTypes.GET_ACTIVE_ACCOUNT](sendResponse){
		sendResponse(await StorageService.getActiveAccount());
	}


	/*******************************************/
	/************ EXPOSED FOR APPS *************/
	/*******************************************/

	[InternalMessageTypes.LOGIN_FROM_PERMISSION](sendResponse, data){
		this.lockGuard(sendResponse, () => {
			const {domain} = data;
			const accountFromPermission = storedData.keychain.getLoginPermission(domain, true);
			if(!accountFromPermission) return sendResponse(null);
			sendResponse(accountFromPermission.forConnector());
		})
	}

	[InternalMessageTypes.LOGIN](sendResponse, data){
		this.lockGuard(sendResponse, async () => {
			const {domain, network = 'Mainnet'} = data;

			const accountFromPermission = storedData.keychain.getLoginPermission(domain, true);
			if(accountFromPermission) {
				if(accountFromPermission.network === network) {
					return sendResponse(accountFromPermission.forConnector());
				} else {
					// Removing any other permission now, otherwise signer below will
					// allow signing with an account it shouldn't have access to.
					storedData.keychain.removeLoginPermission(domain);
					await StorageService.save(storedData, seed);
				}
			}




			// TODO: Fix for multiple accounts
			const atLeastOneAccount = storedData.keychain.accounts.find(x => x.network === network);
			if(!atLeastOneAccount) return sendResponse(Error.loginError("no_account", `User does not have an account for this network (${network})`));

			PromptService.open(new Prompt(storedData.safe(), PromptTypes.REQUEST_LOGIN, domain, network, {domain, network}, async accountId => {
				if(accountId){
					const account = storedData.keychain.accounts.find(x => x.network === network && x.name === accountId);
					storedData.keychain.addLoginPermission(LoginPermission.fromJson({
						domain,
						network,
						account_id:account.id
					}));

					const savedPermission = await StorageService.save(storedData, seed);
					return sendResponse(account.forConnector());
				}

				sendResponse(Error.loginError("login_rejected", "User rejected the login request"));
			}));
		})
	}

	[InternalMessageTypes.LOGOUT](sendResponse, data){
		this.lockGuard(sendResponse, async () => {
			const {domain} = data;
			storedData.keychain.removeLoginPermission(domain);
			await StorageService.save(storedData, seed);
			sendResponse(true);
		})
	}

	[InternalMessageTypes.REQUEST_ADD_ACCOUNT](sendResponse, data){
		this.lockGuard(sendResponse, async () => {
			const {domain, network, privateKey, id:name} = data;
			if(!['Testnet', 'Mainnet', 'Previewnet'].includes(network))
				return sendResponse(Error.loginError('invalid_network', `Only "Mainnet", "Testnet", and "Previewnet" networks are supported.`));

			let key = null;
			try {
				key = PrivateKey.fromString(privateKey);
			} catch(e){
				return sendResponse(Error.loginError('bad_private_key', 'The private key you passed in is invalid'));
			}

			const publicKey = key.publicKey.toString();


			if(name.split('.').length !== 3)
				return sendResponse(Error.loginError('invalid_id', 'The Account ID you specified is invalid'));

			const client = Client[`for${network}`]();
			const balance = await new AccountBalanceQuery()
				.setAccountId(name)
				.execute(client).then(() => true).catch(() => false);

			if(!balance) return sendResponse(Error.loginError('invalid_account', 'This account does not exist on the specified network'));

			if(storedData.keychain.accounts.find(x => x.name === name))
				return sendResponse(Error.loginError('id_exists', 'The user already has this Account ID in their wallet.'));

			PromptService.open(new Prompt(storedData.safe(), PromptTypes.REQUEST_ADD_ACCOUNT, domain, network, {domain, network, name, publicKey}, async approved => {
				if(approved){
					const account = storedData.keychain.addAccount(name, key, network, seed, domain);
					const saved = await StorageService.save(storedData, seed);

					if(!saved) return sendResponse(Error.loginError('error_saving', `There was an error saving this account to the user's wallet`));

					return sendResponse(account.forConnector());
				}

				sendResponse(Error.loginError("account_rejected", "User rejected the request to add this account to their wallet"));
			}));


		})
	}

	[InternalMessageTypes.REQUEST_SIGNATURE](sendResponse, data){
		this.lockGuard(sendResponse, async () => {
			try {
				let {data:buffer, domain} = data;
				if(!Array.isArray(buffer) && typeof buffer === 'object') buffer = Object.values(buffer);
				buffer = Buffer.from(buffer);

				// Must be logged in first.
				const loginPermission = storedData.keychain.getLoginPermission(domain, true);
				if(!loginPermission) return sendResponse(null);

				const network = loginPermission.network;
				const details = TransactionBody.decode(buffer);
				//console.log('Details', details)

				// if(details.data === 'cryptoTransfer' && details.cryptoTransfer.transfers.accountAmounts.every(x => x.amount.high === 0 && x.amount.low === 0)){
				// 	// This is a free query, can just sign it off.
				// 	return sendResponse(storedData.keychain.signWithAccount(buffer, loginPermission, seed));
				// }

				PromptService.open(new Prompt(storedData.safe(), PromptTypes.REQUEST_SIGNATURE, domain, network, {domain, network, details, account:loginPermission}, async approved => {
					if(approved){
						return sendResponse(storedData.keychain.signWithAccount(buffer, loginPermission, seed));
					}

					sendResponse(Error.signatureError("signature_rejected", "User rejected the signature request"));
				}));



			} catch(e){
				//console.error("There was an error while trying to sign a transaction", e);
				sendResponse(null);
			}
		})
	}

	[InternalMessageTypes.ABI_CACHE](sendResponse, data){
		this.lockGuard(sendResponse, () => {
			// TODO: Implement
			sendResponse(true);
		})
	}

	[InternalMessageTypes.SET_PROMPT](sendResponse, data){
		prompt = data;
		sendResponse(true);
	}

	[InternalMessageTypes.GET_PROMPT](sendResponse){
		sendResponse(prompt);
	}

	lockGuard(sendResponse, cb){
		if(!seed || !seed.length) {
			// PromptService.open(Prompt.scatterIsLocked());
			sendResponse(Error.locked());
		}
		else cb();
	}
}

const background = new Background();
