import React from 'react'
import ViewAllButton from 'components/common/buttons/ViewAllButton'
import AuctionCard from 'components/common/cards/AuctionCard'
import fetchAuctions from 'utils/getAuctions'
import FeaturedAuction from './FeaturedAuction'
import { useRouter } from 'next/router'

const LiveAuction = () => {
  const router = useRouter()
  const [isViewingAllAuctions, setViewingAllAuctionsStatus] =
    React.useState(false)
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

  const hanldeViewAllLiveAuctionsClick = () => setViewingAllAuctionsStatus(true)
  const handleBackToAuctionClick = () => setViewingAllAuctionsStatus(false)

  if (!auctions) return null

  const liveAuctions = auctions.filter(
    auction => !auction.ended && auction.active
  )

  const soldAuctions = auctions.filter(
    auction => !auction.active && Boolean(auction.winningbid)
  )

  const getFeaturedAuction = () => {
    const doesHaveLiveAuctions = liveAuctions && liveAuctions.length > 0
    if (!doesHaveLiveAuctions) return null
    const getSeconds = timestamp => timestamp.substr(0, timestamp.indexOf('.'))
    // getting acution that expires the soonest
    return liveAuctions.reduce((auctionExpiringSoonest, nextAuction) => {
      const aSeconds = getSeconds(auctionExpiringSoonest.endtimestamp)
      const bSeconds = getSeconds(nextAuction.endtimestamp)
      if (aSeconds > bSeconds) {
        auctionExpiringSoonest = nextAuction
      }
      return auctionExpiringSoonest
    }, liveAuctions[0])
  }

  const featuredAuction = getFeaturedAuction()

  if (isViewingAllAuctions) {
    return (
      <div className=''>
        <div className='text-right'>
          <button
            onClick={handleBackToAuctionClick}
            className='border-gradient border-gradient-purple px-6 uppercase mr-4 mb-2 font-thin'
          >
            Back To Live Auction
          </button>
        </div>
        <div className='flex flex-wrap'>
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

  const handleViewAllSoldClick = () => router.push('/sold')

  // grabbing the most recent four live auctions
  const mostRecentLiveAuctions =
    liveAuctions.length > 4
      ? liveAuctions.slice(Math.max(liveAuctions.length - 4, 1))
      : liveAuctions

  // grabbing the most recent four sold auctions
  const mostRecentSoldAuctions =
    soldAuctions.length > 4
      ? soldAuctions.slice(Math.max(soldAuctions.length - 4, 1))
      : soldAuctions

  const noLiveAuctionsToShow = mostRecentLiveAuctions.length === 0
  const noSoldAuctionsToShow = mostRecentSoldAuctions.length === 0

  return (
    <div className=''>
      <FeaturedAuction featuredAuction={featuredAuction} />
      <div className='pb-12'>
        <div className='flex justify-between border-b border-indigo-500 py-2 mb-6'>
          <h1 className='text-md'>Live Auctions</h1>
          <ViewAllButton onClick={hanldeViewAllLiveAuctionsClick} />
        </div>
        <div className='flex flex-wrap'>
          {noLiveAuctionsToShow ? (
            <p className='font-thin'>No Live Auctions</p>
          ) : (
            mostRecentLiveAuctions.map((auction, index) => {
              const isLastItem = index === mostRecentLiveAuctions.length - 1
              return (
                <AuctionCard
                  key={auction.id}
                  auction={auction}
                  isLastItem={isLastItem}
                />
              )
            })
          )}
        </div>
      </div>
      <div>
        <div className='flex justify-between border-b border-indigo-500 py-2 mb-6'>
          <h1 className='text-md'>Sold</h1>
          <ViewAllButton onClick={handleViewAllSoldClick} />
        </div>
        <div className='flex justify-start flex-wrap'>
          {noSoldAuctionsToShow ? (
            <p className='font-thin'>No Sold Auctions</p>
          ) : (
            mostRecentSoldAuctions.map((auction, index) => {
              const isLastItem = index === mostRecentSoldAuctions.length - 1
              return (
                <AuctionCard
                  key={auction.id}
                  auction={auction}
                  showStatus
                  isLastItem={isLastItem}
                />
              )
            })
          )}
        </div>
      </div>
    </div>
  )
}

export default LiveAuction
