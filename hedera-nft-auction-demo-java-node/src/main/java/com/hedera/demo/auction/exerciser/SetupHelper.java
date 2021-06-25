package com.hedera.demo.auction.exerciser;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.CreateTopic;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TokenCreateTransaction;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import com.hedera.hashgraph.sdk.TransferTransaction;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.json.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

public final class SetupHelper {

  private SetupHelper() {

  }

  public static void main(String[] args) throws Exception {

    Dotenv env = Dotenv.configure().ignoreIfMissing().load();
    AccountId testOperator = AccountId.fromString(env.get("EXERCISE_OPERATOR_ID"));
    PrivateKey testOperatorKey = PrivateKey.fromString(env.get("EXERCISE_OPERATOR_KEY"));

    Client client = Client.forTestnet();
    client.setOperator(testOperator, testOperatorKey);
    @Var TransactionResponse response;
    @Var TransactionReceipt receipt;

    // create topic
    CreateTopic createTopic = new CreateTopic();
    createTopic.create();

    System.out.println("Topic Created");
    System.out.println("Update .env in environments and restart");
    pressAnyKeyToContinue();

    System.out.println("Creating token");

    response = new TokenCreateTransaction()
            .setDecimals(0)
            .setInitialSupply(1)
            .setTokenName("test")
            .setTokenSymbol("test")
            .setTreasuryAccountId(testOperator)
            .execute(client);

    receipt = response.getReceipt(client);

    TokenId tokenId = receipt.tokenId;

    // create auction account
    // Account create JSON
    String createAccountString = "    {" +
            "      \"keyList\" : {" +
            "      \"keys\": [" +
            "      {" +
            "        \"key\" : \"302a300506032b6570032100362c7be3137d0c6f53b9d89cb591c8bfb0ce4d28e8b0549948da5a7db9b9d768\"" +
            "      }," +
            "      {" +
            "        \"keyList\": {" +
            "        \"keys\": [" +
            "        {" +
            "          \"key\": \"302a300506032b6570032100130044fa6c178739733d525210d2965cb89420255335349e50c8b329e4732c75\"" +
            "        }," +
            "        {" +
            "          \"key\": \"302a300506032b65700321008ba273d242fb1ebd3c66c26d88c5c433876d5cffdfd6e5520a151034eb9eabff\"" +
            "        }" +
            "          ]," +
            "        \"threshold\": 1" +
            "      }" +
            "      }" +
            "    ]," +
            "      \"threshold\" : 1" +
            "    }," +
            "      \"initialBalance\": 100" +
            "    }    ";
    JsonObject createAccountJson = new JsonObject(createAccountString);// .mapFrom(createAccountString);

    System.out.println("Creating auction account");

    @Var HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://hedera-nft-auction:8082/v1/admin/auctionaccount"))
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(createAccountJson.toString()))
            .build();

    @Var HttpClient httpClient = HttpClient.newHttpClient();
    @Var HttpResponse<String> httpResponse = httpClient.send(request, BodyHandlers.ofString());
    JsonObject jsonResponse = new JsonObject(httpResponse.body());
    AccountId accountId = AccountId.fromString(jsonResponse.getString("accountId"));

    // create auction
    @Var String createAuction = "{" +
          "    \"tokenid\": \"{{tokenId}}\"," +
          "    \"auctionaccountid\": \"{{accountId}}\"," +
          "    \"reserve\": \"\"," +
          "    \"minimumbid\": \"10\"," +
          "    \"endtimestamp\": \"\"," +
          "    \"winnercanbid\": true," +
          "    \"title\": \"auction title\"," +
          "    \"description\": \"auction description\"" +
          "}";
    createAuction = createAuction.replace("{{tokenId}}", tokenId.toString());
    createAuction = createAuction.replace("{{accountId}}", accountId.toString());

    JsonObject createAuctionJson = new JsonObject(createAuction);

    System.out.println("Creating auction");

    request = HttpRequest.newBuilder()
            .uri(URI.create("http://hedera-nft-auction:8082/v1/admin/auction"))
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(createAuctionJson.encode()))
            .build();


    httpClient = HttpClient.newHttpClient();
    httpResponse = httpClient.send(request, BodyHandlers.ofString());

    System.out.println(httpResponse.body());

    // transfer token
    System.out.println("Start the containers so that the auction account associates with the token");
    System.out.println("Press a key to transfer the token to the auction");
    pressAnyKeyToContinue();

    response = new TransferTransaction()
            .addTokenTransfer(tokenId, testOperator, -1)
            .addTokenTransfer(tokenId, accountId, 1)
            .execute(client);

    response.getReceipt(client);
  }

  private static void pressAnyKeyToContinue()
  {
    System.out.println("Press Enter key to continue...");
    try
    {
      System.in.read();
      System.out.println("Continuing...");
    }
    catch(Exception e) {
      System.out.println(e.getMessage());
    }
  }
}


