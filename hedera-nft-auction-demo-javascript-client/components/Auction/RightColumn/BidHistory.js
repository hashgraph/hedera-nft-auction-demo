import fetchBidHistory from 'utils/getBidHistory'
import React from 'react'
import { useQuery } from 'react-query'
import useHederaPrice from 'hooks/useHederaPrice'
import getUsdValue from 'utils/getUsdValue'
import getBidValue from 'utils/getBidValueToShow'
import PurpleGradientBorder from 'components/common/border/PurpleGradientBorder'
import dayjs from 'dayjs'
var localizedFormat = require('dayjs/plugin/localizedFormat')
dayjs.extend(localizedFormat)

const MESSAGE_FETCH_INTERVAL = 1000

const getFormattedTime = timestamp => {
  const seconds = timestamp.substr(0, timestamp.indexOf('.'))
  return dayjs(new Date(seconds * 1000)).format('LLL')
}

const BidItem = ({ bid, currentPrice, isFirstItem, isLastItem }) => {
  const { timestamp, bidamount, bidderaccountid, transactionid } = bid
  const formattedTimestamp = getFormattedTime(timestamp)

  const bidAmountToShow = getBidValue(bidamount)
  const usdValue = getUsdValue(bidAmountToShow, currentPrice)

  const getIdForDragonglassExplorer = () => {
    const hashWithoutPeriods = transactionid.split('.').join('')
    return hashWithoutPeriods.replace(/-/g, '')
  }

  const handleTransactoinViewClick = () => {
    const idForDragonGlass = getIdForDragonglassExplorer()
    const dragonGlassBaseUrl =
      'https://testnet.dragonglass.me/hedera/transactions/'
    window.open(dragonGlassBaseUrl + idForDragonGlass)
  }

  const getMarginClass = () => {
    if (isFirstItem) return 'mb-8'
    if (isLastItem) return 'mt-8'
    return 'my-8'
  }

  const marginClass = getMarginClass()

  return (
    <div
      className={`${marginClass} shadow-bid-item sm:h-16 h-full relative flex justify-between`}
    >
      <div className='bg-purple-gradient w-2 h-full absolute' />
      <div className='flex sm:flex-row flex-col sm:items-center items-left w-full justify-between sm:ml-5 ml-7'>
        <div className='sm:pb-0 pb-4'>
          <p className='font-light sm:text-base text-sm text-gray-400'>
            Bidder
          </p>
          <p className='font-bold sm:text-sm text-xs'>{bidderaccountid}</p>
        </div>
        <div className='sm:pb-0 pb-4'>
          <p className='font-light sm:text-base text-sm text-gray-400'>
            Date Placed
          </p>
          <p className='font-bold sm:text-sm text-xs'>{formattedTimestamp}</p>
        </div>
        <div className='flex items-center sm:pb-0 pb-3'>
          <div>
            <p className='font-bold text-md mx-0'>
              {bidAmountToShow} <span className='font-light text-md'>HBAR</span>
            </p>
            <p className='font-semibold text-xs mx-0 text-gray-400'>
              ${usdValue}
            </p>
          </div>
          <img
            src='/assets/view-transaction.svg'
            onClick={handleTransactoinViewClick}
            className='h-6 w-6 sm:ml-12 ml-2 cursor-pointer sm:relative absolute top-1 right-3'
          />
        </div>
      </div>
    </div>
  )
}

const BidHistory = ({ auction }) => {
  const auctionId = auction?.id
  const { currentPrice, isFetching: isFetchingHederaData } = useHederaPrice()

  const asyncHIstoryFetch = async () => {
    try {
      const history = await fetchBidHistory(auctionId)
      return history
    } catch (error) {
      console.log('Error fetching history', error)
    }
  }

  const {
    data: bidHistory,
    status,
    error,
    isLoading,
  } = useQuery(`bidHistory/${auctionId}`, asyncHIstoryFetch, {
    refetchInterval: MESSAGE_FETCH_INTERVAL,
    enabled: Boolean(auctionId),
  })

  if (isLoading) return <p>Loading...</p>
  if (!bidHistory) return <p>Error fetching Bid History</p>

  const hasNoBids = bidHistory.length === 0

  if (hasNoBids) return <p>No Bids To Show</p>

  return (
    <div className='relative'>
      <PurpleGradientBorder />
      <h1 className='font-bold text-lg pt-2 pb-6'>History</h1>
      <div className='p-1 '>
        {bidHistory.map((bid, index) => {
          const isFirstItem = index === 0
          const isLastItem = index === bidHistory.length - 1
          return (
            <BidItem
              bid={bid}
              key={bid.timestamp}
              currentPrice={currentPrice}
              isFirstItem={isFirstItem}
              isLastItem={isLastItem}
            />
          )
        })}
      </div>
    </div>
  )
}

export default BidHistory
