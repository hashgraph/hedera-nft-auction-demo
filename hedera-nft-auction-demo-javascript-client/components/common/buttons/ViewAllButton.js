const ViewAllButton = ({ onClick }) => {
  return (
    <button
      onClick={onClick}
      className='border-gradient border-gradient-purple px-2 uppercase text-xs sm:ml-8 ml-0 font-light'
    >
      View All
    </button>
  )
}

export default ViewAllButton
