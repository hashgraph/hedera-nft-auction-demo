import React from 'react'
import calculateTimeLeft from 'utils/calculateTimeLeft'
import { useRouter } from 'next/router'
import HbarUnbit from 'components/common/HbarUnit'
import getBidValue from 'utils/getBidValueToShow'
import useCountdown from 'hooks/useCountdown'
import fetchAuctionImage from 'utils/getAuctionImage'

const LiveAuctionCard = ({ auction, showStatus }) => {
  const router = useRouter()
  const {
    tokenid,
    winningbid,
    endtimestamp,
    id: auctionId,
    active,
    ended,
    status,
    description,
    title,
  } = auction

  const { days, hours, minutes, seconds } = useCountdown(endtimestamp)

  const moreThanOneDayLeft = days >= 1
  const lessThanOneDayLeft = !days && hours >= 1
  const lessThanOneHourLeft = !days && hours < 1
  const lessThanMinutLeft = !days && !hours && minutes < 1

  const goToAuctionPage = () => router.push(`/auction/${auctionId}`)

  const [auctionImage, setAuctionImage] = React.useState(null)

  React.useEffect(() => {
    const asyncFetchAuction = async () => {
      const auctionImage = await fetchAuctionImage(auction.tokenmetadata)  
      setAuctionImage(auctionImage.image.description)
    }
    if (auction) asyncFetchAuction()
  }, [auction])

  const getStatus = () => {
    if (!showStatus) return
    if (ended) return 'SOLD'
    if (status === 'CLOSED') return 'CLOSED'
  }

  const bidToShow = getBidValue(winningbid)

  const isSold = !active
 

  const getEndTime = () => {
    if (moreThanOneDayLeft)
      return (
        <>
          <span style={{ marginRight: '10px' }}>{days}d</span>
          <span>{hours}h</span>
        </>
      )
    if (lessThanOneDayLeft)
      return (
        <>
          <span style={{ marginRight: '10px' }}>{hours}h</span>
          <span>{minutes}m</span>
        </>
      )
    if (lessThanOneHourLeft)
      return (
        <>
          <span style={{ marginRight: '10px' }}>{minutes}m</span>
          <span>{seconds}s</span>
        </>
      )
    if (lessThanMinutLeft)
      return (
        <>
          <span style={{ marginRight: '10px' }}>{minutes}m</span>
          <span>{seconds}s</span>
        </>
      )
    return null
  }

  const endTimeToDisplay = getEndTime()

  const titleToRender = title || 'Title'

  const CardFooter = () => {
    if (isSold)
      return (
        <div className=''>
          <p className='text-card-subtitle'>Winning Bid</p>
          <p className='text-lg'>
            <HbarUnbit italic amount={bidToShow} card />
          </p>
        </div>
      )
    return (
      <>
        <div className=''>
          <p className='text-card-subtitle '>Current Bid</p>
          <p className='text-lg'>
            <HbarUnbit italic amount={bidToShow} card />
          </p>
        </div>
        <div style={{ marginRight: '8%' }}>
          <p className='text-card-subtitle'>Auction Ends</p>
          <p className='text-lg sm:mt-0.5 mt-0 font-light'>
            {endTimeToDisplay}
          </p>
        </div>
      </>
    )
  }

  const displayStatus = getStatus()

  return (
    <div className={`mb-10 cursor-pointer h-full`} onClick={goToAuctionPage}>
      <div className='flex flex-col h-full shadow-card'>
        {showStatus && (
          <p className='p-1 bg-purple-gradient absolute uppercase font-bold px-2 z-10'>
            {displayStatus}
          </p>
        )}
        <div className='flex flex-col h-full justify-between'>
          <div className='outer m-10 mb-0'>
            <img
              src={auctionImage}
              alt='live-auction-card'
              className='inner h-full w-full object-cover'
            />
          </div>
          <div className='flex flex-col p-2 px-4'>
            <p
              className='font-bold text-card-title mb-4'
              style={{ fontSize: '20px' }}
            >
              {titleToRender}
            </p>
            <p className='font-light text-card-tokenid'>
              Token I.D.: <span className='font-normal'>{tokenid}</span>
            </p>
          </div>
        </div>
        <div className='flex justify-between font-bold border-t border-indigo-600 pt-2 pb-4 px-4'>
          <CardFooter />
        </div>
      </div>
      <div className='bg-purple-gradient h-1 w-full' />
    </div>
  )
}

export default LiveAuctionCard
