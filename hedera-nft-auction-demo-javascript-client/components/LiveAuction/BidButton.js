import React from 'react'

const BidButton = ({ onClick }) => {
  return (
    <button
      onClick={onClick}
      className='bg-purple-gradient px-6 uppercase'
      style={{ paddingTop: '2px', paddingBottom: '2px' }}
    >
      Bid
    </button>
  )
}

export default BidButton
