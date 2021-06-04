import React from 'react'
import LeftColumn from './LeftColumn'
import RightColumn from './RightColumn'
import fetchAuction from 'utils/getAuction'
import { useRouter } from 'next/router'

const LEFT_COLUMN_WIDTH = '25%'
const RIGHT_COLUMN_WIDTH = '75%'

const AuctionView = () => {
  const router = useRouter()
  const { auctionId } = router.query
  const [auction, setAuction] = React.useState(null)

  React.useEffect(() => {
    const asyncFetchAution = async () => {
      const auction = await fetchAuction(auctionId)
      setAuction(auction)
    }
    if (auctionId) asyncFetchAution()
  }, [auctionId])

  if (!auction) return null

  return (
    <div className='flex sm:flex-row flex-col'>
      <div className='sm:mr-10 mr-0' style={{ flexBasis: LEFT_COLUMN_WIDTH }}>
        <LeftColumn auction={auction} />
      </div>
      <div className='' style={{ flexBasis: RIGHT_COLUMN_WIDTH }}>
        <RightColumn auction={auction} />
      </div>
    </div>
  )
}

export default AuctionView
