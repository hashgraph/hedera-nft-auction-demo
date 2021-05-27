import useCountdown from 'hooks/useCountdown'
import React from 'react'
import useHederaPrice from 'hooks/useHederaPrice'
import getUsdValue from 'utils/getUsdValue'
import getBidValue from 'utils/getBidValueToShow'
import BidButton from './BidButton'
import BidModal from 'components/modals/BidModal'
import { Hbar } from '@hashgraph/sdk'

const BidMetaData = ({ auction }) => {
  const { currentPrice, isFetching: isFetchingHederaData } = useHederaPrice()
  const [isPlacingBid, setPlacingBidStatus] = React.useState(false)

  const openBidModal = () => setPlacingBidStatus(true)
  const closeBidModal = () => setPlacingBidStatus(false)

  const { endtimestamp, winningbid, minimumbid, reserve } = auction

  const timeLeft = useCountdown(endtimestamp)

  const bidToShow = getBidValue(winningbid)
  const usdValue = getUsdValue(bidToShow, currentPrice)

  const isOver = !auction.active && Boolean(auction.winningbid)

  if (isOver)
    return (
      <div className='flex justify-between flex-row items-center'>
        <div>
          <p className='font-bold text-md'>Sold For</p>
          <p className='font-bold sm:text-3xl text-md'>
            {Hbar.from(bidToShow).toString()}
          </p>
          <p className='text-gray-400 font-thin text-sm'>${usdValue}</p>
        </div>
        <p
          className='border-gradient border-gradient-purple font-thin text-sm p-1 mt-3'
          style={{ fontSize: '12px' }}
        >
          This auction is <span className='font-bold'>Closed</span>
        </p>
      </div>
    )

  return (
    <div className='flex justify-between sm:flex-row flex-col p-3'>
      <div className='sm:mb-0 mb-8'>
        <p className='sm:font-semibold font-semibold sm:mb-2 mb-0'>
          Current Bid
        </p>
        <p className='font-semibold sm:text-3xl text-lg'>
          {Hbar.from(bidToShow).toString()}
        </p>
        <p className='text-gray-400 sm:font-semibold sm:text-md text-sm'>
          ${usdValue}
        </p>
      </div>
      <div className='sm:mb-0 mb-8'>
        <p className='font-semibold sm:mb-2 mb-0'>Auction Ends</p>
        <div className='flex'>
          {!timeLeft ? (
            <p>Calculating...</p>
          ) : (
            <>
              <div className='mr-6 font-thin'>
                <p className='sm:text-3xl text-md'>{timeLeft.hours}</p>
                <p className='font-light'>Hours</p>
              </div>
              <div className='mr-4 font-thin'>
                <p className='sm:text-3xl text-md'>{timeLeft.minutes}</p>
                <p className='font-light'>Minutes</p>
              </div>
              <div className='font-thin'>
                <p className='sm:text-3xl text-md'>{timeLeft.seconds}</p>
                <p className='font-light'>Seconds</p>
              </div>
            </>
          )}
        </div>
      </div>
      <div>
        <p className='font-semibold mb-2'>Reserve</p>
        <p className='font-bold sm:text-3xl text-md'>
          {Hbar.from(reserve || 1).toString()}
        </p>
      </div>
      <div className='sm:ml-5 ml-0 sm:mt-0 mt-5 relative sm:bottom-6 bottom-0'>
        <div className='font-semibold mb-2'>
          <p>Minimum</p>
          <p>Bid Increase</p>
        </div>
        <p className='font-bold sm:text-3xl text-sm'>
          {Hbar.from(minimumbid).toString()}
        </p>
      </div>
      <div className='block sm:hidden'>
        <BidButton openBidModal={openBidModal} />
      </div>
      <BidModal isOpen={isPlacingBid} close={closeBidModal} auction={auction} />
    </div>
  )
}

export default BidMetaData
