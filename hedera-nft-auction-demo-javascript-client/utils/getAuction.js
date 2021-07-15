const fetchAuction = (auctionId) => {
  let endpoint = process.env.NEXT_PUBLIC_BASE_API_URL ? process.env.NEXT_PUBLIC_BASE_API_URL : window.location.protocol.concat("//").concat(window.location.hostname).concat(":8081");
  endpoint = endpoint  + `/v1/auctions/${auctionId}`;
  console.log(endpoint);
  return new Promise(async (resolve, reject) => {
    try {
      const auctionResponse = await fetch(endpoint)
      const auction = await auctionResponse.json()
      resolve(auction)
    } catch (error) {
      reject(error)
    }
  })
}

export default fetchAuction
