import { API_BASE_URL } from './constants'

const fetchLiveAutcions = () => {
  const endpoint = API_BASE_URL + `/activeauctions`
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

export default fetchLiveAutcions