import calculateTimeLeft from 'utils/calculateTimeLeft'
import { Hbar } from '@hashgraph/sdk'
import { useRouter } from 'next/router'
import HbarUnbit from 'components/common/HbarUnit'

const MINUTES_IN_A_DAY = 1440
const MINUTES_IN_AN_HOUR = 60

const LiveAuctionCard = ({ auction, showStatus, isLastItem }) => {
  const router = useRouter()
  const {
    tokenid,
    winningbid,
    endtimestamp,
    id: auctionId,
    tokenimage,
    active,
    status,
    description,
    title,
  } = auction

  const { days, hours, minutes, seconds } = calculateTimeLeft(endtimestamp)
  const goToAuctionPage = () => router.push(`/auction/${auctionId}`)

  const endTimeisGreaterThanTwoDays = days >= 2
  console.log('endTimeisGreaterThanTwoDays', endTimeisGreaterThanTwoDays)

  const isSold = !active
  const auctionImage = tokenimage || '/assets/default-token-image.png'

  const getEndTime = () => {
    if (endTimeisGreaterThanTwoDays) return `${days}D ${hours}H`
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
    return `${totalMinutesToShow}M ${seconds}S`
  }

  const endTimeToDisplay = getEndTime()

  const titleToRender = title || 'Title'

  const CardFooter = () => {
    if (isSold)
      return (
        <div className='p-2'>
          <p className='text-card-subtitle'>Winning Bid</p>
          <p className='text-card-units'>
            <HbarUnbit italic amount={winningbid} />
          </p>
        </div>
      )
    return (
      <>
        <div className='p-2'>
          <p className='text-card-subtitle '>Current Bid</p>
          <p className='text-lg'>
            <HbarUnbit italic amount={winningbid} />
          </p>
        </div>
        <div className='p-2'>
          <p className='text-card-subtitle'>Auction ends</p>
          <p className='text-card-units mt-1'>{endTimeToDisplay}</p>
        </div>
      </>
    )
  }

  const marginRightClass = isLastItem ? 'mr-0' : 'mr-10'

  return (
    <div
      className={`sm:${marginRightClass} mr-0 mb-4 cursor-pointer sm:h-card h-full sm:w-card-small w-full`}
      onClick={goToAuctionPage}
    >
      <div className='flex flex-col h-full shadow-card'>
        {showStatus && (
          <p className='p-1 bg-purple-gradient absolute uppercase font-bold px-2'>
            SOLD
          </p>
        )}
        <div className='flex flex-col h-full justify-between'>
          <div
            className='flex justify-center items-center'
            style={{ flexBasis: '75%' }}
          >
            <img
              src={auctionImage}
              alt='live-auction-card'
              className='sm:py-0 py-3 max-h-40'
            />
          </div>
          <div className='flex flex-col p-2 px-4'>
            <p className='font-bold text-card-title mb-4'>{titleToRender}</p>
            <p className='font-light mb-2 text-card-tokenid'>
              Token I.D.: <span className='font-normal'>{tokenid}</span>
            </p>
          </div>
        </div>
        <div className='flex justify-between font-bold border-t border-indigo-600 py-4 px-4'>
          <CardFooter />
        </div>
      </div>
      <div className='bg-purple-gradient h-1 w-full' />
    </div>
  )
}

export default LiveAuctionCard
