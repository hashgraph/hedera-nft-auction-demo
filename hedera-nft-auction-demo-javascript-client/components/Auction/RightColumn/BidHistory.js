import fetchBidHistory from 'utils/getBidHistory'
import React from 'react'
import { useQuery } from 'react-query'
import useHederaPrice from 'hooks/useHederaPrice'
import getUsdValue from 'utils/getUsdValue'
import getBidValue from 'utils/getBidValueToShow'
import fetchEnvironment from 'utils/getEnvironment'
import PurpleGradientBorder from 'components/common/border/PurpleGradientBorder'
import FirstBidItem from './FirstBid'
import dayjs from 'dayjs'
var localizedFormat = require('dayjs/plugin/localizedFormat')
dayjs.extend(localizedFormat)

const MESSAGE_FETCH_INTERVAL = 1000

const getFormattedTime = timestamp => {
  const seconds = timestamp.substr(0, timestamp.indexOf('.'))
  return dayjs(new Date(seconds * 1000)).format('LLL')
}

const BidItem = ({ bid, currentPrice, isWinner, isEnded, isLive }) => {
  const { timestamp, bidamount, bidderaccountid, transactionid } = bid
  const formattedTimestamp = getFormattedTime(timestamp)

  const bidAmountToShow = getBidValue(bidamount)
  const usdValue = getUsdValue(bidAmountToShow, currentPrice)

  const getIdForDragonglassExplorer = () => {
    const hashWithoutPeriods = transactionid.split('.').join('')
    return hashWithoutPeriods.replace(/-/g, '')
  }

  const handleTransactoinViewClick = async () => {
    const { network } = await fetchEnvironment()
    const isTestNet = network === 'testnet'
    const idForDragonGlass = getIdForDragonglassExplorer()
    const subdomain = isTestNet ? 'testnet.' : ''
    const dragonGlassBaseUrl = `https://${subdomain}dragonglass.me/hedera/transactions/`
    window.open(dragonGlassBaseUrl + idForDragonGlass)
  }

  const getMarginClass = () => 'mb-8'

  const marginClass = getMarginClass()

  const showPurpleBar = isLive || (isEnded && isWinner)

  return (
    <div
      className={`${marginClass} shadow-bid-item sm:h-16 h-full relative flex justify-between`}
    >
      {showPurpleBar && (
        <div className='bg-purple-gradient w-2 h-full absolute' />
      )}
      <div className='flex sm:flex-row flex-col sm:items-center items-left w-full justify-between sm:ml-5 ml-7 sm:mt-0 mt-3'>
        <div className='sm:pb-0 pb-4 w-1/4'>
          <p className='font-light text-xs text-gray-400'>Bidder</p>
          <p className='font-bold text-sm'>{bidderaccountid}</p>
        </div>
        <div className='flex flex-grow justify-between sm:items-center items-baseline sm:flex-row flex-col'>
          <div className='sm:pb-0 pb-4 w-3/4'>
            <p className='font-light text-xs text-gray-400'>Date placed</p>
            <p className='font-bold text-sm'>{formattedTimestamp}</p>
          </div>
          <div className='flex items-center sm:pb-0 pb-3 w-1/2'>
            <div className='mr-4'>
              <p className='font-bold text-md mx-0'>
                {bidAmountToShow}{' '}
                <span className='font-light text-md'>HBAR</span>
              </p>
              <p className='font-semibold text-xs mx-0 text-gray-400'>
                ${usdValue}
              </p>
            </div>
          </div>
          <img
            src='/assets/view-transaction.svg'
            onClick={handleTransactoinViewClick}
            className='h-6 w-6 sm:ml-12 ml-2 cursor-pointer sm:relative absolute top-1 right-3 sm:mt-0 mt-3'
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

  const { winningtxid } = auction

  const {
    data: bidHistory,
    status,
    error,
    isLoading,
  } = useQuery(`bidHistory/${auctionId}`, asyncHIstoryFetch, {
    refetchInterval: MESSAGE_FETCH_INTERVAL,
    enabled: Boolean(auctionId),
  })

  const {
    auctionaccountid,
    starttimestamp: createdAtTimestamp,
    reserve,
    status: auctionStatus,
    createauctiontxid,
  } = auction

  const isEnded = auctionStatus === 'ENDED'
  const isLive = auctionStatus === 'ACTIVE'
  const formattedCreatedTime = getFormattedTime(createdAtTimestamp)

  if (isLoading) return <p>Loading...</p>
  if (!bidHistory) return <p>Error fetching Bid History</p>

  const hasNoBids = bidHistory.length === 0

  if (hasNoBids)
    return (
      <div className='relative'>
        <PurpleGradientBorder />
        <h1 className='font-bold text-lg pt-2 pb-6'>
          History{' '}
          <span className='text-xs font-thin'>
            (limited to the last 50 transactions)
          </span>
        </h1>
        <div className='p-1 '>
          <FirstBidItem
            auctionaccountid={auctionaccountid}
            createdAt={formattedCreatedTime}
            reserve={reserve}
            isLive={isLive}
            transactionId={createauctiontxid}
          />
        </div>
      </div>
    )

  return (
    <div className='relative'>
      <PurpleGradientBorder />
      <h1 className='font-bold text-lg pt-2 pb-6'>
        History{' '}
        <span className='text-xs font-thin'>
          (limited to the last 50 transactions)
        </span>
      </h1>

      <div className='p-1 '>
        {bidHistory.map((bid, index) => {
          const isWinner = bid.transactionid === winningtxid
          const isLastItem = index === bidHistory.length - 1
          return (
            <BidItem
              bid={bid}
              key={bid.timestamp}
              currentPrice={currentPrice}
              isLastItem={isLastItem}
              isWinner={isWinner}
              isEnded={isEnded}
              isLive={isLive}
            />
          )
        })}
        <FirstBidItem
          auctionaccountid={auctionaccountid}
          createdAt={formattedCreatedTime}
          reserve={reserve}
        />
      </div>
    </div>
  )
}

export default BidHistory
