import NetworkMessage from './messages/NetworkMessage';
import * as NetworkMessageTypes from './messages/NetworkMessageTypes'
import * as PairingTags from './messages/PairingTags'
import Error from './models/errors/Error'
import IdGenerator from './util/IdGenerator';
import {strippedHost} from './util/GenericTools'


const throws = (msg) => {
	throw new Error(msg);
};

/***
 * This is just a helper to manage resolving fake-async
 * requests using browser messaging.
 */
class DanglingResolver {
	constructor(_id, _resolve, _reject){
		this.id = _id;
		this.resolve = _resolve;
		this.reject = _reject;
	}
}

// Removing properties from exposed IV connector object
// Pseudo privacy
let provider = new WeakMap();
let stream = new WeakMap();
let resolvers = new WeakMap();
let network = new WeakMap();
let publicKey = new WeakMap();


const locationHost = () => strippedHost();

/***
 * Messages do not come back on the same thread.
 * To accomplish a future promise structure this method
 * catches all incoming messages and dispenses
 * them to the open promises. */
const _subscribe = () => {
	stream.listenWith(msg => {
		if(!msg || !msg.hasOwnProperty('type')) return false;
		for(let i=0; i < resolvers.length; i++) {
			if (resolvers[i].id === msg.resolver) {
				if(msg.type === 'error') resolvers[i].reject(msg.payload);
				else resolvers[i].resolve(msg.payload);
				resolvers = resolvers.slice(i, 1);
			}
		}
	});
};

/***
 * Turns message sending between the application
 * and the content script into async promises
 * @param _type
 * @param _payload
 */
const _send = (_type, _payload) => {
	return new Promise((resolve, reject) => {
		let id = IdGenerator.numeric(24);
		let message = new NetworkMessage(_type, _payload, id);
		resolvers.push(new DanglingResolver(id, resolve, reject));
		stream.send(message, PairingTags.IV);
	});
};

const formatNetwork = network => {
	if(network && network.length){
		const firstChar = network[0];
		network = network.substr(1, network.length);
		network = firstChar.toUpperCase() + network;
	}

	return network;
}

/***
 * IV connector is the object injected into the web application that
 * allows it to interact with IV. Without using this the web application
 * has no access to the extension.
 */
export default class IVConnector {

	constructor(_stream, _options){
		this.useAccount(_options.account);
		stream = _stream;
		resolvers = [];

		_subscribe();
	}

	useAccount(account){
		this.account = account;
		publicKey = account ? account.publicKey : '';
	}

	login(network = 'Mainnet'){
		network = formatNetwork(network);
		return _send(NetworkMessageTypes.LOGIN, { network }).then(async account => {
			this.useAccount(account);
			return account;
		});
	}

	logout(){
		return _send(NetworkMessageTypes.LOGOUT, {}).then(() => {
			this.account = null;
			publicKey = null;
			return true;
		});
	}

	addAccount(network, id, privateKey){
		network = formatNetwork(network);
		return _send(NetworkMessageTypes.REQUEST_ADD_ACCOUNT, {network, id, privateKey}).then(account => {
			if(account) this.useAccount(account);
			return account;
		});
	}

	/***
	 * This is a builder for a transaction signer, which can take in an option "abis" array
	 * for custom smart contract parsing.
	 * @param abis
	 * @returns {Promise<function(*)>}
	 */
	getTransactionSigner(abis = []){
		if(abis.length) _send(NetworkMessageTypes.ABI_CACHE, { abis });
		return async (data) => {
			const signature = await _send(NetworkMessageTypes.REQUEST_SIGNATURE, { data });
			return Buffer.from(signature, 'hex');
		}
	}

}
