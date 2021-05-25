import { useRouter } from 'next/router'

const ViewButton = ({ auctionId }) => {
  const router = useRouter()
  const goToAuctionDetailPage = () => router.push(`/auction/${auctionId}`)
  return (
    <button
      onClick={goToAuctionDetailPage}
      className='cursor-pointer border-gradient border-gradient-purple px-6 uppercase ml-8 font-light'
    >
      View
    </button>
  )
}

export default ViewButton
