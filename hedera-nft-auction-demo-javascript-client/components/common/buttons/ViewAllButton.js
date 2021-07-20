const ViewAllButton = ({ onClick }) => {
  return (
    <button
      onClick={onClick}
      className='border-gradient border-gradient-purple sm:px-2 px-1 uppercase sm:text-xs text-xxs sm:ml-8 ml-0 font-light'
    >
      View All
    </button>
  )
}

export default ViewAllButton
