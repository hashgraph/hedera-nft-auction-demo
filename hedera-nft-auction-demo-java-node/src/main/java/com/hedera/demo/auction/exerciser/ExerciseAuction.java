package com.hedera.demo.auction.exerciser;

import com.google.common.base.Splitter;
import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.HederaClient;
import com.hedera.hashgraph.sdk.AccountBalance;
import com.hedera.hashgraph.sdk.AccountBalanceQuery;
import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import com.hedera.hashgraph.sdk.TransferTransaction;
import lombok.extern.log4j.Log4j2;
import org.jooq.tools.StringUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j2
public final class ExerciseAuction {

  private ExerciseAuction() {
  }

  public static void main(String[] args) throws Exception {

    HederaClient hederaClient = new HederaClient();
    Client client = hederaClient.client();

    InputStream inputStream = new FileInputStream("./AuctionSetup.yaml");
    Yaml yaml = new Yaml(new Constructor(SetupProperties.class));
    SetupProperties setupProperties = yaml.load(inputStream);

    log.info("starting exerciser");

    String ACCOUNTS_FILE = "accountsFile.txt";
    Hbar TENHBAR = new Hbar(10);

    // check file containing accounts exists
    Path accountFile = Paths.get(ACCOUNTS_FILE);
    if (!Files.exists(accountFile)) {
      Files.createFile(accountFile);
    }

    List<String> accounts = Files.readAllLines(accountFile);

    int totalAccounts = accounts.size();
    if (totalAccounts < setupProperties.getExerciser().getNumAccounts()) {
      log.info("creating new accounts");
      PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(ACCOUNTS_FILE, StandardCharsets.UTF_8, /* append= */true)));
      for (int i=1; i <= setupProperties.getExerciser().getNumAccounts() - totalAccounts; i++) {
        // add a new account to the file
        PrivateKey privateKey = PrivateKey.generate();
        TransactionResponse transactionResponse = new AccountCreateTransaction()
                .setInitialBalance(TENHBAR)
                .setKey(privateKey.getPublicKey())
                .execute(client);
        TransactionReceipt receipt = transactionResponse.getReceipt(client);
        AccountId accountId = receipt.accountId;
        String accountLine = accountId.toString().concat(",").concat(privateKey.toString());
        out.println(accountLine);
        accounts.add(accountLine);
      }
      out.close();
    }

    log.info("Loading list of accounts");
    List<AccountId> accountsIds = new ArrayList<>();
    List<PrivateKey> privateKeys = new ArrayList<>();

    for (String account : accounts) {
      if ( ! StringUtils.isEmpty(account)) {
        List<String> accountRow = Splitter.on(",").splitToList(account);
        accountsIds.add(AccountId.fromString(accountRow.get(0)));
        privateKeys.add(PrivateKey.fromString(accountRow.get(1)));
      }
    }

    log.info("Checking for sufficient balances and topping up");

    for (AccountId accountId : accountsIds) {
      AccountBalance accountBalance = new AccountBalanceQuery()
              .setAccountId(accountId)
              .execute(client);

      if (accountBalance.hbars.toTinybars() < TENHBAR.toTinybars()) {
        long delta = TENHBAR.toTinybars() - accountBalance.hbars.toTinybars();

        TransactionResponse transactionResponse = new TransferTransaction()
                .addHbarTransfer(hederaClient.operatorId(), Hbar.fromTinybars(delta).negated())
                .addHbarTransfer(accountId, Hbar.fromTinybars(delta))
                .execute(client);

        transactionResponse.getReceipt(client);
      }
    }

    ExecutorService executor = Executors.newCachedThreadPool();
    Collection<ExerciseWinner> results = new ConcurrentLinkedQueue<>();
    CompletableFuture<?>[] allFutures = new CompletableFuture[setupProperties.getExerciser().getNumThreads()];
    for (int i = 0; i < setupProperties.getExerciser().getNumThreads(); i++) {
      CompletableFuture<ExerciseWinner> future = CompletableFuture.supplyAsync(()->
        new ExerciseTransfer(AccountId.fromString(setupProperties.getExerciser().getAuctionAccount()), accountsIds, privateKeys, setupProperties.getExerciser().getNumTransfers()).get()
              , executor);
      allFutures[i] = future.thenAccept(results::add);
    }

    List<ExerciseWinner> winners = new ArrayList<>();

    CompletableFuture.allOf(allFutures).thenAccept(c->{
      @Var long maxBid = 0;

      for (ExerciseWinner exerciseWinner : results) {
        if (exerciseWinner.accountId != null) {
          if (maxBid < exerciseWinner.amount) {
            maxBid = exerciseWinner.amount;
            winners.clear();
            winners.add(exerciseWinner);
          } else if (maxBid == exerciseWinner.amount) {
            winners.add(exerciseWinner);
          }
        }
      }

      if (winners.size() > 1) {
        log.warn("Equal amounts bid by different bidders");
      }
      System.out.println("Winning amount " + winners.get(0).amount);
      for (ExerciseWinner exerciseWinner : winners) {
        if (exerciseWinner.accountId != null) {
          System.out.println("Winning account(s) " + exerciseWinner.accountId.toString());
        }
      }
    })
    .exceptionally(exception -> {
      log.error(exception);
      return null;
    });

    System.out.println("End of transfers");

  }
}


