import { API_BASE_URL } from './constants'

const fetchSoldAuctions = () => {
  const endpoint = API_BASE_URL + `/soldauctions`
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

export default fetchSoldAuctions