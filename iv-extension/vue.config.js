module.exports = {
	pages: {
		popup: {
			template: 'public/browser-extension.html',
			entry: './src/popup/main.js',
			title: 'IV - Hedera Hashgraph Extension'
		}
	},
	pluginOptions: {
		browserExtension: {
			componentOptions: {
				background: {
					entry: 'src/background.js'
				},
				contentScripts: {
					entries: {
						'content-script': [
							'src/content-scripts/content-script.js'
						]
					}
				}
			}
		}
	},
	filenameHashing: false,
	chainWebpack: config => {
		config.plugins.delete('provide-webextension-polyfill');
		config.module.rules.delete('provide-webextension-polyfill');

		config.entry('inject')
			.add('./src/inject.js')
			.end()
	},
	configureWebpack: {
		optimization: {
			splitChunks: false
		}
	},
}
