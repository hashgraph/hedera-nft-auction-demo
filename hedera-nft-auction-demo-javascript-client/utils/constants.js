export const API_BASE_URL =
  process.env.NEXT_PUBLIC_BASE_API_URL ||
  window.location.protocol
    .concat('//')
    .concat(window.location.hostname)
    .concat(':8081/v1')
