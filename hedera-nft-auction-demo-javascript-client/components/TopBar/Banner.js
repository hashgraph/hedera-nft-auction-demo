import fetchEnvironment from 'utils/getEnvironment'
import React from 'react'

const UnderLine = ({ children, onClick }) => {
  const handleClick = () => {
    if (onClick) onClick()
  }
  return (
    <span
      onClick={handleClick}
      className='underline children-white cursor-pointer'
    >
      {children}
    </span>
  )
}

const Banner = () => {
  const [environment, setEnvironment] = React.useState()

  const asyncFetchEnv = async () => {
    const environment = await fetchEnvironment()
    setEnvironment(environment)
  }

  React.useEffect(() => {
    asyncFetchEnv()
  }, [])

  if (!environment) return null

  const goToHederaFAQ = () => window.open('https://help.hedera.com/', '_blank')
  const { network, nodeOperator, topicId, validators } = environment
  return (
    <div className='flex bg-purple-gradient items-center justify-between px-20 py-4'>
      <p className='font-thin text-sm'>
        This application is part of a Hedera Validator network using topic{' '}
        <UnderLine>{topicId}</UnderLine>. This{' '}
        <UnderLine>hedera.auction</UnderLine> instance is run by {nodeOperator}{' '}
        - other validators are run by:{' '}
        {validators.map((validator, index) => {
          const isLastItem = index === validators.length - 1
          const comma = isLastItem ? '' : ', '
          const goToPage = () => window.open(validator.url, '_blank')
          return (
            <React.Fragment key={index}>
              <UnderLine onClick={goToPage}>{validator.name}</UnderLine>
              {comma}
            </React.Fragment>
          )
        })}
        .
      </p>
      <button
        onClick={goToHederaFAQ}
        className='text-white border font-thin p-3 text-sm cursor-pointer'
      >
        How it works
      </button>
    </div>
  )
}

export default Banner
