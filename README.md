[![Java CI with Gradle](https://github.com/hashgraph/hedera-nft-auction-demo/actions/workflows/unit-integration-test.yml/badge.svg)](https://github.com/hashgraph/hedera-nft-auction-demo/actions/workflows/unit-integration-test.yml)
[![codecov](https://img.shields.io/codecov/c/github/hashgraph/hedera-nft-auction-demo/master)](https://codecov.io/gh/hashgraph/hedera-nft-auction-demo)
[![GitHub](https://img.shields.io/github/license/hashgraph/hedera-nft-auction-demo)](LICENSE)
[![Discord](https://img.shields.io/badge/discord-join%20chat-blue.svg)](https://hedera.com/discord)

# Hedera Non Fungible Token Auction Demo

## Dependencies

* A testnet or mainnet account
* git command line installed on your computer

* To build from code
  * PostgreSQL version 12
  * Node.js v14.15.0
  * NPM 6.14.8
  * Java 14

* To run from docker images  
  * Docker and docker-compose (optional)

## Notes

The java projects use Lombok, ensure that the plug is installed in your IDE and configured properly [Lombok Plugin](https://www.baeldung.com/lombok-ide)

_Note that enabling annotation processing differs between versions of IntelliJ `Preferences > Compiler > Annotation Processors` before IntelliJ2017, starting with IntelliJ 2017, the "Enable Annotation Processing" checkbox has moved to: `Settings > Build, Execution, Deployment > Compiler > Annotation Processors`_

## Description

This project consists of two main modules, a Java back-end and a front-end.

The front end displays auctions and enables users to place bids (optionally requires a browser plug in to sign transactions) and monitor the auction's progress.

The back end fulfils 3 separate roles

* UI REST API server for the UI above
* Admin REST API server for admin features (such as creating a new auction)
* Auction and Bid processing, registering new auctions, checking bid validity, issuing refunds

The latter can be run in readonly mode, meaning the processing will validate bids but will not be able to participate in refunds or token transfers on auction completion.

_Note: The three roles can be run within a single instance of the Java application or run in individual Java instances, for example, one instance could process bids, while one or more others could serve the UI REST API for scalability purposes. This is determined by environment variable parameters.
The Docker deployment runs two instances, one for bid processing, the other for the UI and Admin REST APIs for example._

The admin API runs on a separate port to the UI REST API to ensure it can be firewalled separately and protected from malicious execution.

Additional detailed documentation on how the system is architected may be found in the `docs` folder of this project.

## Setup, compilation, execution for developer testing

Pull the repository from github

```shell
git clone https://github.com/hashgraph/hedera-nft-auction-demo.git
```

### With docker

This section assumes you have `docker` and `docker-compose` installed, if not, please consult the following links to perform the installation.

* `docker` [https://docs.docker.com/engine/install/](https://docs.docker.com/engine/install/)
* `docker-compose` [https://docs.docker.com/compose/install/](https://docs.docker.com/compose/install/)

#### Setup environment

```shell
cd hedera-nft-auction-demo
cd docker-files
cp .env.sample .env
nano .env
```

setup the `.env` properties as follows

* `OPERATOR_ID=` (input your account id for the Hedera network)
* `OPERATOR_KEY=` (input your private key associated with the Hedera account above - 302xxxx)
* `NEXT_PUBLIC_NETWORK=` (mainnet, testnet or previewnet)
* `MASTER_KEY=` (set only for one node which has additional authority over the auction accounts, can be the same as operator key only, else must be different)
* `NFT_STORAGE_API_KEY=` (We use IPFS storage using [nft.storage](https://nft.storage) to store NFT metadata. You can create your API key on https://nft.storage and add it to your .env file to enable IPFS upload, this is only required if your node will be involved in token creation through the API or command line)

you may leave the other properties as is for now

```shell
cd hedera-nft-auction-demo
cd hedera-nft-auction-demo-javascript-client
cp .env.sample .env
```

you may leave the properties as they are for now

#### Start docker images

Using pre-built images

```shell
cd ..
docker-compose --profile image up
```

Building your own images from source code
```shell
cd ..
docker-compose --profile compile build
docker-compose --profile compile up
```

_Note: you may need to `sudo` these commands depending on your environment`

you may now navigate to [http://localhost:8080](http://localhost:8080) to verify the UI is up and running, it should indicate no auctions are currently setup.

#### Create a sample auction

```shell script
curl -H "Content-Type: application/json" -X POST -d '{
  "symbol":"MTT",
  "name":"Test Token",
  "clean":false
}' http://localhost:8082/v1/admin/easysetup
```

#### Restart the docker containers for the topic to be taken into account

Stop the containers with `CTRL+C`

Restart the containers

```shell script
docker-compose up
```

_Note: you may need to `sudo` this command depending on your environment`

You should see logs similar to

`
nft-auction-demo-node | 2021-04-23 12:26:27.063 INFO  com.hedera.demo.auction.node.app.subscriber.TopicSubscriber - Auction for token 0.0.539174 added (150)
nft-auction-demo-node | 2021-04-23 12:26:27.063 INFO  com.hedera.demo.auction.node.app.subscriber.TopicSubscriber - Auction for token 0.0.539174 added (150)
nft-auction-demo-node | 2021-04-23 12:26:29.022 INFO  com.hedera.demo.auction.node.app.readinesswatcher.HederaAuctionReadinessWatcher - Watching auction account Id 0.0.539175, token Id 0.0.539174 (36)
nft-auction-demo-node | 2021-04-23 12:26:29.024 DEBUG com.hedera.demo.auction.node.app.readinesswatcher.HederaAuctionReadinessWatcher - Checking ownership of token 0.0.539174 for account 0.0.539175 (52)
nft-auction-demo-node | 2021-04-23 12:26:29.364 INFO  com.hedera.demo.auction.node.app.readinesswatcher.AbstractAuctionReadinessWatcher - Account 0.0.539175 owns token 0.0.539174, starting auction (70)
nft-auction-demo-node | 2021-04-23 12:26:39.380 DEBUG com.hedera.demo.auction.node.app.bidwatcher.HederaBidsWatcher - Checking for bids on account 0.0.539175 and token 0.0.539174 (38)
...
`

you may now navigate to [http://localhost:8080](http://localhost:8080) to verify the UI is up and running, it should show the auction created above (it may take a few seconds to appear).

#### Notes

* EasySetup which is invoked to create an auction deletes all the data from the database, creates a new topic and a new auction. Make sure you restart the containers after running this easySetup.

* Database files are persisted on your host under `docker-files\postgres-data`, to completely delete the database, delete this folder and restart the containers.

* The `docker-files` folder is mounted as a volume on the containers.

### Standalone

#### Database

All database objects will be created in the `public` database.

_Note the installation below assumes the user is `postgres` and the password is `password`._

#### Java Appnet Node

```shell
cd hedera-nft-auction-demo
cd hedera-nft-auction-demo-java-node

# Build the code
./gradlew assemble
```

setup the environment

```shell
cp .env.sample .env
nano .env
```

set the following properties according to your Hedera account details

_Note:
The operator id/key is used to query the hedera network for the token's metadata if present (FileId in memo) and issue or sign scheduled transactions._
* _It is also used by the auction administrator to set the submit key for the auction topic and also for creating the auction account and submitting auction creation messages to the topic._
* _And optionally creating a token to auction, then transferring it to the auction account_

* `OPERATOR_ID=` (input your account id for the Hedera network)
* `OPERATOR_KEY=` (input your private key associated with the Hedera account above - 302xxxx)
* `TRANSFER_ON_WIN=`true

You may edit additional parameters such as `MIRROR_PROVIDER`, etc... if you wish (although only the hedera mirror API is supported at this time).

start the application

```shell
./gradlew runNode
```

#### Javascript UI

_Note: The UI assumes it is served from the same server as the client REST api (The Java Appnet Node above)_

Edit environment variables

```shell
cp .env.sample .env
nano .env
```

* `NEXT_PUBLIC_BASE_API_URL=` this is the url of the client API (e.g. http://localhost:8081). Note that if not specified, the UI will assume the REST API is served from the same server as the UI on port 8081.
* `NEXT_PUBLIC_NETWORK=` `Testnet` or `Mainnet`. If not specified, the UI will default to `Testnet`.

```shell
cd hedera-nft-auction-demo
cd hedera-nft-auction-demo-javascript-client
npm install
npm run-script build
```

#### Setting up an auction

A number of helper functions are available from the project in order to get you started quickly.

#### Super simple

This command takes a number of parameters runs all the necessary steps to create a demo auction:

* create a HCS Topic
* create a simple token
* create an auction account
* setup the auction
* transfers the token to the auction

This requires that the REST api and database are up and running

```shell script
curl -H "Content-Type: application/json" -X POST -d '{
  "symbol":"MTT",
  "name":"Test Token",
  "clean":false
}' http://localhost:8082/v1/admin/easysetup
```

#### Step by step 

The examples below show curl commands, however the `hedera-nft-auction-demo-java-node` project includes `postman` files for the admin and client APIS which you can import into Postman instead.

__Create a topic__

This will create a new topic id and set the `TOPIC_ID` in the `.env` file.

It is now necessary to restart the application for the changes to take effect.

```shell script
curl -H "Content-Type: application/json" -X POST -d '
  {
  }
' http://localhost:8082/v1/admin/topic
```

returns a topic id

```json
{
    "topicId": "0.0.57044"
}
```

__Create a simple token__

This command will create a token named `Token Name`, an initial supply of `1`, `0` decimals and add a memo to the token.

If `image`, `certificate` or `description` are included, files will be created on IPFS and will be referenced in a `metadata` file also on IPFS. The `metadata` file URI will be used for the token's symbol.
If neither of these attributes are set, the `symbol` attribute is used.

```shell script
curl -H "Content-Type: application/json" -X POST -d '
{
  "name": "Token Name",
  "symbol": "Symbol",
  "initialSupply": 1,
  "decimals": 0,
  "memo": "token memo",
  "description": {
    "type": "string",
    "description": "Describes the asset to which this token represents."
  },
  "image": {
    "type": "base64",
    "description": "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"
  },
  "certificate": {
    "type": "base64",
    "description": "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"
  }
}
' http://localhost:8082/v1/admin/token
```

or

```shell script
curl -H "Content-Type: application/json" -X POST -d '
{
  "name": "Token Name",
  "symbol": "Symbol",
  "initialSupply": 1,
  "decimals": 0,
  "memo": "token memo",
  "description": {
    "type": "string",
    "description": "Describes the asset to which this token represents."
  },
  "image": {
    "type": "file",
    "description": "GoldCoin.jpg"
  },
  "certificate": {
    "type": "file",
    "description": "silver.jpg"
  }
}
' http://localhost:8082/v1/admin/token
```

returns a token id

```json
{
    "tokenId": "0.0.58792"
}
```

__Create an auction account__

This command will create an auction account with an initial balance of `100` hbar and use the operator key for the account.

```shell script
curl -H "Content-Type: application/json" -X POST -d '
  {
    "keylist: {
      "keys": [
        {
          "key": "your public key"
        }
      ],
      "threshold": 1
    },
    "initialBalance": 100
  }
' http://localhost:8082/v1/admin/auctionaccount
```

returns an account id

```json
{
    "accountId": "0.0.58793"
}
```

__Create an auction account with a key list__

This command will create an auction account with an initial balance of `100` hbar and a key list for scheduled transactions.

_Note: if the environment file contains an entry for `MASTER_KEY`, it will automatically be added to the keylist below with a threshold of 1, resulting in an auction account having a threshold key of 1 of 2, one of the keys being the master key, the other being the threshold key supplied in the JSON._

_Note: all keys are *public* keys_

```shell script
curl -H "Content-Type: application/json" -X POST -d '
{
  "keylist": {
    "keys": [
      {
        "key": "302a300506032b657003210090ec5045925d37b358ee0c60f858dc79c3b4370cbf7e0c5dad882f1171265cb3"
      },
      {
        "key": "302a300506032b657003210076045799d169c6b6fc2bf45f779171a1cb10fd239b4f758bc556cb0de6799105"
      }
    ],
    "threshold": 2
  },
  "initialBalance": 100
}' http://localhost:8082/v1/admin/auctionaccount
```

returns an account id

```json
{
    "accountId": "0.0.58793"
}
```


__Create the auction__

be sure the replace `{{tokenId}}`, `{{accountId}}` in the json below with the values you obtained earlier.

```shell script
curl -H "Content-Type: application/json" -X POST -d '
{
  "tokenid": "{{tokenId}}",
  "auctionaccountid": "{{accountId}}",
  "reserve": 0,
  "minimumbid": 1000000,
  "endtimestamp": "",
  "winnercanbid": true,
  "title": "Auction title",
  "description": "Auction description"
}' http://localhost:8082/v1/admin/auction
```

_Note: the minimum bid and reserve are expressed in `tinybars`_

__Transfer the token to the auction account__

This transfer the token from the account that created it to the `auctionaccountid`, supply the `tokenId` and `accountId` created above in the parameters.

be sure the replace `{{tokenId}}`, `{{accountId}}` in the json below with the values you obtained earlier.

_Note: If the token has been created with the REST api call above, it will already by associated and owned by the `auctionaccountid`, there is no need to transfer it._

```shell script
curl -H "Content-Type: application/json" -X POST -d '
{
  "tokenid" : "{{tokenId}}",
  "auctionaccountid" : "{{accountId}}"
}' http://localhost:8082/v1/admin/transfer
```

#### Run the components

_Note: Each of the steps below need to be run from a different command line window_

```shell
cd hedera-nft-auction-demo
```

__Appnet node and REST API__

```shell
cd hedera-nft-auction-demo-java-node
java -jar build/libs/hedera-nft-auction-demo-1.0.jar
```

__Web UI__

```shell
cd hedera-nft-auction-demo-javascript-client
npm start
```

## Developing new features requiring database changes

This is only required in order to create/modify database objects, it happens automatically when the application is launched too.

__Setup the database objects__

```shell script
./gradlew flywayMigrate
````

__Build the database classes__

```shell script
./gradlew jooqGenerate
````

## Testing

The project contains a number of tests suites which are described below

### Unit testing

Unit testing is run automatically when you `./gradlew build`, you can run these independently with `./gradlew test`.

There are no interactions with the database or the Hedera network in these tests.

### Integration testing

Integration testing is run automatically when you `./gradlew build`, you can run these independently with `./gradlew testIntegration`.

These tests include testing the outcome of various operations in the database.

### System testing

System testing is run with `./gradlew testSystem`.

These tests include testing the outcome of various operations in the database and invoke Hedera APIs.

# Token Specification

## Binaries from base64

If you'd like to attach an image and optionally a certificate document to the token, convert the two binary files to base64 and include in the JSON below.

_Note: The `type` for the `image` and `certificate` is `base64`_

```json
{
  "name": "Token Name",
  "symbol": "Symbol",
  "initialSupply": 1,
  "decimals": 0,
  "memo": "token memo",
  "description": {
    "type": "string",
    "description": "Describes the asset to which this token represents."
  },
  "image": {
    "type": "base64",
    "description": "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"
  },
  "certificate": {
    "type": "base64",
    "description": "/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAAMCAgMCAgMDAwMEAwMEBQgFBQQEBQoHBwYIDAoMDAsKCwsNDhIQDQ4RDgsLEBYQERMUFRUVDA8XGBYUGBIUFRT/2wBDAQMEBAUEBQkFBQkUDQsNFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBT/wgARCABLAEsDAREAAhEBAxEB/8QAHAAAAQUBAQEAAAAAAAAAAAAABgADBAUHCAIB/8QAGgEAAQUBAAAAAAAAAAAAAAAAAgABAwQFBv/aAAwDAQACEAMQAAAB6pSSUGvJnuddr6s7sgFl+oWXqvskkkkL0LOGc3t+q0t+AyUw5K5Fs5u2b2W8bRIS5m5PoqirY0gI2JwgJMi9Cj1LosTW9eiBZGhzdzm2+61KWqPSvdwE2JAoTWV6n091XP55iaXPOHsypGPLFf5E5tChQnBQnl2a/T/T4AhQt85c/uNMVvYh0mAZRCGI6ITuLEPSfVc7XRT8rcp0NeJndmtfAvoodYmE5tfq7f0+F6T4xz+ti2TpsGxHPDfRO7ET5jt/UYV3bhTryyzjG0cpxtNivJbyRlF2tpu/lXVuFJJJJJN4B4cROG0o2+kkkkv/xAAlEAACAgICAgEEAwAAAAAAAAADBAECAAUGEhARExQgISIVJDP/2gAIAQEAAQUC8MuCVhvmoaWryTYFmvKWwZr+SKv5W8Xr53W6HqwtNm2s1ZqPKEJWJLauMVC1mi5MVc4i1NTGjwuDZbC21duftmuRhMW42UL3Sv8AUxtKjqPrQ4uHbm565y0toR7YO37Xuc8bNr9dGsMV+TEoa/vNQaVtyEnyi5bf0KY64AsiKp/mMMtHhMUC3q4QRkDkdlJ/r8nB8mFi0jicVsUp7i+VT+QGK27NW16R7wK/yuKR6W3KktJNhrRuJ6WCZQ40tn0u5M2NsNXREghfnjaP1b/jlvHO0mpNMkHaibR8Da1ckEzZdWzpNXr6oL+JjtG44iNrG9AypNFy0tQd/SmgZbxDViQp9s/nJSXnBriFP2f/xAAoEQABAwMDAwQDAQAAAAAAAAABAAIRAxAxBBIhIEFhEzJRcRQiQoH/2gAIAQMBAT8Bsym5+E3S/JX4zEdM0p1Bwx00aPqfSFMNCF3NByFXoIggwbMbvcAqTQ0cYsT2QxKd4TJThOVXpx/ltI3klRHFphDwuYVLuiqzZ5Tv1MLTYH3ZwkQo+E21IzZ+IVT3laY8HxcthYKPeEyz8QnmXFUH7HiU3Fnk91MiV5CDptqKmxnF9LqP4cpRC22CcQ0SVVqeo6eilqy3iom1WP8AabeU/UMZjlVKzqmeoLe4d1Jdnp//xAAvEQABAwMDAwAIBwAAAAAAAAABAAIRAwQxEBIhEyJBBSBRUmGBsdEUMjNDcaHB/9oACAECAQE/AdKtenREuKdeujgbf5X413v/ANJl473gm3bf3OPogZ5HqXNz0u1v5k+sZlufas8leFMKnUcwyCra48t+Y+yY4PG4aVagpMLz4VZ585Oftq1snlFuw8p0ZQcWmWq1rAwRg/XS/fG1iJ3GUEGJog96e+TEp4iNLc5amncJV/8AqfL/AHSmQHSU50xKe7yt0lVBzOlDh6t56QV+3vafbwisLrSIU8QmiSj8dKUA7lRBbTAV3SNWlAyigqbdvPhPp7cIY1tqe98KNL21LT1GY0Y4tMhGqzynbDyjlMaXmArej0m/HUiVX9H7u6kn0qlMw5q3SU3ngBUrOtVyICoWzKA4z6xzC6bDloTWNae0Lz6n/8QAOBAAAgADBAcECgAHAAAAAAAAAQIAAxEEEiExEBMiQVFhcVKBobEUICMyM0KRssHwJFNygpLh8f/aAAgBAQAGPwLRtnHsjOLlnBnvwkrf8ch9Ywsrj+qYo/Bis6yT7u8qFmeVDGw6txu5r1U4iKqajiPUJLbUO09qSs9TX7uMBZYoIDFHH9v7wjFScsIvM2rmpisxTRl6GFs9qYEuaJOyEzk3Bue+Ay5aGmHdDvf2Frcqc+cVy6QCV/iSKkn5YpKAM4DEt8og35zCe3ukHEjlxES81tQwmKVp3wZRVdrNmO6Gs85r06VRWbtj5W/B0GWhoxBPSPxCm6XANbozMFg5TVrVlui89PKEaXMarHaOPj9ImzdkAHDh1iSy1DAFTUU0WVw10TDqW78vGkI2RIi1crMfuEZq3QwrLnzgrrSrgAkVrSm/Gh/7zga660ljtiuNekej4pL3XDQwEEw7Iqt+veNFlmYfHl/eIXv84p/NlPK76YeNIVyl0aEycYA1BhnC3J8p8Dup+mJZtUuZJXtUqv1ESJcq0+kygtQag05aLFKuXWM1WqDuXE+US+lYa78RdpYmX/ZjOkC8teWUCTZ6Sy42VPv3hu/eEFJwwm7N7d1jU3phTaDpWmVIurMvtU7JzpxjEVENPO0o9mDx7R8hpNpkjDM03QRNrf3RtDDyg6wLaRxfBv8Afh1glRRj8zbu6p84JY3m3k5mNXKBUVozZ9w5wqgUNKUG7lpoYLyKA9gxQqyjdwipTjhAFyvWBrAUTnh4xsircfX+DL/xiqS0Q8QPV//EACUQAQABBAIBBAMBAQAAAAAAAAERACExQVFhcYGhsfAQkdEg4f/aAAgBAQABPyH8SnCkuL7zTDBd9PMj1qRfex8H8lWFywMHkX6BojC8/pQ9YosR8JI/4L8QNSk4g2uimJSQORyt+sfNPkmAARrj2rUCCcpj+Kvs+yW4i1qSmKn3iF5W0eZtQEgIh3FjiFujl2pXOTr8YFTvbqnqVYYBYfN10dtTLCy/LujzPGF1xHu0jjZBZr/fDxSEuiFYWVjZa2aS2QZhbEW2vyVJU4GHT69VOeCOV9+z2A7qaXsnpoGfS76Vc2INcDioGBCxg3CprqThAScK827pLg1KamiFyRE/EU7gZIsncOXdR3EDpLIw+WmkqonODd6F06pBKcOykUZheoPs1Z5mzaj3g+HFBiFphSSQGSBBfKkFpg465gd3OdTRDgF7xmnbUUkV0Pv5oZqY6LYZoY8nyUkNfXEvYoEiW3f/AD+1OQ3O6ZABDQQYW8WOTVASgQZBiExF30qKV5VA5+JqeR4KWlqtx4Nyb0uSy2PN6NnCzHJWcwsBvcfMVcQBlnJKhTNhSp8pLjtjujFJRBybe76WkkLl0eiA7HnNTkwydgYJd0UJtgVcrPWdPg/Ya/CTWXEwDL+UgSC0S0fV+tQDJkJlaZ1imRQ5/aNoX7UaXDDaODcOOlMqlltyjFpRuPk+FF2AaAMD7f8AJoEjZGoKKzh9KPDqQbr75qJC2l3P9qLGJuiCf31T/MBKR2rvpFAAIIsiDgNf5aIESRyNKTe5AV0phjR/j//aAAwDAQACAAMAAAAQkGmikjtaFM/mg4ymtbwZPkay/tF1b/kVA/NVOcd+gQLlkkKWEk//xAAkEQEAAgICAQQCAwAAAAAAAAABABEhMUFRECBxgaGxwWGR0f/aAAgBAwEBPxDwjRuGm72lXH3NcJ8zvX5jhp9CO3T7lQqjqDEQuAMpS6VFv99e8dBk8IDzBhq14vgj0RNYWRpTZRmCE1Zfe+XZ/vgnplHzAGvEcEzAcx2O1cRBliKkGoLhkej9wXvifU/hhL+0MlUANG4rdkcIkYhcEgpVyH7r9wRySr3BzHE01zA1BXbFbm2DUw3aOGUAE9opHhKgOIrZzRM0iy4Fnj3lprwCG01/MEwnDFcQUixHTURupx6Ddx34lRC4iZITbrRm9OvStQcxwBR3l+n/xAAnEQEAAgEDAQgDAQAAAAAAAAABABExIUFRcRBhgZGhsdHwIMHh8f/aAAgBAgEBPxDsIUu7d6E1oRy/0Wxtur0+UQ2N1KfMuGClHfPmIJyH4UQxWDjveAgmlvK36cEVVls4RWgj6teTZ6mJS019L+H6glbHsSvQf4RKnO8WOg3N4wVzCAjiPdz1gWgrV8toWaki2g9EZ8+wrWLtOkdHl1gL1ahN2Uc3iYbV4jVtBE1M9jgfU6n8led5qq7OHSDhsjMDTvM92npKQHrnz/UFYxdBzUqXF6+0Wh+6zoA+LJ6yjiXkS1i7O/8AnpBN0QSFA7DTwH2jOzXvrEGFqdYLbJS6cd0vK1W/zESK7lCrpUwoY27FBYdXofO0o5iXGZ2snH854lBr2CUaivv3mUMq+9xNUBTtYetqsv67QFMJug8beHE8TOUom4liB41p/ZuLy/Gri6E348D4lGA6Ae0xX8P/xAAjEAEBAAICAgEFAQEAAAAAAAABEQAhMUFRYXEQIIGRofDB/9oACAEBAAE/EPopdcAA8y6PZA7TC+Z2yvasPJbwZwRTQuug9nC55zlI4O/Avzs4wHp+B/iF5YC4e2aAD0n2C8UOcUAbVoPl1zvWJdLaNGnNzQBlTYT1WB0N3cE06dGaPIgoLbKX0db84MWYEkACQpQOrjFGmRBywoEGSsLe+kLoBEcCGggGGHVkEQYo6Ronk+iUBQHY0PypnM0oToumCW61TcmxjtRHMdXwatgEB2CTKrR8ArZsJXgFiKxMmD60PC0bjY2fzaDRAgFCVHRCTfwRGAJp0A1gSAx8lnPOGt3d20QPJ82lMJFu791ogABxQgxSoPxDgQ+jgXEoD4AMBxhVBCo4UEHq5TFrWhZXQincHWMJUxaM5gQ1FfkSPMx7hg6qSqbrgAEDMsJFHUya1iY2D3GN4qcvlwMoL+L+DT8YHNB9Lb9l+c8LCoE3wowy1U5GiHapbzdk1sSjfsQXhqgRyaRCcWBmOpCIjWulxQKm/wBIpQHt5uDvrLZzBaFEiInKFGrq2Wfy/wC/OW6VEwznEXqCp3hAGgzCD6uzzMQjDoQTcEDQRKBsq8jhIAhEFpmgcqytTekoppdUx5+ptuBugFCmnoYLPoPyu6yBy1mvGboV3ZpQlYyu21zWRb4N4FKsU2bCLd5AgQ7lChpGzofe8YQdyknR7/6GNj244Jo1dVB4IM2mhVFbFsGMpdxmK7RCEvfa2DU4Gg2CQdcRXhhUobu2CpvahB0gIRdm2kAu2CCpiagOId84fyhEZ/v3McoZKoqZ71TvajUEMARxJz5Cq7PbjoVHTusLcQKujg2j2rezmxkR6AWA2qok4cAEwJEbyVVu7N1XOlY1RALQ4CeDF0uvnMx5WAHCeoAcYtvBQLeQAQnUuvh65iUb2GdrtV8B9D8HgUTwmHpesFLefXw/vFJLBqHRVibYC594PsQoq7mx4dPgxG7MMymq8NDi/nDvnrvQAC40OEsUTjyEx4lo+Zt7WE+zhghjQFE8JjxEna5+UN4lSRq952Fzr8fZ/9k="
  }
}
```

## Binaries from files

If you'd like to attach an image and optionally a certificate document to the token using existing binary files, include the name of the files in the JSON below.
The path where the files should be located is defaulted to `sample-files/` and may be modified in the `.env` file.
_Note: If the path contains a `.env` file, an error will be generated._

_Note: The `type` for the `image` and `certificate` is `file`_

```json
{
  "name": "Token Name",
  "symbol": "Symbol",
  "initialSupply": 1,
  "decimals": 0,
  "memo": "token memo",
  "description": {
    "type": "string",
    "description": "Describes the asset to which this token represents."
  },
  "image": {
    "type": "file",
    "description": "GoldCoin.jpg"
  },
  "certificate": {
    "type": "file",
    "description": "silver.jpg"
  }
}
```

## Use of IPFS

If `image`, `certificate` or `description` are included above, files will be created on IPFS and will be referenced in a `metadata` file also on IPFS. The `metadata` file URI will be used for the token's symbol.

If neither of these attributes are set, the `symbol` attribute is used.

For example, the above token specifications would result in a file on IPFS containing this JSON data:

```json
{
  "name": "Token Name",
  "symbol": "Symbol",
  "initialSupply": 1,
  "decimals": 0,
  "memo": "token memo",
  "description": {
    "type": "string",
    "description": "Describes the asset to which this token represents."
  },
  "image": {
    "type": "string",
    "description": "https://cloudflare-ipfs.com/ipfs/bafkreieqtmwpcj75uff42bmmk3wldjdjmf2fpmi6zjkbdv67qsbzgs22qa"
  },
  "certificate": {
    "type": "string",
    "description": "https://cloudflare-ipfs.com/ipfs/bafkreieqtmwpcj75uff42bmmk3wldjdjmf2fpmi6zjkbdv67qsbzgs22qa"
  }
}
```

Where the `image.description` and `certificate.description` attributes are links to the provided `image` and `description` binaries on IPFS.

The uri to this JSON file on IPFS will be stored in the created token's symbol.

# Full Application network setup

The back-end can be considered a node within the application network, there may be several such nodes in operation to ensure full decentralisation of the auction process. Such nodes fall into three categories:

* `Readonly Node`: This node type is not actively participating in the running of the auctions, but may be run by any third party to verify the correct running of the auctions.

* `Validator Node`: This node type is actively participating in the running of the auctions, it holds a private key which is used to counter-sign transactions on behalf of the entire application network. Indeed, an auction account is created for each auction, and this account is multi-sig. Depending on the threshold set on the auction account's key, a minimum number of signatures from validator nodes is required in order to approve operations such as refunds, token transfers, etc...

* `Master Node`: This node is functionally equivalent to the `Validator Node` above, except that it has the ability to associate the auction accounts with tokens and set the auction account's `signature required` option when an auction ends to prevent further bids from being placed.

_Note: This `Master Node` will eventually become a `Validator Node` in its own right once the whitelisting of the `Token Associate` and `Account Update` transactions has occurred. It is a temporary solution in the mean time._

## Determining the node type

Depending on the type of node you're setting up, you may need different information or may need to submit some information to other node operators.

### Readonly Node

You must acquire the `Topic Id` for the application network from the entity that setup the application network in the first place. This `Topic Id` is used by the application network to share details of new auctions being created.

### Validator Node

You must acquire the `Topic Id` as described above for a `Readonly Node`.

In addition, you'll need to generate an ED25519 private/public key and share the public key with whoever is setting up an auction for you to validate.

### Master Node

This node will be creating the `Topic Id` to share with the `Readonly` and `Validator` Nodes.

One additional ED25519 private/public key will be required, the `MASTER_KEY`. the key should be shared with whoever is setting up an auction account.

### Generating keys

A helper function is available to generate keys as follows

```shell
./gradlew generateKey
```

or

```shell script
curl -H "Content-Type: application/json" -X POST -d '{}' http://localhost:8081/v1/generatekey
```

_Note: this runs on the client REST API port (8081), not the admin API port (8082)

## Environment setup

### Creating accounts

If you need to create accounts for testing purposes, you may use the following helper.

```shell
./gradlew createAccount --args="100"
```

which will create an account with the specified initial balance in hbar and will return if successful (the actual keys have been truncated here but will be output fully).

```shell
2021-07-02 13:27:16.026 INFO  com.hedera.demo.auction.exerciser.CreateAccount - Account created 0.0.2060215 (43)
2021-07-02 13:27:16.029 INFO  com.hedera.demo.auction.exerciser.CreateAccount - Private key is 302e020.......83135bbb84cf17 (44)
2021-07-02 13:27:16.030 INFO  com.hedera.demo.auction.exerciser.CreateAccount - Public key is 302a3005.......f06f311c60accc (45)
```

### All node types

* `OPERATOR_ID=` (input your account id for the Hedera network)
* `OPERATOR_KEY=` (input your private key associated with the Hedera account above - 302xxxx)
* `NEXT_PUBLIC_NETWORK=` (mainnet, testnet or previewnet)
* `NODE_OWNER=` (an identifier, e.g. `ACMEAuctions` to be rendered in the UI to show which node the UI is connected to)
* `TOPIC_ID=` (the topic id provided by whoever is setting up the application network, leave blank if you're setting up a new application network)

### Validator nodes

in addition to all node types above

* `TRANSFER_ON_WIN=` true or false depending on whether you want the auction to transfer the tokens and winning bid automatically at the end.

### Master node

in addition to all node types above

* `MASTER_KEY=` The ED25519 private key you generated (set only for one node which has additional authority over the auction accounts, can be the same as operator key for testing purposes only, else must be different)
* `TRANSFER_ON_WIN=` true or false depending on whether you want the auction to transfer the tokens and winning bid automatically at the end.

## Creating the topic ID to share with the rest of the network

From the command line of your node (assuming the admin API is enabled)

```shell
curl -H "Content-Type: application/json" -X POST -d '
{
}
' http://localhost:8082/v1/admin/topic
```

This will create and output a topic id and will also update your `.env` file with its value. You may now share this topic Id with the rest of the application network participants.

## Creating a token to auction

You may now create a token to auction, see documentation above for helpers if you're not sure how to do this.

## Creating an account for a token auction

This action needs to be performed for every new token to be auctioned, the same account cannot be used for two different tokens, the application will reject the auction creation if this is the case.

This command will create an auction account with an initial balance of `100` hbar, and a key list for scheduled transactions.

_Note: if the environment file contains an entry for `MASTER_KEY`, it will automatically be added to the keylist below with a threshold of 1, resulting in an auction account having a threshold key of 1 of 2, one of the keys being the master key, the other being the threshold key supplied in the JSON._

* Replace and add "validator n public key" as required

* Set threshold on the inner key list as required (leave the last threshold before `initialBalance` to 1).

_Note: all keys are *public* keys_

```shell script
curl -H "Content-Type: application/json" -X POST -d '
{
  "keylist": {
    "keys": [
      {
        "key": "validator 1 public key"
      },
      {
        "key": "validator 2 public key"
      },
      {
        "key": "validator 3 public key"
      }
    ],
    "threshold": 2
  },
  "initialBalance": 100
}' http://localhost:8082/v1/admin/auctionaccount
```

## Creating an auction

be sure the replace `{{tokenId}}`, `{{accountId}}` in the json below with the values you obtained earlier, you may also set different values for:

* `reserve` in tinybars
* `minimumbid` in tinybars
* `endtimestamp` will default to 2 days in the future if not set, otherwise specify the date and time you wish the auction to end in seconds since epoch or
    * `2m` for two minutes after the consensus timestamp of the HCS message
    * `4h` for four hours after the consensus timestamp of the HCS message
    * `1d` for 1 day after the consensus timestamp of the HCS message
* `winnercanbid` whether the highest bidder is allowed to place a higher bid
* `title` and `description` for the auction (rendered in the UI)

```shell script
curl -H "Content-Type: application/json" -X POST -d '
{
  "tokenid": "{{tokenId}}",
  "auctionaccountid": "{{accountId}}",
  "reserve": 0,
  "minimumbid": 1000000,
  "endtimestamp": "",
  "winnercanbid": true,
  "title": "Auction title",
  "description": "Auction description"
}' http://localhost:8082/v1/admin/auction
```

_Note: the minimum bid and reserve are expressed in `tinybars`_

This will submit a HCS message on the application network's topic id so that all participants are aware of the auction.

## Transfer the token to the auction account

This has to be done by the token creator so that the auction for the token can start. At the end of the auction, the token will be transferred to the winner and the hbar proceeds transferred to the token owner. In the event there are no bids, the token is transferred back to the owner.

## Alternative with gradle

Copy the sample yaml file and edit

```shell
cd hedera-nft-auction-demo-java-node
cp AuctionSetup.yaml.sample AuctionSetup.yaml
nano AuctionSetup.yaml
```

specify whether a new topic should be created or not

```yaml
createTopic: true
```

specify the account to use to create the token

```yaml
setupOperator:
  accountId: 0.0.xxxxx
  privateKey: 302.....
```

specify the token details

```yaml
token:
  name: Token Name
  symbol: Symbol
```


specify the public keys, threshold and initial balance to use for the auction account

```yaml
auctionAccount:
  publicKeys:
    - 302a300506032b6570032100130044fa6c178739733d525210d2965cb89420255335349e50c8b329e4732c75
    - 302a300506032b65700321008ba273d242fb1ebd3c66c26d88c5c433876d5cffdfd6e5520a151034eb9eabff
  threshold: 2
  balance: 10
```

specify the auction's details

```yaml
auction:
  reserve: 0
  minimumbid: 10
  endtimestamp: 2d
  winnercanbid: true
  title: auction title
  description: auction description
```

and the host for the admin api calls

```yaml
adminApiHost: http://localhost:8082
```

then run the setup

```shell
./gradlew setupHelper
```

## Sending bids to the auction

_Note: This uses `.env` to read the operator keys_

The exerciser creates a number of pre-defined accounts and funds them with 10 hbar from the operator's account as specified in the `.env` file, if these accounts already exist (they are saved in a file), the code checks their balance is sufficient and if not, tops them up to 10 hbar.

When run, the exerciser starts a number of threads and within each thread sends a number of bids.

Finally, the exerciser outputs the highest bid (or bids if several equal bids were sent).

Copy the sample yaml file and edit

```shell
cd hedera-nft-auction-demo-java-node
cp AuctionSetup.yaml.sample AuctionSetup.yaml
AuctionSetup.yaml
```

specify the auction account to send bids to, the number of accounts to send from, the number of threads and transfers to run.

```yaml
exerciser:
  auctionAccount = 0.0.xxxx
  numAccounts = 10
  numThreads = 4
  numTransfers = 4
```

```shell
./gradlew exerciseAuction 
```

## Validators information for rendering in the UI

You may list the validators who participate in the network in the UI by adding them to the database via the REST API.

After sending the request to the admin API, a message will be sent to the TOPIC ID so that any other participants' list of validators will be updated automatically.

_Note: in the event of a create or update, if a parameter such as `url` isn't specified, it will be set to an empty string._

```shell script
curl -H "Content-Type: application/json" -X POST -d '
{
    "validators": [
    {
      "name": "name of the validator",
      "url": "url of the company or of the location where their UI is hosted",
      "publicKey": "optional, may be used later",
      "operation" : "add"
    }
  ]
}' http://localhost:8082/v1/admin/validators
```

You may modify the details of a validator as follows:

```shell script
curl -H "Content-Type: application/json" -X POST -d '
{
    "validators": [
    {
      "nameToUpdate": "name of the validator to update",
      "name": "new name of the validator",
      "url": "new url",
      "publicKey": "new public key",
      "operation" : "update"
    }
  ]
}' http://localhost:8082/v1/admin/validators
```

And finally, you may delete details of a validator as follows:

```shell script
curl -H "Content-Type: application/json" -X POST -d '
{
  "validators": [
    {
      "name": "name of the validator to delete",
      "operation" : "delete"
    }
  ]
}' http://localhost:8082/v1/admin/validators
```
