const Lightbox = ({ close, image }) => {
  return (
    <div className='fixed top-0 left-0 pin h-screen w-screen z-50 overflow-auto bg-black pin bg-opacity-75 flex justify-center items-center'>
      <div className='relative bg-black p-2 rounded shadow-card'>
        <p className='absolute right-6 cursor-pointer font-thin' onClick={close}>
          X
        </p>
        <img src={image} />
      </div>
    </div>
  )
}

export default Lightbox
