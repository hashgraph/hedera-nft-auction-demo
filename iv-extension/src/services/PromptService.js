import Error from '../models/errors/Error'
import {apis} from '../util/BrowserApis';
import InternalMessage from '../messages/InternalMessage'
import * as InternalMessageTypes from '../messages/InternalMessageTypes'

let openWindow = null;

export default class PromptService {

	/***
	 * Opens a prompt window outside of the extension
	 * @param prompt
	 */
	static async open(prompt){
		if(openWindow){
			// For now we're just going to close the window to get rid of the error
			// that is caused by already open windows swallowing all further requests
			openWindow.close();
			openWindow = null;

			// Alternatively we could focus the old window, but this would cause
			// urgent 1-time messages to be lost, such as after dying in a game and
			// uploading a high-score. That message will be lost.
			// openWindow.focus();
			// return false;

			// A third option would be to add a queue, but this could cause
			// virus-like behavior as apps overflow the queue causing the user
			// to have to quit the browser to regain control.
		}

		// TODO: IMPORTANT!
		// There's some bug here which causes an 'accept' when a window closes
		// under certain edge-cases. Not yet able to reproduce consistently.
		// --
		// Found it. When the `inspector` is open and you close the popup window
		// it accepts the transaction. Not yet sure why.


		const height = 600;
		const width = prompt.data.hasOwnProperty('details') && prompt.data.details.data !== 'cryptoTransfer' ? 600 : 350;
		let middleX = window.screen.availWidth/2 - (width/2);
		let middleY = window.screen.availHeight/2 - (height/2);

		const getPopup = async () => {
			try {
				const url = apis.runtime.getURL('/popup.html');

				// Prompts get bound differently depending on browser
				// as Firefox does not support opening windows from background.
				if(typeof browser !== 'undefined') {
					const created = await apis.windows.create({
						url,
						height,
						width,
						type:'popup'
					});

					window.injectedPrompt = prompt;
					return created;
				}
				else {
					const win = window.open(url, '', `width=${width},height=${height},resizable=0,top=${middleY},left=${middleX},titlebar=0`);
					win.data = prompt;
					openWindow = win;
					return win;
				}
			} catch (e) {
				//console.error('prompt error', e);
				return null;
			}
		}

		await InternalMessage.payload(InternalMessageTypes.SET_PROMPT, JSON.stringify(prompt)).send();

		let popup = await getPopup();

		// Handles the user closing the popup without taking any action
		popup.onbeforeunload = () => {
			prompt.responder(Error.promptClosedWithoutAction());

			// https://developer.mozilla.org/en-US/docs/Web/API/WindowEventHandlers/onbeforeunload
			// Must return undefined to bypass form protection
			openWindow = null;
			return undefined;
		};

	}

	/***
	 * Always use this method for closing prompt popups.
	 * Otherwise you will double send responses and one will always be null.
	 */
	static async close(){
		if(typeof browser !== 'undefined') {
			const {id: windowId,} = (await apis.windows.getCurrent());
			apis.windows.remove(windowId);
		} else {
			window.onbeforeunload = () => {};
			window.close();
		}
	}

}
