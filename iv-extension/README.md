# Hedera Hashgraph Extension Wallet

This wallet was created for the Hedera Hashgraph hackathon. It supports all hashgraph actions such as transfer, token create, etc.

## Acknowledgments

This extension was copied from [iv-extension on github](https://github.com/nsjames/iv-extension) which was developed during the 2021 Hackathon by [Nathan James](https://github.com/nsjames).
Only minor modifications were made to the original source code for this demo.

* Changed `Token Transfer` in `PromptSignature.vue` to `Transfer`.
* Updated the `src/manifest.json` file.

## Installation

- Grab the latest release from this repository

- Install the dependencies

```shell
npm install
```

- Build the extension

```shell
npm run build
```

- Zip the contents of the `dist` folder
- Open up chrome and navigate to `chrome://extensions/`
- Drag the zip file onto that page
- Open the wallet from the top right extension icon, and import your accounts.
