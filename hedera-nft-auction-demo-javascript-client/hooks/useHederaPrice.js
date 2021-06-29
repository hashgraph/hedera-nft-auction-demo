import React from 'react'

const PRICING_URL = 'https://api.coingecko.com/api/v3/coins/hedera-hashgraph'

const useHederaPrice = () => {
  const [isFetching, setFetchingStatus] = React.useState(false)
  const [currentPrice, setCurrentPrice] = React.useState(null)

  React.useEffect(() => {
    setFetchingStatus(true)
    const asyncFetchHederaData = async () => {
      try {
        const hederaResponse = await fetch(PRICING_URL)
        const {
          market_data: { current_price, circulating_supply },
        } = await hederaResponse.json()
        setCurrentPrice(current_price.usd)
      } catch (error) {
        console.log('Error fetching hedera price data:', error)
      } finally {
        setFetchingStatus(false)
      }
    }
    asyncFetchHederaData()
  }, [])

  return { currentPrice, isFetching }
}

export default useHederaPrice
