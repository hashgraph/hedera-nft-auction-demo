import React from 'react'
import sendBid from 'utils/sendBid'
import getUsdValue from 'utils/getUsdValue'
import useHederaPrice from 'hooks/useHederaPrice'
import CloseIcon from './assets/close_icon.svg'

const Modal = ({ isOpen, close, auction }) => {
  const [bidAmount, setBidAmount] = React.useState(0)
  const [bidStatus, setBidStatus] = React.useState(null)
  const { currentPrice } = useHederaPrice()

  if (!isOpen) return null
  const { auctionaccountid: auctionAccountId } = auction

  const handleClose = () => {
    setBidAmount(0)
    setBidStatus(null)
    close()
  }

  const handleBidAmountChange = e => {
    const { value: bidAmount } = e.target
    setBidAmount(bidAmount)
  }

  const handleBidSend = async () => {
    setBidStatus('submitting')
    try {
      const result = await sendBid({ auctionAccountId, bid: bidAmount })
      setBidStatus('success')
    } catch (error) {
      setBidStatus('error')
    }
  }

  const usdValue = getUsdValue(bidAmount, currentPrice)

  const isSendingBid = bidStatus === 'submitting'
  const successfullySentBid = bidStatus === 'success'
  const errorSendingBid = bidStatus === 'error'

  const Content = () => {
    if (successfullySentBid)
      return (
        <div className='flex'>
          <div>
            <h3 className='font-thin leading-6 text-sm'>Your bid in HBAR:</h3>
            <div className='text-3xl'>
              <p>
                {bidAmount}
                <span className='relative'>ℏ</span>
              </p>
              <p className='font-thin text-gray-400 text-sm'>${usdValue}</p>
            </div>
          </div>
          <div className='font-thin flex uppercase items-center ml-3'>
            <img className='w-8' src='/assets/bid_success.svg' />
            <div>
              <p>Bid</p>
              <p>Submitted</p>
            </div>
          </div>
        </div>
      )
    return (
      <div>
        <div className='mt-5 text-left sm:mt-0 w-full'>
          <h3 className='font-thin leading-6 text-xs'>Your bid in HBAR:</h3>
          <div className='flex items-end'>
            <input
              type='number'
              className='border-b focus:outline-none px-3 text-3xl text-left text-white bg-transparent font-bold'
              style={{
                borderColor: '#4B68F1',
                width: '62%',
              }}
              value={bidAmount}
              onChange={handleBidAmountChange}
              autoFocus
            />
            <span className='text-3xl relative top-1 italic'>ℏ</span>
            <button
              onClick={handleBidSend}
              className='bg-purple-gradient px-9 py-1 uppercase ml-10 text-lg'
              // style={{ paddingTop: '2px', paddingBottom: '2px' }}
              disabled={isSendingBid}
            >
              {isSendingBid ? '...' : 'Bid'}
            </button>
          </div>
          <div>
            <p
              className='text-gray-400 font-thin text-sm relative py-1'
              style={{
                top: '5px',
              }}
            >
              ${usdValue}
            </p>
            <p
              className='font-thin my-3'
              style={{
                fontSize: '9px',
              }}
            >
              The specified amount of HBAR will be transferred to the auction
              account and refunded if your bud is too low or does not meet the
              bidding requirements
            </p>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className='fixed z-50 inset-0 overflow-y-none flex justify-center items-center'>
      <div
        className='flex items-end justify-center pt-4 px-4 text-center absolute'
        style={{
          top: '14.5rem',
        }}
      >
        <div
          className='fixed inset-0 transition-opacity'
          aria-hidden='true'
          style={{ touchAction: 'none' }}
          onClick={handleClose}
        >
          <div className='absolute inset-0 opacity-75 bg-black'></div>
        </div>
        {/* Modal Content */}
        <div
          className=' border-l-4 w-full inline-block align-bottom bg-black text-left overflow-hidden shadow-2xl transform transition-all sm:align-middle sm:max-w-md sm:w-full'
          role='dialog'
          aria-modal='true'
          aria-labelledby='modal-headline'
          style={{
            borderColor: '#4B68F1',
          }}
        >
          <div
            className='pl-8 pr-10 w-full'
            style={{
              position: 'relative',
              paddingTop: '3.5rem',
              bottom: '7px',
              paddingBottom: '1rem',
            }}
          >
            <CloseIcon
              className='h-8 w-8 text-white absolute cursor-pointer'
              onClick={handleClose}
              style={{
                top: '.75rem',
                right: '.25rem',
              }}
            />
            <Content />
          </div>
        </div>
      </div>
    </div>
  )
}

export default Modal
