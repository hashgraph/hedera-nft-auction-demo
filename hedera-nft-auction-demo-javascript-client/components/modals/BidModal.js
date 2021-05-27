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
        <div className='mt-6 flex'>
          <div className=''>
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
      <div className='mt-6'>
        <div className='mt-3 text-left sm:mt-0 w-full'>
          <h3 className='font-thin leading-6 text-sm'>Your bid in HBAR:</h3>
          <div className='mt-2 flex items-end'>
            <input
              type='number'
              className='border-b focus:outline-none px-3 py-1 text-3xl text-left text-white bg-transparent font-bold'
              style={{
                borderColor: '#4B68F1',
                width: '75%',
              }}
              value={bidAmount}
              onChange={handleBidAmountChange}
              autoFocus
            />
            <span className='text-3xl relative top-1'>ℏ</span>
            <button
              onClick={handleBidSend}
              className='bg-purple-gradient px-6 uppercase ml-5'
              style={{ paddingTop: '2px', paddingBottom: '2px' }}
              disabled={isSendingBid}
            >
              {isSendingBid ? '...' : 'Bid'}
            </button>
          </div>
          <div>
            <p className='mt-3 text-gray-400'>${usdValue}</p>
            <p
              className='font-thin my-3'
              style={{
                fontSize: '10px',
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
      <div className='pb-alot flex items-end justify-center pt-4 px-4 text-center'>
        <div
          className='fixed inset-0 transition-opacity'
          aria-hidden='true'
          style={{ touchAction: 'none' }}
          onClick={handleClose}
        >
          <div className='absolute inset-0 bg-black opacity-75'></div>
        </div>
        <div
          className='w-full inline-block align-bottom bg-white text-left overflow-hidden shadow-2xl transform transition-all sm:my-8 sm:align-middle sm:max-w-md sm:w-full'
          role='dialog'
          aria-modal='true'
          aria-labelledby='modal-headline'
        >
          <div
            className='bg-black px-4 pt-5 pb-4 sm:pb-4 w-full border-l-8'
            style={{
              borderColor: '#4B68F1',
            }}
          >
            <CloseIcon
              className='h-8 w-8 text-white absolute cursor-pointer'
              onClick={handleClose}
              style={{
                top: '.25rem',
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
