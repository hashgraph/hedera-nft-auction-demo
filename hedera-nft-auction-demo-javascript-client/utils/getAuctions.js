const fetchAuctions = () => {
  let endpoint = process.env.NEXT_PUBLIC_BASE_API_URL ? process.env.NEXT_PUBLIC_BASE_API_URL : window.location.protocol.concat("//").concat(window.location.hostname).concat(":8081");
  endpoint = endpoint + '/v1/auctions';
  console.log(endpoint);

  return new Promise(async (resolve, reject) => {
    try {
      const auctionResponse = await fetch(endpoint)
      const auctions = await auctionResponse.json()
      resolve(auctions)
    } catch (error) {
      reject(error)
    }
  })
}

export default fetchAuctions
