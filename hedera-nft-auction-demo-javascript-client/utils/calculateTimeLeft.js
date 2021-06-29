const calculateTimeLeft = timestamp => {
  const seconds = timestamp.substr(0, timestamp.indexOf('.'))
  const formattedDate = new Date(seconds * 1000)
  let difference = +new Date(formattedDate) - +new Date()
  let timeLeft = {}

  if (difference > 0) {
    timeLeft = {
      days: Math.floor(difference / (1000 * 60 * 60 * 24)),
      hours: Math.floor((difference / (1000 * 60 * 60)) % 24),
      minutes: Math.floor((difference / 1000 / 60) % 60),
      seconds: Math.floor((difference / 1000) % 60),
    }
  }

  return timeLeft
}

export default calculateTimeLeft
