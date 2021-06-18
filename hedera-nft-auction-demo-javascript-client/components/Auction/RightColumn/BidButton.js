import React from 'react'

const BidButton = ({ openBidModal }) => {
  return (
    <button
      onClick={openBidModal}
      className='bg-purple-gradient px-6 uppercase my-12 px-6 uppercase py-1 text-md'
    >
      Bid
    </button>
  )
}

export default BidButton
