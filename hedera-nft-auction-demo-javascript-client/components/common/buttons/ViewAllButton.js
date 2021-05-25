const ViewAllButton = ({ onClick }) => {
  return (
    <button
      onClick={onClick}
      className='border-gradient border-gradient-purple px-6 uppercase ml-8 font-light'
    >
      View All
    </button>
  )
}

export default ViewAllButton
