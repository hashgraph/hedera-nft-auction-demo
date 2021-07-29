import React from 'react'
import AuctionCard from 'components/common/cards/AuctionCard'
import fetchSoldAuctions from 'utils/getSoldAuctions'

const Sold = () => {
  const [soldAuctions, setAuctions] = React.useState([])

  React.useEffect(() => {
    const asyncAuctionsFetch = async () => {
      try {
        const soldAuctions = await fetchSoldAuctions()
        setAuctions(soldAuctions)
      } catch (error) {
        console.log('Error fetching auctions', error)
      }
    }
    asyncAuctionsFetch()
  }, [])

  if (!soldAuctions) return null

  const noSoldAuctions = soldAuctions.length === 0

  if (noSoldAuctions)
    return (
      <div className='text-center font-thin'>
        <p>No sold auctions</p>
      </div>
    )

  return (
    <div className='grid sm:grid-cols-2 lg:grid-cols-4 grid-rows-1 gap-10'>
      {soldAuctions.map(auction => (
        <AuctionCard key={auction.id} auction={auction} showStatus />
      ))}
    </div>
  )
}

export default Sold
