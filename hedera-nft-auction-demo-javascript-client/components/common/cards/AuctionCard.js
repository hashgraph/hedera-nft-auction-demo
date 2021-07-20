import calculateTimeLeft from 'utils/calculateTimeLeft'
import { useRouter } from 'next/router'
import HbarUnbit from 'components/common/HbarUnit'
import getBidValue from 'utils/getBidValueToShow'

const MINUTES_IN_A_DAY = 1440
const MINUTES_IN_AN_HOUR = 60

const LiveAuctionCard = ({
  auction,
  showStatus,
  isLastItem,
}) => {
  const router = useRouter()
  const {
    tokenid,
    winningbid,
    endtimestamp,
    id: auctionId,
    tokenimage,
    active,
    ended,
    status,
    description,
    title,
  } = auction

  const { days, hours, minutes, seconds } = calculateTimeLeft(endtimestamp)
  const goToAuctionPage = () => router.push(`/auction/${auctionId}`)

  const getStatus = () => {
    if (!showStatus) return
    if (ended) return 'SOLD'
    if (status === 'CLOSED') return 'CLOSED'
  }

  const bidToShow = getBidValue(winningbid)

  const endTimeisGreaterThanTwoDays = days >= 2

  const isSold = !active
  const auctionImage = tokenimage || '/assets/default-token-image.png'

  const getEndTime = () => {
    if (endTimeisGreaterThanTwoDays)
      return (
        <>
          <span style={{ marginRight: '10px' }}>{days}d</span>
          <span>{hours}h</span>
        </>
      )
    const getTotalMinutes = () => {
      let totalMinutes = minutes
      if (days >= 1) {
        totalMinutes =
          days * MINUTES_IN_A_DAY + hours * MINUTES_IN_AN_HOUR + minutes
      } else if (hours >= 1) {
        totalMinutes = hours * MINUTES_IN_AN_HOUR + minutes
      }
      return totalMinutes
    }
    const totalMinutesToShow = getTotalMinutes()
    return (
      <>
        <span style={{ marginRight: '10px' }}>{totalMinutesToShow}m</span>
        <span>{seconds}s</span>
      </>
    )
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
          <p className='sm:text-card-units text-20 sm:mt-1 mt-0 font-light '>
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
          <div className='outer m-2'>
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
