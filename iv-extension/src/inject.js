import IdGenerator from './util/IdGenerator';
import {EncryptedStream} from 'extension-streams';
import * as PairingTags from './messages/PairingTags'
import * as NetworkMessageTypes from './messages/NetworkMessageTypes'
import IVConnector from "./connector";

/***
 * This is the javascript which gets injected into
 * the application and facilitates communication between
 * IV and the web application.
 */
class Inject {

	constructor(){
		// Injecting an encrypted stream into the
		// web application.
		const stream = new EncryptedStream(PairingTags.INJECTED, IdGenerator.text(64));

		// Waiting for IV to push itself onto the application
		stream.listenWith(msg => {
		    if(msg && msg.hasOwnProperty('type') && msg.type === NetworkMessageTypes.PUSH_IV)
		        window.wallet = new IVConnector(stream, msg.payload);
		});

		// Syncing the streams between the
		// extension and the web application
		stream.sync(PairingTags.IV, stream.key);

	}

}

new Inject();



