import {apis} from '../util/BrowserApis';
import StoredData, {Account} from "../models/StoredData";
import AES from "aes-oop";
import IdGenerator from "../util/IdGenerator";

export default class StorageService {

	constructor(){}

	static save(data, seed){
		if(!seed || !seed.length) return false;
		return new Promise(resolve => {
			try {
				const encrypted = AES.encrypt(data, seed);
				apis.storage.local.set({data:encrypted}, () => {
					resolve(true);
				});
			} catch(e){
				//console.error("Error saving storage", e);
				resolve(false);
			}
		})
	};

	static get(seed) {
		return new Promise(resolve => {
			apis.storage.local.get('data', (possible) => {
				try {
					if(possible && possible.hasOwnProperty('data')){
						const decrypted = AES.decrypt(possible.data, seed);
						const storedData = StoredData.fromJson(decrypted);
						return resolve(storedData);
					}

					resolve(null);
				} catch(e){
					//console.error("Error decrypting stored data", e);
					resolve(null);
				}
			});
		})
	}

	static hasData() {
		return new Promise(resolve => {
			apis.storage.local.get('data', (possible) => {
				resolve(possible && possible.hasOwnProperty('data'));
			});
		})
	}

	static getSalt(){
		return new Promise(resolve => {
			apis.storage.local.get('salt', async (possible) => {
				if(possible && possible.hasOwnProperty('salt')) return resolve(possible.salt);

				const salt = IdGenerator.text(1024);
				apis.storage.local.set({salt}, () => {
					resolve(salt);
				});
			});
		})
	}

	static setTokenMeta(id, meta){
		return new Promise(async resolve => {
			try {
				const persisted = await StorageService.getTokenMeta();
				persisted[id] = meta;
				apis.storage.local.set({tokenMeta:persisted}, () => {
					resolve(true);
				});
			} catch(e){
				//console.error("Error saving token meta", e);
				resolve(false);
			}
		})
	};

	static getTokenMeta() {
		return new Promise(resolve => {
			apis.storage.local.get('tokenMeta', (possible) => {
				try {
					if(possible && possible.hasOwnProperty('tokenMeta')){
						return resolve(possible.tokenMeta);
					}

					resolve({});
				} catch(e){
					//console.error("Error getting token meta", e);
					resolve(null);
				}
			});
		})
	}

	static setActiveAccount(activeAccount){
		return new Promise(async resolve => {
			try {
				apis.storage.local.set({activeAccount}, () => {
					resolve(true);
				});
			} catch(e){
				//console.error("Error saving active account", e);
				resolve(false);
			}
		})
	};

	static getActiveAccount() {
		return new Promise(resolve => {
			apis.storage.local.get('activeAccount', (possible) => {
				try {
					if(possible && possible.hasOwnProperty('activeAccount')){
						return resolve(Account.fromJson(possible.activeAccount));
					}

					resolve(null);
				} catch(e){
					//console.error("Error getting active account", e);
					resolve(null);
				}
			});
		})
	}


	static async remove(){
		await new Promise(r => apis.storage.local.remove('data', () => r()));
		await new Promise(r => apis.storage.local.remove('tokenMeta', () => r()));
		await new Promise(r => apis.storage.local.remove('salt', () => r()));
		await new Promise(r => apis.storage.local.remove('activeAccount', () => r()));
		return true;
	}
}
