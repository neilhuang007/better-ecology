import nextra from 'nextra'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

const withNextra = nextra({
  defaultShowCopyCode: true,
  contentDirBasePath: '/docs'
})

export default withNextra({
  output: 'export',
  images: {
    unoptimized: true
  },
  trailingSlash: true,
  turbopack: {
    root: __dirname
  }
})
