
const HOSTS = {
	'Mainnet':'https://mainnet.mirrornode.hedera.com',
	'Testnet':'https://testnet.mirrornode.hedera.com',
	'Previewnet':'https://previewnet.mirrornode.hedera.com',
}

const GET = (network, route) => fetch(`${HOSTS[network]}/${route}`).then(x => x.json());

export default class ApiService {

	static getAccountsFromPublicKey(network, publicKey){
		const formatted = Buffer.from(publicKey.toBytes()).toString('hex');
		return GET(network, `api/v1/accounts?account.publickey=${formatted}`).then(x => x.accounts.map(acc => acc.account)).catch(err => {
			//console.error("Error getting account from key", err);
			return [];
		});
	}

}
