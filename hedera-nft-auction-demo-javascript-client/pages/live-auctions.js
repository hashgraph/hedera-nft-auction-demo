import React from 'react'
import AuctionCard from 'components/common/cards/AuctionCard'
import fetchAuctions from 'utils/getAuctions'
import { useRouter } from 'next/router'

const AllLiveAuctions = () => {
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

  const liveAuctions = auctions.filter(
    auction => !auction.ended && auction.active
  )

  return (
    <div className=''>
      <div className='grid sm:grid-cols-4 grid-rows-1 gap-10'>
        {liveAuctions.map((auction, index) => {
          const isLastItem = index === liveAuctions.length - 1
          return (
            <AuctionCard
              key={auction.id}
              auction={auction}
              isLastItem={isLastItem}
            />
          )
        })}
      </div>
    </div>
  )
}

export default AllLiveAuctions
