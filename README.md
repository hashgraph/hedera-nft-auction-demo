# Hedera Non Fungible Token Auction Demo

## Dependencies

* A testnet or mainnet account
* PostgreSQL version 12
* Node.js v14.9.0
* Yarn 1.22.10
* Java 14
* Docker and docker-compose (optional)

## Notes

The java projects use Lombok, ensure that the plug is in installed and configured properly [Lombok Plugin](https://www.baeldung.com/lombok-ide)

Note that enabling annotation processing differs between versions of IntelliJ `Preferences > Compiler > Annotation Processors` before IntelliJ2017, starting with IntelliJ 2017, the "Enable Annotation Processing" checkbox has moved to: `Settings > Build, Execution, Deployment > Compiler > Annotation Processors`

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

Create a database named `auctions`, note the installation below asssumes the user is `postgres` and the password is `password`, if your user, password and database names are different, you will need to edit `build.gradle` to reflect those.

#### Java Appnet Node

```shell
cd hedera-nft-auction-demo
cd hedera-nft-auction-demo-java-node
# Setup the database objects
./gradlew flywayMigrate
# Build the database classes
# ./gradlew jooqGenerate
# Build the code
./gradlew build
```

setup the environment

```shell
cp .env.sample .env
nano .env
```

set the following properties according to your Hedera account details

* OPERATOR_ID=
* OPERATOR_KEY=302.....
* REFUND_KEY=302.......

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

#### Setting up an auction

A number of helper functions are available from the project in order to get you started quickly.

Note, this section assumes you are running the commands from the `hedera-nft-auction-demo-java-node` directory.

```shell
cd hedera-nft-auction-demo
cd hedera-nft-auction-demo-java-node
```

#### Super simple

This command takes a number of parameters runs all the necessary steps to create a demo auction:

* create a HCS Topic
* create a simple token
* create an auction account
* associate the token to the auction account and transfer it to the same account
* create an auction file
* setup the auction

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

```shell
./gradlew easySetup --args="--name=myToken --symbol=MTT --no-clean"
```

__REST API__

This requires that the REST api and database are up and running

```shell script
curl -H "Content-Type: application/json" -X POST -d '{}' http://localhost:8081/v1/easysetup
```

or

```shell script
curl -H "Content-Type: application/json" -X POST -d '{"symbol":"./sample-files/gold-base64.txt","name":"Test Token","clean":false}' http://localhost:8081/v1/easysetup
```

#### Step by step via command line

These steps will enable you to create an `initDemo.json` file (located in `./sample-files`) which you can finally use to setup a new auction.

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

This command will create an auction account with an initial balance of `100` hbar and a threshold key of `1`.

```shell
./gradlew createAuctionAccount --args="100 1"
```

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

__Create the auction__
```shell
./gradlew createAuction --args="./sample-files/initDemo.json"
```

__Associate the token with the auction account and transfer__

This will associate the token with the auction account.

```shell
./gradlew createTokenAssociation --args="tokenId accountId"
```

__Transfer the token to the auction account__

This transfer the token from the account that created it to the `auctionaccountid`, supply the `tokenId` and `accountId` created above in the parameters.

```shell
./gradlew createTokenTransfer --args="tokenId accountId"
```

#### Step by step via REST API

This requires that the REST api and database are up and running

__Create a topic__

```shell script
curl -H "Content-Type: application/json" -X POST -d '{}' http://localhost:8081/v1/topic
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
curl -H "Content-Type: application/json" -X POST -d '{"name": "test", "symbol":"tst", "initialSupply": 1, "decimals": 0}' http://localhost:8081/v1/token
```

returns a token id

```json
{
    "tokenId": "0.0.58792"
}
```

__Create an auction account__

This command will create an auction account with an initial balance of `100` hbar and a threshold key of `1`.

```shell script
curl -H "Content-Type: application/json" -X POST -d '{}' http://localhost:8081/v1/auctionaccount
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
curl -H "Content-Type: application/json" -X POST -d '{"tokenid": "{{tokenId}}", "auctionaccountid": "{{accountId}}", "reserve": "", "minimumbid": "10", "endtimestamp": "", "winnercanbid": true}' http://localhost:8081/v1/auction
```

__Associate the token with the auction account and transfer__

This will associate the token with the auction account.

be sure the replace `{{tokenId}}`, `{{accountId}}` in the json below with the values you obtained earlier.

```shell script
curl -H "Content-Type: application/json" -X POST -d '{"tokenid" : "{{tokenId}}", "auctionaccountid" : "{{accountId}}"}' http://localhost:8081/v1/associate
```

__Transfer the token to the auction account__

This transfer the token from the account that created it to the `auctionaccountid`, supply the `tokenId` and `accountId` created above in the parameters.

be sure the replace `{{tokenId}}`, `{{accountId}}` in the json below with the values you obtained earlier.

```shell script
curl -H "Content-Type: application/json" -X POST -d '{"tokenid" : "{{tokenId}}", "auctionaccountid" : "{{accountId}}"}' http://localhost:8081/v1/transfer
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
yarn serve
```
