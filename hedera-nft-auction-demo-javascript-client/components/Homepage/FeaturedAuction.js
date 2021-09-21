import React from 'react'
import BidButton from './BidButton'
import ViewButton from './ViewButton'
import BidModal from 'components/modals/BidModal'
import useHederaPrice from 'hooks/useHederaPrice'
import getUsdValue from 'utils/getUsdValue'
import HbarUnbit from 'components/common/HbarUnit'
import useCountdown from 'hooks/useCountdown'
import getBidValue from 'utils/getBidValueToShow'
import fetchAuctionImage from 'utils/getAuctionImage'

const FeaturedAuction = ({ featuredAuction }) => {
  const [isPlacingBid, setBidStatus] = React.useState(false)
  const { currentPrice, isFetching: isFetchingHederaData } = useHederaPrice()

  const { endtimestamp, winningbid, minimumbid, reserve } = featuredAuction
  const timeLeft = useCountdown(endtimestamp)

  const [auctionImage, setAuctionImage] = React.useState(null)

  if (!featuredAuction) return null

  React.useEffect(() => {
    const asyncFetchAuction = async () => {
      const auctionImage = await fetchAuctionImage(featuredAuction.tokenmetadata)  
      setAuctionImage(auctionImage.image.description)
    }
    if (featuredAuction) asyncFetchAuction()
  }, [featuredAuction])

  const {
    tokenid: featuredTokenId,
    reserve: featuredReserve,
    id: auctionId,
    title,
  } = featuredAuction

  const openBidModal = () => setBidStatus(true)
  const closeBidModal = () => setBidStatus(false)
  const bidToShow = getBidValue(winningbid)

  const usdValue = getUsdValue(bidToShow, currentPrice)
  const featuredAuctionImage = auctionImage

  const titleToRender = title

  const TITLE_CHAR_LIMIT = 20
  const truncatedTitle = titleToRender.substring(0, TITLE_CHAR_LIMIT)

  const moreThanOneDayLeft = timeLeft.days >= 1
  const lessThanOneHourLeft = !timeLeft.days && timeLeft.hours < 1
  const lessThanMinutLeft =
    !timeLeft.days && !timeLeft.hours && timeLeft.minutes < 1

  const shouldShowDays = moreThanOneDayLeft
  const shouldShowHours = !lessThanOneHourLeft
  const shouldShowMinutes = !lessThanMinutLeft
  const shouldShowSeconds = !moreThanOneDayLeft

  return (
    <div className='flex sm:flex-row flex-col justify-center bg-black text-white pb-12'>
      <p
        className='mb-25 font-medium block sm:hidden'
        style={{
          fontSize: '22px',
        }}
      >
        Featured Auction
      </p>
      <div>
        <img
          className='sm:max-w-md w-full object-contain'
          src={featuredAuctionImage}
          alt='current-live-auction-item'
        />
      </div>
      <div className='sm:ml-10'>
        <p
          className='mb-12 font-light hidden sm:block'
          style={{
            fontSize: '22px',
          }}
        >
          Featured Auction
        </p>
        <p className='mb-4 sm:mt-0 mt-25 font-light text-md'>
          Token I.D.: <span className='font-normal'>{featuredTokenId}</span>
        </p>
        <p className='font-bold sm:text-4xl mb-10' style={{ fontSize: '38px' }}>
          {truncatedTitle}
        </p>
        <div>
          <div className='flex justify-between flex-col lg:flex-row'>
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
            <div className='ml-0 lg:ml-44 lg:absolute relative'>
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
                          {timeLeft.days}
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
                          {timeLeft.hours}
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
                          {timeLeft.minutes}
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
                          {timeLeft.seconds}
                        </p>
                        <p className='font-light sm:mb-0 -m-0.5'>Seconds</p>
                      </div>
                    )}
                  </>
                )}
              </div>
            </div>
          </div>
        </div>
        <div className='mt-12 flex'>
          <BidButton onClick={openBidModal} />
          <ViewButton auctionId={auctionId} />
        </div>
      </div>
      <BidModal
        isOpen={isPlacingBid}
        close={closeBidModal}
        auction={featuredAuction}
      />
    </div>
  )
}

export default FeaturedAuction
