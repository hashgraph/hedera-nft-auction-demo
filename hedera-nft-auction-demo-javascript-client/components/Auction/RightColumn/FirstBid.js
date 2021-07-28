import getUsdValue from 'utils/getUsdValue'
import useHederaPrice from 'hooks/useHederaPrice'
import fetchEnvironment from 'utils/getEnvironment'

const getIdForDragonglassExplorer = transactionid => {
  const hashWithoutPeriods = transactionid.split('.').join('')
  const hashWithoutSymbol = hashWithoutPeriods.split('@').join('')
  return hashWithoutSymbol.replace(/-/g, '')
}

const FirstBidItem = ({
  auctionaccountid,
  createdAt,
  reserve,
  isLive,
  transactionId,
}) => {
  const handleTransactoinViewClick = async () => {
    if (!transactionId) return
    const { network } = await fetchEnvironment()
    const isTestNet = network === 'testnet'
    const idForDragonGlass = getIdForDragonglassExplorer(transactionId)
    const subdomain = isTestNet ? 'testnet.' : ''
    const dragonGlassBaseUrl = `https://${subdomain}dragonglass.me/hedera/transactions/`
    window.open(dragonGlassBaseUrl + idForDragonGlass)
  }
  const { currentPrice, isFetching: isFetchingHederaData } = useHederaPrice()
  return (
    <div
      className={`mb-8 shadow-bid-item sm:h-16 h-full relative flex justify-between`}
    >
      {isLive && <div className='bg-purple-gradient w-2 h-full absolute' />}
      <div className='flex sm:flex-row flex-col sm:items-center items-left w-full justify-between sm:ml-5 ml-7 sm:mt-0 mt-3'>
        <div className='sm:pb-0 pb-4 w-1/4'>
          <p className='font-light text-xs text-gray-400 whitespace-nowrap'>
            Listing Transaction
          </p>
          <p className='font-bold text-sm'>{auctionaccountid}</p>
        </div>
        <div className='flex flex-grow justify-between sm:items-center items-left sm:flex-row flex-col '>
          <div className='sm:pb-0 pb-4 w-3/4'>
            <p className='font-light text-xs text-gray-400'>Date created</p>
            <p className='font-bold text-sm'>{createdAt}</p>
          </div>
          <div className='sm:pb-0 pb-3 w-1/2'>
            <p className='font-bold text-md mx-0'>
              {reserve} <span className='font-light text-md'>HBAR</span>
            </p>
            <p className='font-semibold text-xs mx-0 text-gray-400'>
              ${getUsdValue(reserve, currentPrice)}
            </p>
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

export default FirstBidItem
