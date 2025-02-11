const PROXY_CONFIG = [
  {
    context: ['/ws'],
    target: 'https://viacep.com.br/',
    secure: false,
    changeOrigin: true,
    pathRewrite: {
      '^/': '',
    },
  },
]

module.exports = PROXY_CONFIG
