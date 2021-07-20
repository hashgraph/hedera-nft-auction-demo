import getUsdValue from 'utils/getUsdValue'
import useHederaPrice from 'hooks/useHederaPrice'

const FirstBidItem = ({ auctionaccountid, createdAt, reserve }) => {
  const handleTransactoinViewClick = async () => {
    const { network } = await fetchEnvironment()
    const isTestNet = network === 'testnet'
    const idForDragonGlass = getIdForDragonglassExplorer()
    const subdomain = isTestNet ? 'testnet.' : ''
    const dragonGlassBaseUrl = `https://${subdomain}dragonglass.me/hedera/transactions/`
    window.open(dragonGlassBaseUrl + idForDragonGlass)
  }
  const { currentPrice, isFetching: isFetchingHederaData } = useHederaPrice()
  return (
    <div
      className={`mb-8 shadow-bid-item sm:h-16 h-full relative flex justify-between`}
    >
      <div className='flex sm:flex-row flex-col sm:items-center items-left w-full justify-between sm:ml-5 ml-7'>
        <div className='sm:pb-0 pb-4 w-1/4'>
          <p className='font-light text-xs text-gray-400'>Listed by</p>
          <p className='font-bold text-sm'>{auctionaccountid}</p>
        </div>
        <div className='flex flex-grow justify-between items-center'>
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
            // onClick={handleTransactoinViewClick}
            className='h-6 w-6 sm:ml-12 ml-2 cursor-pointer sm:relative absolute top-1 right-3'
          />
        </div>
      </div>
    </div>
  )
}

export default FirstBidItem
