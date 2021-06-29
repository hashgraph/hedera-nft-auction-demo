const FirstBidItem = ({ auctionaccountid, createdAt, reserve }) => (
  <div
    className={`mb-8 shadow-bid-item sm:h-16 h-full relative flex justify-between`}
  >
    <div className='bg-purple-gradient w-2 h-full absolute' />
    <div className='flex sm:flex-row flex-col sm:items-center items-left w-full justify-between sm:ml-5 ml-7'>
      <div className='sm:pb-0 pb-4'>
        <p className='font-light text-xs text-gray-400'>Listed by</p>
        <p className='font-bold text-sm'>{auctionaccountid}</p>
      </div>
      <div className='sm:pb-0 pb-4'>
        <p className='font-light text-xs text-gray-400'>Date created</p>
        <p className='font-bold text-sm'>{createdAt}</p>
      </div>
      <div className='flex items-center flex-col sm:pb-0 pb-3'>
        <p className='font-light text-xs text-gray-400'>Reserve</p>
        <p className='font-bold text-sm'>{reserve}</p>
      </div>
    </div>
  </div>
)

export default FirstBidItem
