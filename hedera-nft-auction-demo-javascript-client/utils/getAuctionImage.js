const fetchAuctionImage = (auctionImageUrl) => {
  const endpoint = auctionImageUrl
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

export default fetchAuctionImage
