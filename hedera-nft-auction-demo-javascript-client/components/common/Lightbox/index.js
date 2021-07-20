import CloseIcon from 'components/modals/assets/close_icon.svg'

const Lightbox = ({ close, image }) => {
  return (
    <div
      className='fixed top-0 left-0 pin h-screen w-screen z-50 overflow-auto bg-black pin bg-opacity-75 flex justify-center items-center'
      onClick={close}
    >
      <div className='relative bg-black p-2 rounded shadow-card sm:h-90 h-auto'>
        <CloseIcon
          className='h-8 w-8 text-white absolute cursor-pointer'
          onClick={close}
          style={{
            top: '10px',
            right: '8px'
          }}
        />
        <img className='sm:h-full h-auto' src={image} />
      </div>
    </div>
  )
}

export default Lightbox
