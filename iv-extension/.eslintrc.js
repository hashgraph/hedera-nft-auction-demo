module.exports = {
  root: true,
  env: {
    node: true,
    webextensions: true
  },
  'extends': [
    'plugin:vue/essential',
    'eslint:recommended'
  ],
  parserOptions: {
    parser: 'babel-eslint'
  },
  rules: {
    'no-console': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
    'no-debugger': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
    "no-unused-vars": "off",
    "no-empty": "off",
    "no-extra-semi":"off",
    "no-prototype-builtins":"off",
    "no-mixed-spaces-and-tabs":"off",
    "no-async-promise-executor":"off",
  }
}
