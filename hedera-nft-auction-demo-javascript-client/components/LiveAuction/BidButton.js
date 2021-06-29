import React from 'react'

const BidButton = ({ onClick }) => {
  return (
    <button
      onClick={onClick}
      className='bg-purple-gradient px-6 uppercase py-1 text-md'
    >
      Bid
    </button>
  )
}

export default BidButton
