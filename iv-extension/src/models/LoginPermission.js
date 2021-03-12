export default class LoginPermission {

	constructor(){
		this.domain = '';
		this.network = '';
		this.account_id = '';
		this.timestamp = +new Date();
	}

	static placeholder(){ return new LoginPermission(); }
	static fromJson(json){ return Object.assign(this.placeholder(), json); }
}
