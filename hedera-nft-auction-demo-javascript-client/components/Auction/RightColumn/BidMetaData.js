import useCountdown from 'hooks/useCountdown'
import React from 'react'
import useHederaPrice from 'hooks/useHederaPrice'
import getUsdValue from 'utils/getUsdValue'
import getBidValue from 'utils/getBidValueToShow'
import BidButton from './BidButton'
import BidModal from 'components/modals/BidModal'
import HbarUnbit from 'components/common/HbarUnit'
import ShareModal from 'components/modals/ShareModal'

const BidMetaData = ({ auction }) => {
  const { currentPrice, isFetching: isFetchingHederaData } = useHederaPrice()
  const [isPlacingBid, setPlacingBidStatus] = React.useState(false)
  const [isSharingAuction, setSharingAuction] = React.useState(false)

  const openBidModal = () => setPlacingBidStatus(true)
  const closeBidModal = () => setPlacingBidStatus(false)

  const { endtimestamp, winningbid, minimumbid, reserve } = auction

  const timeLeft = useCountdown(endtimestamp)

  const bidToShow = getBidValue(winningbid)
  const usdValue = getUsdValue(bidToShow, currentPrice)

  const isOver = auction.status === 'ENDED'

  const openShareModal = () => setSharingAuction(true)
  const closeShareModal = () => setSharingAuction(false)

  const moreThanOneDayLeft = timeLeft.days >= 1
  const lessThanOneHourLeft = !timeLeft.days && timeLeft.hours < 1
  const lessThanMinutLeft =
    !timeLeft.days && !timeLeft.hours && timeLeft.minutes < 1

  const shouldShowDays = moreThanOneDayLeft
  const shouldShowHours = !lessThanOneHourLeft
  const shouldShowMinutes = !lessThanMinutLeft
  const shouldShowSeconds = !moreThanOneDayLeft

  if (isOver)
    return (
      <div className='flex sm:items-center items-left sm:flex-row flex-col'>
        <div>
          <p className='font-bold text-md'>Sold For</p>
          <p className='font-bold sm:text-3xl text-md'>
            <HbarUnbit large amountBold amount={bidToShow} />
          </p>
          <p className='text-gray-400 font-thin text-sm'>${usdValue}</p>
        </div>
        <p className='border-gradient border-gradient-purple text-sm py-1 px-3 sm:mt-3 mt-6 sm:ml-10 ml-0 relative bottom-1 text-xl font-light sm:text-left text-center'>
          This auction has <span className='font-bold'>ended</span>
        </p>
      </div>
    )

  return (
    <div className='flex justify-between sm:flex-row flex-col'>
      <div className='sm:mb-0 mb-8'>
        <p className='sm:mb-2 mb-0'>Current Bid</p>
        <p className='font-semibold sm:text-3xl text-lg pb-1.5 sm:mb-1 mb-0'>
          <HbarUnbit
            amount={bidToShow}
            className='sm:text-34 text-36 sm:font-bold font-light'
          />
        </p>
        <p className='text-17 relative bottom-2'>
          <span
            className='text-sm relative'
            style={{
              bottom: '1px',
            }}
          >
            $
          </span>
          {usdValue}
        </p>
      </div>
      <div className='sm:mb-0 mb-8'>
        <p className='font-semibold sm:mb-2 sm:mb-0 sm:m-0 -m-1'>Reserve</p>
        <div className='font-bold sm:text-3xl text-md '>
          <HbarUnbit
            className='sm:text-34 text-30 sm:font-bold font-light'
            italic
            amount={reserve}
          />
        </div>
      </div>
      <div>
        <p className='font-semibold sm:mb-2 mb-0'>Auction Ends</p>
        <div className='flex'>
          {!timeLeft ? (
            <p>Calculating...</p>
          ) : (
            <>
              {shouldShowDays && (
                <div className='mr-6 font-thin'>
                  <p
                    className='sm:text-34 text-30 sm:mb-1 mb-0'
                    style={{
                      lineHeight: '2.25rem',
                    }}
                  >
                    {timeLeft.days || 0}
                  </p>
                  <p className='font-light sm:mb-0 -m-0.5'>Days</p>
                </div>
              )}
              {shouldShowHours && (
                <div className='mr-6 font-thin'>
                  <p
                    className='sm:text-34 text-30 sm:mb-1 mb-0'
                    style={{
                      lineHeight: '2.25rem',
                    }}
                  >
                    {timeLeft.hours || 0}
                  </p>
                  <p className='font-light sm:mb-0 -m-0.5'>Hours</p>
                </div>
              )}
              {shouldShowMinutes && (
                <div className='mr-4 font-thin'>
                  <p
                    className='sm:text-34 text-30 sm:mb-1 mb-0'
                    style={{
                      lineHeight: '2.25rem',
                    }}
                  >
                    {timeLeft.minutes || 0}
                  </p>
                  <p className='font-light sm:mb-0 -m-0.5'>Minutes</p>
                </div>
              )}
              {shouldShowSeconds && (
                <div className='font-thin'>
                  <p
                    className='sm:text-34 text-30 sm:mb-1 mb-0'
                    style={{
                      lineHeight: '2.25rem',
                    }}
                  >
                    {timeLeft.seconds || 0}
                  </p>
                  <p className='font-light sm:mb-0 -m-0.5'>Seconds</p>
                </div>
              )}
            </>
          )}
        </div>
      </div>
      <div className='sm:ml-5 ml-0 sm:mt-0 mt-5 relative sm:bottom-6 bottom-0'>
        {/* fix this on desktop */}
        <div className='font-semibold mb-2 sm:block flex'>
          <p className='relative sm:top-2 top-0'>Minimum</p>
          <p className='sm:ml-0 ml-1'>Bid Increase</p>
        </div>
        <div className='font-bold sm:text-3xl text-sm'>
          <HbarUnbit
            className='sm:text-34 text-30 sm:font-bold font-light'
            italic
            amount={minimumbid}
            denomination='tinybar'
          />
        </div>
      </div>
      <div className='sm:hidden flex justify-between items-end '>
        <BidButton openBidModal={openBidModal} />
        <div
          className='flex flex-col items-end justify-center cursor-pointer'
          onClick={openShareModal}
        >
          <img
            src='/assets/share-icon.svg'
            className='h-5 w-5 relative'
            style={{ right: '11px' }}
          />
          <p className='font-light'>Share</p>
        </div>
      </div>
      <BidModal isOpen={isPlacingBid} close={closeBidModal} auction={auction} />
      <ShareModal isOpen={isSharingAuction} close={closeShareModal} />
    </div>
  )
}

export default BidMetaData
