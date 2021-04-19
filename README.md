[![Java CI with Gradle](https://github.com/hashgraph/hedera-nft-auction-demo/actions/workflows/unit-integration-test.yml/badge.svg)](https://github.com/hashgraph/hedera-nft-auction-demo/actions/workflows/unit-integration-test.yml)
[![codecov](https://img.shields.io/codecov/c/github/hashgraph/hedera-nft-auction-demo/master)](https://codecov.io/gh/hashgraph/hedera-nft-auction-demo)
[![GitHub](https://img.shields.io/github/license/hashgraph/hedera-nft-auction-demo)](LICENSE)
[![Discord](https://img.shields.io/badge/discord-join%20chat-blue.svg)](https://hedera.com/discord)

# Hedera Non Fungible Token Auction Demo

## Dependencies

* A testnet or mainnet account
* PostgreSQL version 12
* Node.js v14.9.0
* Yarn 1.22.10
* Java 14
* Docker and docker-compose (optional)

## Notes

The java projects use Lombok, ensure that the plug is installed in your IDE and configured properly [Lombok Plugin](https://www.baeldung.com/lombok-ide)

*Note that enabling annotation processing differs between versions of IntelliJ `Preferences > Compiler > Annotation Processors` before IntelliJ2017, starting with IntelliJ 2017, the "Enable Annotation Processing" checkbox has moved to: `Settings > Build, Execution, Deployment > Compiler > Annotation Processors`*

## Description

This project consists of two main modules, a Java back end and a Vue.js front end.

The Vue.JS front end displays auctions and enables users to place bids (requires a browser plug in to sign transactions) and monitor the auction's progress.

The back end fulfils 3 separate roles

* UI REST API server for the UI above
* Admin REST API server for admin features (such as creating a new auction)
* Auction and Bid processing, registering new auctions, checking bid validity, issuing refunds

The latter can be run in readonly mode, meaning the processing will validate bids but will not be able to participate in refunds or token transfers on auction completion.

*Note: The three roles can be run within a single instance of the Java application or run in individual Java instances, for example, one instance could process bids, while one or more others could serve the UI REST API for scalability purposes. This is determined by environment variable parameters.
The Docker deployment runs two instances, one for bid processing, the other for the UI and Admin REST APIs for example.*

The admin API runs on a separate port to the UI REST API to ensure it can be firewalled separately and protected from malicious execution.

## Setup, compilation, execution

Pull the repository from github

```shell
git clone https://github.com/hashgraph/hedera-nft-auction-demo.git
```

### With docker

//TODO:

```shell script
./gradlew easySetup --args="--name=myToken --symbol=./sample-files/gold-base64.txt"
```

### Standalone

#### Database

All database objects will be created in the `public` database.

*Note the installation below assumes the user is `postgres` and the password is `password`.*

#### Java Appnet Node

```shell
cd hedera-nft-auction-demo
cd hedera-nft-auction-demo-java-node

# Build the code
./gradlew build
```

setup the environment

```shell
cp .env.sample .env
nano .env
```

set the following properties according to your Hedera account and refund key details

*Note: 
The operator id/key is used to query the hedera network (free queries) 
* It is also used to set the submit key for the auction topic and also for creating the auction account, submitting auction creation messages to the topic.
* And optionally creating a token to auction, then transferring it to the auction account*


* OPERATOR_ID=
* OPERATOR_KEY=302.....
* REFUND_KEY=302.......
* TRANSFER_ON_WIN=true

You may edit additional parameters such as `MIRROR_PROVIDER`, etc... if you wish

#### Javascript UI

```shell
cd hedera-nft-auction-demo
cd hedera-nft-auction-demo-javascript-client
# Build the code
./yarn install
```

Edit environment variables

```shell
cp .env.sample .env
nano .env
```

* `VUE_APP_API_PORT=8081` this is the port of the `Java REST API` above
* `VUE_APP_NETWORK=testnet` previewnet, testnet or mainnet
* `VUE_APP_TOPIC_ID=` topic id the appnet is using
* `PORT=8080` the port you want to run the UI on
* `VUE_APP_NODE_OWNER` optionally set the name of the company operating the node to display in the UI

#### Setting up an auction

A number of helper functions are available from the project in order to get you started quickly.

*Note, this section assumes you are running the commands from the `hedera-nft-auction-demo-java-node` directory.*

```shell
cd hedera-nft-auction-demo
cd hedera-nft-auction-demo-java-node
```

#### Super simple

This command takes a number of parameters runs all the necessary steps to create a demo auction:

* create a HCS Topic
* create a simple token
* create an auction account
* create an auction file
* setup the auction
* transfers the token to the auction

__Parameters__

The following parameter are optional and defaulted if not supplied 

*note, the database will be cleared and a new topic created unless `--no-clean` is provided*

* --name, token's name
* --symbol, this will determine the symbol for the token, if the symbol refers to a file path, a Hedera file entity will be created with the contents and the token's symbol set to the file id 
* --no-clean, do no create a new topic and do not delete data from the database 

__Command line__

```shell
./gradlew easySetup
```

*Note: the application wil need to be restarted to take the new topic into account*

```shell
./gradlew easySetup --args="--name=myToken --symbol=MTT --no-clean"
```

__REST API__

This requires that the REST api and database are up and running

```shell script
curl -H "Content-Type: application/json" -X POST -d '{}' http://localhost:8082/v1/admin/easysetup
```

or

```shell script
curl -H "Content-Type: application/json" -X POST -d '{"symbol":"./sample-files/gold-base64.txt","name":"Test Token","clean":false}' http://localhost:8082/v1/admin/easysetup
```

#### Step by step via command line

These steps will enable you to create an `initDemo.json` file (located in `./sample-files`) which you can finally use to setup a new auction.

*Note: the application wil need to be restarted to take the new topic into account*

__Create a topic__

```shell
./gradlew createTopic
```

__Create a simple token__

This command will create a token named `test` with a symbol of `tst`, an initial supply of `1` and `0` decimals.

```shell
./gradlew createToken --args="test tst 1 0"
```

set the resulting `Token Id` to the `tokenId` attribute in your `./sample-files/initDemo.json` file.

__Create an auction account__

This command will create an auction account with an initial balance of `100` hbar using the operator key for the account.

```shell
./gradlew createAuctionAccount --args="100"
```

*Note: For more complex key structures, use the REST admin api.*

set the resulting `Account Id` to the `auctionaccountid` attribute in your `./sample-files/initDemo.json` file.

__Finalising the initDemo.json file__

Your initDemo.json file should look like this (with your own values).

You can change some of the attribute values if you wish

*Note: if the `endtimestamp` (end of auction in seconds since Epoch) is left blank, the auction will run for 48 hours from now by default.*

```json
{
  "tokenid": "0.0.xxxxxx",
  "auctionaccountid": "0.0.yyyyyy",
  "endtimestamp": "",
  "reserve": 0,
  "minimumbid": 0
}
```

*Note: the minimum bid and reserve are expressed in `tinybars`*

__Create the auction__
```shell
./gradlew createAuction --args="./sample-files/initDemo.json"
```

__Transfer the token to the auction account__

This transfer the token from the account that created it to the `auctionaccountid`, supply the `tokenId` and `accountId` created above in the parameters.

```shell
./gradlew createTokenTransfer --args="tokenId accountId"
```

#### Step by step via REST API

This requires that the REST api and database are up and running. 

The examples below show curl commands, however the `hedera-nft-auction-demo-java-node` project includes a `postman_collection.json` file which you can import into Postman instead.

__Create a topic__

*Note: the application wil need to be restarted to take the new topic into account*

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

This command will create a token named `test` with a symbol of `tst`, an initial supply of `1` and `0` decimals.

```shell script
curl -H "Content-Type: application/json" -X POST -d '
{
  "name": "test", 
  "symbol":"tst", 
  "initialSupply": 1, 
  "decimals": 0
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

```shell script
curl -H "Content-Type: application/json" -X POST -d '
{
 "keylist": [
     {
         "keys": [
             {"key": "302a300506032b657003210090ec5045925d37b358ee0c60f858dc79c3b4370cbf7e0c5dad882f1171265cb3"},
            {"key": "302a300506032b657003210076045799d169c6b6fc2bf45f779171a1cb10fd239b4f758bc556cb0de6799105"},
            {"key": "302a300506032b65700321001481572a21874fb9da18b49f0265aca8d94f435a879c0d6631b8ce54d96dc58c"}
         ],
         "threshold": 2
     },
     {
         "keys": [
             {"key": "302a300506032b6570032100977755b19ba16e152326ea921973aeca57907757e9ad528c34c971b63eab9e31"},
            {"key": "302a300506032b65700321002d140a12f637b7a29a97034a552a7d228f93f1c941480fcb2411af1763b18c67"}
         ]
     }
 ],
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
  "reserve": "", 
  "minimumbid": "1000000", 
  "endtimestamp": "", 
  "winnercanbid": true
}' http://localhost:8082/v1/admin/auction
```

*Note: the minimum bid and reserve are expressed in `tinybars`*

__Transfer the token to the auction account__

This transfer the token from the account that created it to the `auctionaccountid`, supply the `tokenId` and `accountId` created above in the parameters.

be sure the replace `{{tokenId}}`, `{{accountId}}` in the json below with the values you obtained earlier.

```shell script
curl -H "Content-Type: application/json" -X POST -d '
{
  "tokenid" : "{{tokenId}}", 
  "auctionaccountid" : "{{accountId}}"
}' http://localhost:8082/v1/admin/transfer
```

#### Run the components

*Note: Each of the steps below need to be run from a different command line window*

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
yarn serve
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
