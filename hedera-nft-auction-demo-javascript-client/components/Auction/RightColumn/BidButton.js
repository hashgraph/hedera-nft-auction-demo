import React from 'react'

// .5rem y 2rem

const BidButton = ({ openBidModal }) => {
  return (
    <button
      onClick={openBidModal}
      className='bg-purple-gradient uppercase mt-12 uppercase sm:py-1 py-2 sm:px-6 p-mobile-bid-button'
      style={{
        fontSize: '20px',
      }}
    >
      Bid
    </button>
  )
}

export default BidButton
