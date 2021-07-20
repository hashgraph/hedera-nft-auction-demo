import { API_BASE_URL } from './constants'

const fetchAuctions = () => {
  console.log('process.env.NEXT_PUBLIC_BASE_API_URL', process.env.NEXT_PUBLIC_BASE_API_URL)
  console.log('API_BASE_URL', API_BASE_URL)
  const endpoint = API_BASE_URL + '/auctions'
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
