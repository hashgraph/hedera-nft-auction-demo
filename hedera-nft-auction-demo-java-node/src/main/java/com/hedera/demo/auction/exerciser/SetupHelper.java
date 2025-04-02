package com.hedera.demo.auction.exerciser;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.CreateTopic;
import com.hedera.hashgraph.sdk.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

public final class SetupHelper {

  private SetupHelper() {
  }

  public static void main(String[] args) throws Exception {

    InputStream inputStream = new FileInputStream("./AuctionSetup.yaml");
    var yaml = new Yaml(new Constructor(SetupProperties.class, new LoaderOptions()));
    SetupProperties setupProperties = yaml.load(inputStream);

    Client client = Client.forTestnet();
    client.setOperator(setupProperties.getSetupOperator().accountId(), setupProperties.getSetupOperator().privateKey());

    // create topic
    if (setupProperties.isCreateTopic()) {
      var createTopic = new CreateTopic();
      createTopic.create();

      System.out.println("Topic Created");
      System.out.println("Update .env in environments and restart");
      pressAnyKeyToContinue();
    }

    System.out.println("Creating token");

    @Var var response = new TokenCreateTransaction()
            .setDecimals(0)
            .setInitialSupply(1)
            .setTokenName(setupProperties.getToken().getName())
            .setTokenSymbol(setupProperties.getToken().getSymbol())
            .setTreasuryAccountId(setupProperties.getSetupOperator().accountId())
            .execute(client);

    var receipt = response.getReceipt(client);

    TokenId tokenId = receipt.tokenId;

    // create auction account
    // Account create JSON

    var keys = new JsonArray();
    for (String key : setupProperties.getAuctionAccount().getPublicKeys()) {
      JsonObject keyJson = new JsonObject().put("key", key);
      keys.add(keyJson);
    }
    var keyList = new JsonObject();
    keyList.put("keys", keys);
    keyList.put("threshold", setupProperties.getAuctionAccount().getThreshold());

    var accountJson = new JsonObject();
    accountJson.put("keylist", keyList);
    accountJson.put("initialBalance", setupProperties.getAuctionAccount().getBalance());

    System.out.println("Creating auction account");

    @Var HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(setupProperties.getAdminApiHost().concat("/v1/admin/auctionaccount")))
            .header("Content-Type", "application/json")
            .header("x-api-key", setupProperties.getXApiKey())
            .POST(BodyPublishers.ofString(accountJson.toString()))
            .build();

    @Var HttpClient httpClient = HttpClient.newHttpClient();
    @Var HttpResponse<String> httpResponse = httpClient.send(request, BodyHandlers.ofString());
    var jsonResponse = new JsonObject(httpResponse.body());
    AccountId accountId = AccountId.fromString(jsonResponse.getString("accountId"));

    // create auction
    var auction = new JsonObject();
    assert tokenId != null;
    auction.put("tokenid", tokenId.toString());
    auction.put("auctionaccountid", accountId.toString());
    auction.put("reserve", setupProperties.getAuction().getReserve());
    auction.put("minimumbid", setupProperties.getAuction().getMinimumbid());
    auction.put("endtimestamp", setupProperties.getAuction().getEndtimestamp());
    auction.put("winnercanbid", setupProperties.getAuction().isWinnercanbid());
    auction.put("title", setupProperties.getAuction().getTitle());
    auction.put("description", setupProperties.getAuction().getDescription());

    request = HttpRequest.newBuilder()
            .uri(URI.create(setupProperties.getAdminApiHost().concat("/v1/admin/auction")))
            .header("Content-Type", "application/json")
            .header("x-api-key", setupProperties.getXApiKey())
            .POST(BodyPublishers.ofString(auction.encode()))
            .build();

    httpClient = HttpClient.newHttpClient();
    httpResponse = httpClient.send(request, BodyHandlers.ofString());

    System.out.println(httpResponse.body());

    @Var String output = "Waiting for token association from auction account";
    @Var boolean keepChecking = true;
    while (keepChecking) {
      System.out.println(output);
      AccountBalance balance = new AccountBalanceQuery()
              .setAccountId(accountId)
              .execute(client);
//TODO: mirror node query
      if (balance.token.containsKey(tokenId)) {
        keepChecking = false;
      } else {
        Thread.sleep(Duration.ofSeconds(5));
        output += " +5s";
      }
    }
    System.out.println();
    System.out.println("Transferring token to auction account");

    response = new TransferTransaction()
            .addTokenTransfer(tokenId, setupProperties.getSetupOperator().accountId(), -1)
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


