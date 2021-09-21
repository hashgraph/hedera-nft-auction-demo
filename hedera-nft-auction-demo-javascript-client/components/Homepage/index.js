import React from 'react'
import ViewAllButton from 'components/common/buttons/ViewAllButton'
import AuctionCard from 'components/common/cards/AuctionCard'
import fetchLiveAuctions from 'utils/getLiveAuctions'
import fetchSoldAuctions from 'utils/getSoldAuctions'
import FeaturedAuction from './FeaturedAuction'
import { useRouter } from 'next/router'

const LiveAuction = () => {
  const router = useRouter()

  const [liveAuctions, setLiveAuctions] = React.useState([])
  const [soldAuctions, setSoldAuctions] = React.useState([])

  React.useEffect(() => {
    const asyncAuctionsFetch = async () => {
      try {
        const liveAuctions = await fetchLiveAuctions()
        setLiveAuctions(liveAuctions)
        const soldAuctions = await fetchSoldAuctions()
        setSoldAuctions(soldAuctions)
      } catch (error) {
        console.log('Error fetching auctions', error)
      }
    }
    asyncAuctionsFetch()
  }, [])

  const hanldeViewAllLiveAuctionsClick = () => router.push('/live-auctions')

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

  const handleViewAllSoldClick = () => router.push('/sold')
  const getMostRecentAuctions = auctions =>
    auctions.length > 4
      ? auctions.slice(Math.max(auctions.length - 4, 1))
      : auctions

  // grabbing the most recent four live auctions
  const mostRecentLiveAuctions = getMostRecentAuctions(liveAuctions)
  // grabbing the most recent four sold auctions
  const mostRecentSoldAuctions = getMostRecentAuctions(soldAuctions)

  const noLiveAuctionsToShow = mostRecentLiveAuctions.length === 0
  const noSoldAuctionsToShow = mostRecentSoldAuctions.length === 0

  return (
    <div className=''>
      {featuredAuction && <FeaturedAuction featuredAuction={featuredAuction} />}
      <div className='pb-24'>
        <div className='flex justify-between border-b border-indigo-500 py-2 mb-6'>
          <h1
            className='text-lg relative'
            style={{
              top: '7px',
              fontSize: '1.375rem',
              marginBottom: '2px',
            }}
          >
            Live Auctions
          </h1>
          <ViewAllButton onClick={hanldeViewAllLiveAuctionsClick} />
        </div>
        <div
          className={`grid sm:grid-cols-2 lg:grid-cols-4 grid-rows-1 gap-10`}
        >
          {noLiveAuctionsToShow ? (
            <p className='font-thin'>No Live Auctions</p>
          ) : (
            mostRecentLiveAuctions.map(auction => {
              return <AuctionCard key={auction.id} auction={auction} />
            })
          )}
        </div>
      </div>
      <div>
        <div className='flex justify-between border-b border-indigo-500 py-2 mb-6'>
          <h1
            className='text-lg relative'
            style={{
              top: '7px',
              fontSize: '1.375rem',
              marginBottom: '2px',
            }}
          >
            Sold
          </h1>
          <ViewAllButton onClick={handleViewAllSoldClick} />
        </div>
        <div
          className={`grid sm:grid-cols-2 lg:grid-cols-4 grid-rows-1 gap-10`}
        >
          {noSoldAuctionsToShow ? (
            <p className='font-thin'>No Sold Auctions</p>
          ) : (
            mostRecentSoldAuctions.map(auction => {
              return (
                <AuctionCard key={auction.id} auction={auction} showStatus />
              )
            })
          )}
        </div>
      </div>
    </div>
  )
}

export default LiveAuction

