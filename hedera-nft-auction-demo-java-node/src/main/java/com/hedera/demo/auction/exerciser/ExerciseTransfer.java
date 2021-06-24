package com.hedera.demo.auction.exerciser;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.HederaClient;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TransferTransaction;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Log4j2
public class ExerciseTransfer implements Supplier<ExerciseWinner> {

  private final AccountId auctionAccount;
  private final List<AccountId> accounts;
  private final List<PrivateKey> privateKeys;
  private final int numTransfers;
  private final ExerciseWinner exerciseWinner = new ExerciseWinner();

  public ExerciseTransfer(AccountId auctionAccount, List<AccountId> accounts, List<PrivateKey> privateKeys, int numTransfers) {
    this.auctionAccount = auctionAccount;
    this.accounts = accounts;
    this.privateKeys = privateKeys;
    this.numTransfers = numTransfers;
  }


  @Override
  public ExerciseWinner get() {

    @Var HederaClient hederaClient;
    try {
      @Var long maxBid = 0;
      Random random = new Random();
      for (int i=0; i < numTransfers; i++) {
        // randomly pick an account to send from
        int index  = random.nextInt(accounts.size());
        long amount = 10 + (long) (Math.random() * (10000 - 10));

        AccountId fromAccount = accounts.get(index);
        PrivateKey privateKey = privateKeys.get(index);

        hederaClient = new HederaClient();
        Client client = hederaClient.client();
        client.setOperator(fromAccount, privateKey);

        try {
          log.info("Transferring {} from {}", amount, fromAccount.toString());
          new TransferTransaction()
                  .addHbarTransfer(auctionAccount, Hbar.fromTinybars(amount))
                  .addHbarTransfer(fromAccount, Hbar.fromTinybars(amount).negated())
                  .freezeWith(client)
                  .sign(privateKey)
                  .execute(client);

          if (amount > maxBid) {
            maxBid = amount;
            exerciseWinner.accountId = fromAccount;
            exerciseWinner.amount = maxBid;
          }

        } catch (TimeoutException timeoutException) {
          timeoutException.printStackTrace();
        } catch (PrecheckStatusException precheckStatusException) {
          precheckStatusException.printStackTrace();
        }
      }
      return this.exerciseWinner;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return new ExerciseWinner();
  }
}
