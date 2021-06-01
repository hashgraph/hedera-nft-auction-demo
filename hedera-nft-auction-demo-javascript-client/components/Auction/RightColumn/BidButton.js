import React from 'react'

const BidButton = ({ openBidModal }) => {
  return (
    <button
      onClick={openBidModal}
      className='bg-purple-gradient px-6 py-px uppercase font-bold my-12'
    >
      Bid
    </button>
  )
}

export default BidButton