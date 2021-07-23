import { API_BASE_URL } from './constants'

const fetchEndedAuctions = () => {
  const endpoint = API_BASE_URL + `/endedauctions`
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

export default fetchEndedAuctions