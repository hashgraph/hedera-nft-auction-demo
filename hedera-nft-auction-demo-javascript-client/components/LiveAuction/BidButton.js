import React from 'react'

const BidButton = ({ onClick }) => {
  return (
    <button
      onClick={onClick}
      className='bg-purple-gradient px-5 uppercase py-0 text-sm'
    >
      Bid
    </button>
  )
}

export default BidButton
