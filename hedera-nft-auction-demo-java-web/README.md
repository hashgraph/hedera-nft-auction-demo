# Hedera Non Fungible Token Auction Demo

## Notes
Lombok (for eclipse)
In addition to having Lombok plugin installed, also make sure that the "Enable annotation processing" checkbox is ticked under:

Preferences > Compiler > Annotation Processors
Note: starting with IntelliJ 2017, the "Enable Annotation Processing" checkbox has moved to:

Settings > Build, Execution, Deployment > Compiler > Annotation Processors

## Setting up

A number of helper functions are available from the project in order to get you started quickly.

### Super simple

This command will run all the necessary steps to create a demo auction:
* create a HCS Topic
* create a simple token
* create an auction account
* associate the token to the auction account and transfer it to the same account
* create an auction file
* setup the auction

```shell
./gradlew easySetup
```

### Step by step

These steps will enable you to create an `initDemo.json` file which you can finally use to setup a new auction.

#### Create a topic

```shell
./gradlew createTopic
```

#### Create a simple token

This command will create a token named `test` with a symbol of `tst`, an initial supply of `1` and `0` decimals.

```shell
./gradlew createToken --args="test tst 1 0"
```

set the resulting `Token Id` to the `tokenId` attribute in your `initDemo.json` file.

#### Create an auction account

This command will create an auction account with an initial balance of `100` hbar and a threshold key of `1`.

```shell
./gradlew createAuctionAccount --args="100 1"
```

set the resulting `Account Id` to the `auctionaccountid` attribute in your `initDemo.json` file.

#### Associate the token with the auction account and transfer

This will associate the token with the auction account and transfer it from the account that created it to the `auctionaccountid`, supply the `tokenId` and `accountId` created above in the parameters.

```shell
./gradlew createTokenAssociation --args="tokenId accountId"
```

#### Finalising the initDemo.json file

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

#### Create the auction

This command will submit your `initDemo.json` file to the `Topic Id` created earlier.

```shell
./gradlew createAuction --args="./initDemo.json"
```

