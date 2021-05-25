import { API_BASE_URL } from './constants'

const fetchAuction = (auctionId) => {
  const endpoint = API_BASE_URL + `/auctions/${auctionId}`
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
