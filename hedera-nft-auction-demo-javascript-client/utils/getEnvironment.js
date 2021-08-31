import { API_BASE_URL } from './constants'

const fetchEnvironment = auctionId => {
  const endpoint = API_BASE_URL + '/environment'
  return new Promise(async (resolve, reject) => {
    try {
      const envResponse = await fetch(endpoint)
      const environemnt = await envResponse.json()
      resolve(environemnt)
    } catch (error) {
      reject(error)
    }
  })
}

export default fetchEnvironment