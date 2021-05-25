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

const BidItem = ({ bid, currentPrice }) => {
  const {
    timestamp,
    bidamount,
    bidderaccountid,
    transactionhash,
    transactionid,
  } = bid
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

  return (
    <div
      className='flex justify-between border-l-8 my-8 px-4 shadow-bid-item items-center'
      style={{
        height: '4rem',
        borderColor: '#5266F1',
      }}
    >
      <div>
        <p className='font-light text-sm text-gray-400'>Bidder</p>
        <p className='font-bold sm:text-md text-xs'>{bidderaccountid}</p>
      </div>
      <div>
        <p className='font-light text-sm text-gray-400'>Date Placed</p>
        <p className='font-bold sm:text-md text-xs'>{formattedTimestamp}</p>
      </div>
      <div className='flex items-center'>
        <div>
          <p className='font-bold text-xl mx-0'>
            {bidAmountToShow} <span className='font-light text-sm'>HBAR</span>
          </p>
          <p className='font-semibold text-sm mx-0 text-gray-400'>
            ${usdValue}
          </p>
        </div>
        <img
          src='/assets/view-transaction.svg'
          onClick={handleTransactoinViewClick}
          className='h-7 w-7 sm:ml-12 ml-2 cursor-pointer'
        />
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
    <div>
      <PurpleGradientBorder />
      <h1 className='font-bold text-lg p-2'>History</h1>
      <div className='p-2'>
        {bidHistory.map(bid => (
          <BidItem bid={bid} key={bid.timestamp} currentPrice={currentPrice} />
        ))}
      </div>
    </div>
  )
}

export default BidHistory
