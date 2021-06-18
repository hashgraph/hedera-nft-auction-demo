import React from 'react'
import BidButton from './BidButton'
import ViewButton from './ViewButton'
import BidModal from 'components/modals/BidModal'
import useHederaPrice from 'hooks/useHederaPrice'
import getUsdValue from 'utils/getUsdValue'
import HbarUnbit from 'components/common/HbarUnit'

const FeaturedAuction = ({ featuredAuction }) => {
  const [isPlacingBid, setBidStatus] = React.useState(false)
  const { currentPrice, isFetching: isFetchingHederaData } = useHederaPrice()

  if (!featuredAuction) return null
  const {
    tokenid: featuredTokenId,
    reserve: featuredReserve,
    id: auctionId,
    tokenimage,
    title,
  } = featuredAuction

  const openBidModal = () => setBidStatus(true)
  const closeBidModal = () => setBidStatus(false)

  const usdValue = getUsdValue(featuredReserve, currentPrice)
  const featuredAuctionImage = tokenimage || '/assets/default-token-image.png'

  const titleToRender = title || 'A Doge Moment'

  const TITLE_CHAR_LIMIT = 20
  const truncatedTitle = titleToRender.substring(0, TITLE_CHAR_LIMIT)

  return (
    <div className='flex sm:flex-row flex-col justify-center bg-black text-white pb-12'>
      <img
        className='sm:w-4/12 w-full object-cover'
        src={featuredAuctionImage}
        alt='current-live-auction-item'
      />
      <div className='sm:ml-10'>
        <p className='mb-12 font-light text-lg'>Featured Auction</p>
        <p className='mb-4 font-light text-md'>
          Token I.D.: <span className='font-normal'>{featuredTokenId}</span>
        </p>
        <p className='font-bold sm:text-4xl text-4xl mb-10'>{truncatedTitle}</p>
        <div>
          <p className='mb-2 text-md'>Reserve Price</p>
          <p className='text-2xl mb-1'>
            <HbarUnbit italic amount={featuredReserve} />
          </p>
          <p className='text-gray-400 font-semibold text-md'>${usdValue}</p>
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
