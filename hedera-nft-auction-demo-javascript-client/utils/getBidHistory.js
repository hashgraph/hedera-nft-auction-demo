const fetchBidHistory = auctionId => {
  let endpoint = process.env.NEXT_PUBLIC_BASE_API_URL ? process.env.NEXT_PUBLIC_BASE_API_URL : window.location.protocol.concat("//").concat(window.location.hostname).concat(":8081");
  endpoint = endpoint + '/v1/bids/' + auctionId;
  return new Promise(async (resolve, reject) => {
    try {
      const historyResponse = await fetch(endpoint)
      const bidHistory = await historyResponse.json()
      resolve(bidHistory)
    } catch (error) {
      reject(error)
    }
  })
}

export default fetchBidHistory
