import { useRouter } from 'next/router'

const ViewButton = ({ auctionId }) => {
  const router = useRouter()
  const goToAuctionDetailPage = () => router.push(`/auction/${auctionId}`)
  return (
    <button
      onClick={goToAuctionDetailPage}
      className='cursor-pointer border-gradient border-gradient-purple px-4 uppercase ml-5 font-light py-1 text-md'
    >
      View
    </button>
  )
}

export default ViewButton
