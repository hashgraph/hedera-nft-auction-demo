package com.hedera.demo.auction.node.app.auctionendtransfer;

import com.hedera.demo.auction.node.app.domain.Auction;

public interface AuctionEndTransferInterface {
    AbstractAuctionEndTransfer.TransferResult checkTransferInProgress(Auction auction);
}
