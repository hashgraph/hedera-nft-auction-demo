#!/bin/sh

if [ $# -eq 0 ]
  then
    echo "argument for server url required. e.g. https://serverIp:8081/v1"
fi

cd ..

sudo docker pull ghcr.io/hashgraph/hedera-nft-auction-demo/hedera_nft_auction_java_node:latest
sudo docker pull ghcr.io/hashgraph/hedera-nft-auction-demo/nft-auction-postgres:latest
sudo docker-compose rm -f
sudo docker-compose --profile image build --build-arg URL="$1"
sudo docker-compose --profile image up -d

echo "Starting log stream in 2s, CTRL+C to stop (will leave containers running)"

sleep 2
sudo docker-compose logs -f --tail=10
