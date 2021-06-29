import React from 'react'
import calculateTimeLeft from 'utils/calculateTimeLeft'

const useCountdown = timestamp => {
  const [timeLeft, setTimeLeft] = React.useState(calculateTimeLeft(timestamp))
  React.useEffect(() => {
    const interval = setInterval(() => {
      const timeLeft = calculateTimeLeft(timestamp)
      setTimeLeft(timeLeft)
    }, 1000)
    return () => clearInterval(interval)
  }, [])
  return timeLeft
}

export default useCountdown
