import React from 'react'
import LiveAuctionCard from 'components/common/cards/AuctionCard'
import fetchAuctions from 'utils/getAuctions'

const Sold = () => {
  const [auctions, setAuctions] = React.useState(null)

  React.useEffect(() => {
    const asyncAuctionsFetch = async () => {
      try {
        const auctions = await fetchAuctions()
        setAuctions(auctions)
      } catch (error) {
        console.log('Error fetching auctions', error)
      }
    }
    asyncAuctionsFetch()
  }, [])

  if (!auctions) return null

  const soldAuctions = auctions.filter(
    auction => !auction.active && Boolean(auction.winningbid)
  )

  const noSoldAuctions = soldAuctions.length === 0

  if (noSoldAuctions)
    return (
      <div className='text-center'>
        <p>No sold auctions</p>
      </div>
    )

  return (
    <div className='flex flex-wrap justify-start sm:p-section-desktop p-section-mobile'>
      {soldAuctions.map(auction => (
        <LiveAuctionCard key={auction.id} auction={auction} showStatus />
      ))}
    </div>
  )
}

export default Sold
