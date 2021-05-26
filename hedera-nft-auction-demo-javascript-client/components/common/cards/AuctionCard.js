import calculateTimeLeft from 'utils/calculateTimeLeft'
import { Hbar } from '@hashgraph/sdk'
import { useRouter } from 'next/router'
import {
  BrowserView,
  MobileView,
  isBrowser,
  isMobile,
} from 'react-device-detect'

const LiveAuctionCard = ({ auction, showStatus }) => {
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

  const isSold = !active
  const auctionImage = tokenimage || 'assets/default-token-image.jpg'

  const getEndTime = () => {
    if (days > 0) return `${days}D ${hours}H ${minutes}M ${seconds}S`
    if (hours > 0) return `${hours}H ${minutes}M ${seconds}S`
    if (minutes > 0) return `${minutes}M ${seconds}S`
    return `${seconds}S`
  }

  const endTimeToDisplay = getEndTime()

  const titleToRender = title || 'Title'

  const CardFooter = () => {
    if (isSold)
      return (
        <div className='p-2'>
          <p className='text-card-subtitle'>Winning Bid</p>
          <p className='text-card-units'>
            {Hbar.fromTinybars(winningbid).toString()}
          </p>
        </div>
      )
    return (
      <>
        <div className='p-2'>
          <p className='text-card-subtitle'>Current Bid</p>
          <p className='text-card-units'>
            {Hbar.fromTinybars(winningbid).toString()}
          </p>
        </div>
        <div className='p-2'>
          <p className='text-card-subtitle'>Auction ends</p>
          <p className='text-card-units'>{endTimeToDisplay}</p>
        </div>
      </>
    )
  }

  return (
    <div
      className='sm:mr-10 mr-0 mb-4 cursor-pointer sm:h-card h-full sm:w-card-small w-full'
      onClick={goToAuctionPage}
    >
      <div className='flex flex-col h-full shadow-card'>
        {showStatus && (
          <p className='p-1 bg-purple-gradient absolute uppercase font-bold'>
            {status}
          </p>
        )}
        <div className='flex flex-col h-full justify-between'>
          <div
            className='flex justify-center items-center'
            style={{ flexBasis: '75%' }}
          >
            <img src={auctionImage} alt='live-auction-card' className='sm:py-0 py-3' />
          </div>
          <div className='flex flex-col p-2'>
            <p className='font-bold text-card-title mb-4'>{titleToRender}</p>
            <p className='font-light mb-2 text-card-tokenid'>
              Token I.D.: <span className='font-normal'>{tokenid}</span>
            </p>
          </div>
        </div>
        <div className='flex justify-between font-bold border-t border-indigo-600 py-4'>
          <CardFooter />
        </div>
      </div>
      <div className='bg-purple-gradient h-1 w-full' />
    </div>
  )
}

export default LiveAuctionCard
