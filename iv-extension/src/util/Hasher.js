const scrypt = require('scrypt-async');
import StorageService from '../services/StorageService';
const crypto = require('crypto');

export default class Hasher {

	static md5hex(data){
		if(!data) return '000000000000000';
		return crypto.createHash('md5').update(data.toString()).digest("hex").toString();
	}

	static async secureHash(cleartext) {
		return new Promise(async resolve => {
			const salt = await StorageService.getSalt();
			scrypt(cleartext, salt, {
				N: 16384,
				r: 8,
				p: 1,
				dkLen: 16,
				encoding: 'hex'
			}, (derivedKey) => {
				resolve(derivedKey);
			})
		});
	}
}
