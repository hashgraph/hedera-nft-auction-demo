import IdGenerator from "../util/IdGenerator";
import AES from 'aes-oop';
import LoginPermission from "./LoginPermission";
const {
	PrivateKey,
	Mnemonic
} = require("@hashgraph/sdk");

export class Meta {

	constructor(){
		this.version = null;
	}

	static placeholder() {
		let p = new Meta();
		p.version = require('../../package').version;
		return p;
	}

	static fromJson(json) {
		return Object.assign(this.placeholder(), json);
	}
}

export class Account {

	constructor() {
		this.id = IdGenerator.text(24);
		this.name = null;
		this.publicKey = null;
		this.network = null;
		this.fromDomain = null;
	}

	static placeholder() {
		return new Account();
	}

	static fromJson(json) {
		return Object.assign(this.placeholder(), json);
	}

	forConnector(){
		const clone = JSON.parse(JSON.stringify(this));
		delete clone.id;
		clone.id = clone.name;
		delete clone.name;
		delete clone.fromDomain;
		return clone;
	}

}

export class Keychain {

	constructor(){
		this.accounts = [];
		this.privateKeys = {};
		this.encryptedMnemonic = null;
		this.loginPermissions = [];
	}

	static placeholder() {
		return new Keychain();
	}

	static fromJson(json) {
		const p = Object.assign(this.placeholder(), json);
		p.accounts = json.hasOwnProperty('accounts') ? json.accounts.map(x => Account.fromJson(x)) : [];
		p.privateKeys = json.hasOwnProperty('privateKeys') ? json.privateKeys : {};
		p.loginPermissions = json.hasOwnProperty('loginPermissions') ? json.loginPermissions.map(x => LoginPermission.fromJson(x)) : [];
		return p;
	}

	getPrivateKeyFor(account, seed){
		const encrypted = this.privateKeys[account.id];
		if(!encrypted){
			//console.error("There was no private key for: ", account);
			return null;
		}

		return PrivateKey.fromString(AES.decrypt(encrypted, seed));
	}

	signWithAccount(data, account, seed){
		const key = this.getPrivateKeyFor(account, seed);
		if(!key) {
			//console.error("Could not get private key for account", account)
			return null;
		}

		return Buffer.from(key.sign(data)).toString('hex');
	}

	addAccount(name, privateKey, network, seed, fromDomain = null){
		if(this.accounts.some(x => x.name === name && x.network === network)) return;

		const key = privateKey instanceof PrivateKey ? privateKey : PrivateKey.fromString(privateKey);
		const account = Account.fromJson({
			name,
			publicKey:key.publicKey.toString(),
			network,
			fromDomain
		});
		this.accounts.push(account);
		this.privateKeys[account.id] = AES.encrypt(key.toString(), seed);
		return account;
	}

	removeAccount(account){
		this.accounts = this.accounts.filter(x => x.id !== account.id);
		delete this.privateKeys[account.id];
	}

	getLoginPermission(domain, asAccount = false){
		const permission = this.loginPermissions.find(x => x.domain === domain);
		if(!permission) return null;
		if(asAccount) return this.accounts.find(x => x.id === permission.account_id);
		return permission;
	}

	addLoginPermission(permission){
		this.removeLoginPermission(permission.domain);
		this.loginPermissions.push(permission);
	}

	removeLoginPermission(domain){
		this.loginPermissions = this.loginPermissions.filter(x => x.domain !== domain);
	}

}

export default class StoredData {

	constructor(){
		this.meta = null;
		this.keychain = null;
	}

	static placeholder() {
		let p = new StoredData();
		p.meta = Meta.placeholder();
		p.keychain = Keychain.placeholder();
		return p;
	}

	static fromJson(json) {
		const p = Object.assign(this.placeholder(), json);
		p.meta = json.hasOwnProperty('meta') ? Meta.fromJson(json.meta) : Meta.placeholder();
		p.keychain = json.hasOwnProperty('keychain') ? Keychain.fromJson(json.keychain) : Keychain.placeholder();
		return p;
	}

	safe(){
		const clone = JSON.parse(JSON.stringify(this));
		delete clone.keychain.privateKeys;
		delete clone.keychain.encryptedMnemonic;
		return clone;
	}


}
