package com.hedera.demo.auction.node.app;

import com.google.errorprone.annotations.Var;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.FileAppendTransaction;
import com.hedera.hashgraph.sdk.FileCreateTransaction;
import com.hedera.hashgraph.sdk.FileId;
import com.hedera.hashgraph.sdk.FileUpdateTransaction;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.KeyList;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TokenCreateTransaction;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import lombok.extern.log4j.Log4j2;

import java.nio.file.Files;
import java.nio.file.Path;

@Log4j2
public class CreateToken extends AbstractCreate {

    public CreateToken() throws Exception {
        super();
    }

    /**
     * Creates a simple token (no kyc, freeze, supply, etc...)
     * @param name the name of the token
     * @param symbol the symbol for the token
     * @param initialSupply the initial supply for the token (defaults to 1)
     * @param decimals the number of decimals for the token (defaults to 0)
     * @param tokenMemo the memo (string) to associate with the token
     * @throws Exception in the event of an exception
     */
    public TokenId create(String name, String symbol, long initialSupply, int decimals, String tokenMemo) throws Exception {

        Client client = hederaClient.client();
        client.setMaxTransactionFee(Hbar.from(100));

        @Var String tokenSymbol = symbol;

        if (Files.exists(Path.of(symbol))) {
            // the symbol is a valid file name, let's create a file on Hedera with the contents
            String contents = Utils.readFileIntoString(symbol);
            FileCreateTransaction fileCreateTransaction = new FileCreateTransaction();
            fileCreateTransaction.setContents("");
            fileCreateTransaction.setKeys(client.getOperatorPublicKey());
            try {
                @Var TransactionResponse response = fileCreateTransaction.execute(client);
                @Var TransactionReceipt receipt = response.getReceipt(client);
                if (receipt.status != Status.SUCCESS) {
                    log.error("File creation for token failed " + receipt.status);
                    throw new Exception("File creation for token failed " + receipt.status);
                } else {
                    log.info("Token file created " + receipt.fileId.toString());

                    FileId fileId = receipt.fileId;
                    // append data now
                    FileAppendTransaction fileAppendTransaction = new FileAppendTransaction();
                    fileAppendTransaction.setFileId(fileId);
                    fileAppendTransaction.setContents(contents);
                    response = fileAppendTransaction.execute(client);
                    receipt = response.getReceipt(client);

                    if (receipt.status != Status.SUCCESS) {
                        log.error("File append for token failed " + receipt.status);
                        throw new Exception("File append for token failed " + receipt.status);
                    } else {
                        log.info("Token data appended to file");
                        FileUpdateTransaction fileUpdateTransaction = new FileUpdateTransaction();
                        fileUpdateTransaction.setFileId(fileId);
                        KeyList keys = new KeyList();
                        fileUpdateTransaction.setKeys(keys);
                        response = fileUpdateTransaction.execute(client);
                        receipt = response.getReceipt(client);
                        if (receipt.status != Status.SUCCESS) {
                            log.error("File removal of keys failed " + receipt.status);
                            throw new Exception("File removal of keys failed " + receipt.status);
                        } else {
                            log.info("File is now immutable");
                            tokenSymbol = "HEDERA://" + fileId.toString();
                        }
                    }
                }
            } catch (Exception e) {
                log.error(e);
                throw e;
            }
        }

        try {
            TokenCreateTransaction tokenCreateTransaction = new TokenCreateTransaction();
            tokenCreateTransaction.setTokenName(name);
            tokenCreateTransaction.setTokenSymbol(tokenSymbol);
            tokenCreateTransaction.setInitialSupply(initialSupply);
            tokenCreateTransaction.setDecimals(decimals);
            tokenCreateTransaction.setTreasuryAccountId(hederaClient.operatorId());
            tokenCreateTransaction.setTokenMemo(tokenMemo);

            TransactionResponse response = tokenCreateTransaction.execute(client);

            TransactionReceipt receipt = response.getReceipt(client);
            if (receipt.status != Status.SUCCESS) {
                log.error("Token creation failed " + receipt.status);
                throw new Exception("Token creation failed " + receipt.status);
            } else {
                log.info("Token created " + receipt.tokenId.toString());
            }
            return receipt.tokenId;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            log.error("Invalid number of arguments supplied - name and symbol are required at a minimum");
        } else {
            log.info("Creating token");
            @Var long initialSupply = 1;
            @Var int decimals = 0;
            @Var String memo = "";
            if (args.length >= 3) {
                initialSupply = Long.parseLong(args[2]);
            }
            if (args.length >= 4) {
                decimals = Integer.parseInt(args[3]);
            }
            if (args.length >= 5) {
                memo = args[4];
            }
            CreateToken createToken = new CreateToken();
            createToken.create(args[0], args[1], initialSupply, decimals, memo);
        }
    }
}
