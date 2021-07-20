import { API_BASE_URL } from './constants'

const fetchBidHistory = auctionId => {
  const endpoint = API_BASE_URL + '/bids/' + auctionId
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
