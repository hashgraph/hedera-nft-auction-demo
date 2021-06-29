#!/bin/sh

cd ..

sudo docker pull ghcr.io/hashgraph/hedera-nft-auction-demo/hedera_nft_auction_java_node:latest
sudo docker pull ghcr.io/hashgraph/hedera-nft-auction-demo/nft-auction-postgres:latest
sudo docker-compose rm -f
sudo docker-compose --profile image up --remove-orphans -d

echo "Starting log stream in 2s, CTRL+C to stop (will leave containers running)"

sleep 2
sudo docker-compose logs -f --tail=10
