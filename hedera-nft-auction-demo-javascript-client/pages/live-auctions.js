import React from 'react'
import AuctionCard from 'components/common/cards/AuctionCard'
import fethLiveAuctions from 'utils/getLiveAuctions'

const AllLiveAuctions = () => {
  const [liveAuctions, setAuctions] = React.useState([])

  React.useEffect(() => {
    const asyncAuctionsFetch = async () => {
      try {
        const liveAuctions = await fethLiveAuctions()
        setAuctions(liveAuctions)
      } catch (error) {
        console.log('Error fetching auctions', error)
      }
    }
    asyncAuctionsFetch()
  }, [])

  const noLiveAuctions = liveAuctions.length === 0

  if (noLiveAuctions)
    return (
      <div className='text-center font-thin'>
        <p>No active auctions</p>
      </div>
    )

  return (
    <div className=''>
      <div className='grid sm:grid-cols-2 lg:grid-cols-4 grid-rows-1 gap-10'>
        {liveAuctions.map(auction => {
          return <AuctionCard key={auction.id} auction={auction} />
        })}
      </div>
    </div>
  )
}

export default AllLiveAuctions
