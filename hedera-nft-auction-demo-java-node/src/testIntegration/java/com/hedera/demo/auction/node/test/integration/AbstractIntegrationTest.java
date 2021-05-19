package com.hedera.demo.auction.node.test.integration;

import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.domain.Bid;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class AbstractIntegrationTest {

    private final WebClientOptions webClientOptions = new WebClientOptions()
            .setUserAgent("HederaAuction/1.0")
            .setKeepAlive(false);
    protected WebClient webClient = WebClient.create(Vertx.vertx(), webClientOptions);
    protected final static Dotenv env = Dotenv.configure().filename(".env.integration.sample").ignoreIfMissing().load();

    private int index;

    private String stringPlusIndex(String string) {
        return string.concat(String.valueOf(this.index));
    }

    long winningBid() {
        return 30L + this.index;
    }
    String winningAccount() {
        return stringPlusIndex("winningAccount");
    }
    String tokenOwnerAccount() { return stringPlusIndex("tokenOwnerAccount"); }
    String winningTimestamp() {
        return stringPlusIndex("winningTimestamp");
    }
    String endtimestamp() {
        return stringPlusIndex("endtimestamp");
    }
    long reserve() {
        return 20L + this.index;
    }
    String status() {
        return stringPlusIndex("status");
    }
    boolean winnercanbid() {
        return (this.index == 1);
    }
    String winningtxid() {
        return stringPlusIndex("winningtxid");
    }
    String winningtxhash() {
        return stringPlusIndex("winningtxhash");
    }
    String tokenimage() {
        return stringPlusIndex("tokenimage");
    }
    long minimumbid() {
        return 10L + this.index;
    }
    String starttimestamp() {
        return stringPlusIndex("starttimestamp");
    }
    String transfertxid() {
        return stringPlusIndex("transfertxid");
    }
    String transfertxhash() {
        return stringPlusIndex("transfertxhash");
    }
    String lastConsensusTimestamp() {
        return stringPlusIndex("lastConsensusTimestamp");
    }
    String auctionAccountId() {
        return stringPlusIndex("auctionAccountId");
    }
    String tokenId() {
        return stringPlusIndex("tokenId");
    }

    public String timestamp() {
        return stringPlusIndex("timestamp");
    }
    String bidderaccountid() {
        return stringPlusIndex("bidderaccountid");
    }
    Long bidamount() {
        return 30L + this.index;
    }
    String refundtxid() {
        return stringPlusIndex("refundtxid");
    }
    String refundtxhash() {
        return stringPlusIndex("refundtxhash");
    }
    String transactionid() {
        return stringPlusIndex("transactionid");
    }
    String transactionhash() {
        return stringPlusIndex("transactionhash");
    }
    String timestampforrefund() { return stringPlusIndex("timestampforrefund"); }
    String refundStatus() { return Bid.REFUND_ISSUED; }

    String transferStatus() { return stringPlusIndex("transferstatus"); }
    String title() { return stringPlusIndex("title");}
    String description() { return stringPlusIndex("description");}

    protected String masterKey = Optional.ofNullable(env.get("MASTER_KEY")).orElse(""); //TODO: Handle tests where masterNode = false

    protected Auction testAuctionObject(int index) {
        this.index = index;
        Auction auction = new Auction();

        auction.setWinningbid(winningBid());
        auction.setWinningaccount(winningAccount());
        auction.setWinningtimestamp(winningTimestamp());
        auction.setTokenid(tokenId());
        auction.setAuctionaccountid(auctionAccountId());
        auction.setEndtimestamp(endtimestamp());
        auction.setReserve(reserve());
        auction.setStatus(status());
        auction.setWinnercanbid(winnercanbid());
        auction.setWinningtxid(winningtxid());
        auction.setWinningtxhash(winningtxhash());
        auction.setTokenimage(tokenimage());
        auction.setMinimumbid(minimumbid());
        auction.setStarttimestamp(starttimestamp());
        auction.setTransfertxid(transfertxid());
        auction.setTransfertxhash(transfertxhash());
        auction.setLastconsensustimestamp(lastConsensusTimestamp());
        auction.setTokenowneraccount(tokenOwnerAccount());
        auction.setTransferstatus(transferStatus());
        auction.setTitle(title());
        auction.setDescription(description());

        return auction;
    }

    public void testNewAuction(Auction auction, Auction getAuction) {
        assertEquals(auction.getTokenid(),getAuction.getTokenid());
        assertEquals("0.0",getAuction.getLastconsensustimestamp());
        assertEquals(auction.getAuctionaccountid(),getAuction.getAuctionaccountid());
        assertEquals(auction.getEndtimestamp(),getAuction.getEndtimestamp());
        assertEquals(auction.getReserve(),getAuction.getReserve());
        assertEquals(winnercanbid(), getAuction.getWinnerCanBid());
        assertEquals(auction.getTokenimage(),getAuction.getTokenimage());
        assertEquals(auction.getWinningbid(),getAuction.getWinningbid());
        assertEquals(auction.getMinimumbid(),getAuction.getMinimumbid());
        assertEquals(auction.getTitle(), getAuction.getTitle());
        assertEquals(auction.getDescription(), getAuction.getDescription());
    }

    protected Bid testBidObject(int index, int auctionId) {
        this.index = index;
        Bid bid = new Bid();

        bid.setTimestamp(timestamp());
        bid.setAuctionid(auctionId);
        bid.setBidderaccountid(bidderaccountid());
        bid.setBidamount(bidamount());
        bid.setStatus(status());
        bid.setRefundtxid(refundtxid());
        bid.setRefundtxhash(refundtxhash());
        bid.setTransactionid(transactionid());
        bid.setTransactionhash(transactionhash());
        bid.setRefundstatus(refundStatus());
        bid.setTimestampforrefund(timestampforrefund());

        return bid;
    }


    public void testNewBid(Bid bid, Bid newBid) {
        assertEquals(bid.getAuctionid(),newBid.getAuctionid());
        assertEquals(bid.getStatus(),newBid.getStatus());
        assertEquals(bid.getTimestamp(),newBid.getTimestamp());
        assertEquals(bid.getBidamount(),newBid.getBidamount());
        assertEquals(bid.getBidderaccountid(),newBid.getBidderaccountid());
        assertEquals(bid.getTransactionid(),newBid.getTransactionid());
        assertEquals(bid.getTransactionhash(),newBid.getTransactionhash());
        assertEquals(bid.getRefundstatus(), newBid.getRefundstatus());
        assertEquals(bid.getTimestamp(), newBid.getTimestampforrefund());
    }

    protected void migrate(PostgreSQLContainer postgres) {
        String postgresUrl = postgres.getJdbcUrl();
        String postgresUser = postgres.getUsername();
        String postgresPassword = postgres.getPassword();
        Flyway flyway = Flyway
                .configure()
                .dataSource(postgresUrl, postgresUser, postgresPassword)
                .locations("filesystem:./src/main/resources/migrations")
                .load();
        flyway.migrate();

    }

    protected DeploymentOptions getVerticleDeploymentOptions(String databaseUrl, String username, String password) {

        JsonObject config = new JsonObject()
                .put("envFile",".env.integration.sample")
                .put("envPath",".")
                .put("DATABASE_URL",databaseUrl.replace("jdbc:",""))
                .put("DATABASE_USERNAME", username)
                .put("DATABASE_PASSWORD", password)
                .put("POOL_SIZE", "1")
                .put("VUE_APP_API_PORT","9005");
        DeploymentOptions options = new DeploymentOptions().setConfig(config).setInstances(1);
        return options;
    }

    protected void verifyBid(Bid bid, JsonObject body) {
        assertEquals(bid.getTimestamp(), body.getString("timestamp"));
        assertEquals(bid.getAuctionid(), body.getInteger("auctionid"));
        assertEquals(bid.getBidderaccountid(), body.getString("bidderaccountid"));
        assertEquals(bid.getBidamount(), body.getLong("bidamount"));
        assertEquals(bid.getStatus(), body.getString("status"));
        assertEquals(bid.getRefundtxid(), body.getString("refundtxid"));
        assertEquals(bid.getRefundtxhash(), body.getString("refundtxhash"));
        assertEquals(bid.getTransactionid(), body.getString("transactionid"));
        assertEquals(bid.getTransactionhash(), body.getString("transactionhash"));
        assertEquals(bid.getTimestamp(), body.getString("timestampforrefund"));
        assertEquals(bid.getRefundstatus(), body.getString("refundstatus"));
    }

    protected void verifyAuction(Auction auction, JsonObject body) {
        assertEquals(auction.getId(), body.getInteger("id"));
        assertEquals(auction.getLastconsensustimestamp(), body.getString("lastconsensustimestamp"));
        assertEquals(auction.getWinningbid(), body.getLong("winningbid"));
        assertEquals(auction.getWinningaccount(), body.getString("winningaccount"));
        assertEquals(auction.getWinningtimestamp(), body.getString("winningtimestamp"));
        assertEquals(auction.getTokenid(), body.getString("tokenid"));
        assertEquals(auction.getAuctionaccountid(), body.getString("auctionaccountid"));
        assertEquals(auction.getEndtimestamp(), body.getString("endtimestamp"));
        assertEquals(auction.getReserve(), body.getLong("reserve"));
        assertEquals(auction.getStatus(), body.getString("status"));
        assertEquals(auction.getWinningtxid(), body.getString("winningtxid"));
        assertEquals(auction.getWinningtxhash(), body.getString("winningtxhash"));
        assertEquals(auction.getTokenimage(), body.getString("tokenimage"));
        assertEquals(auction.getMinimumbid(), body.getLong("minimumbid"));
        assertEquals(auction.getStarttimestamp(), body.getString("starttimestamp"));
        assertEquals(auction.getTransfertxid(), body.getString("transfertxid"));
        assertEquals(auction.getTransfertxhash(), body.getString("transfertxhash"));
        assertEquals(auction.isActive(), body.getBoolean("active"));
        assertEquals(auction.isPending(), body.getBoolean("pending"));
        assertEquals(auction.isClosed(), body.getBoolean("closed"));
        assertEquals(auction.getWinnerCanBid(), body.getBoolean("winnerCanBid"));
        assertEquals(auction.isTransferPending(), body.getBoolean("transferPending"));
        assertEquals(auction.isEnded(), body.getBoolean("ended"));
        assertEquals(auction.getTokenowneraccount(), body.getString("tokenowneraccount"));
        assertEquals(auction.getTransferstatus(), body.getString("transferstatus"));
        assertEquals(auction.getTitle(), body.getString("title"));
        assertEquals(auction.getDescription(), body.getString("description"));
    }
}
