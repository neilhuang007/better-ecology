import { Footer, Layout, Navbar } from 'nextra-theme-docs'
import { Head } from 'nextra/components'
import { getPageMap } from 'nextra/page-map'
import 'nextra-theme-docs/style.css'
import './globals.css'

export const metadata = {
  title: 'Better Ecology',
  description: 'A Minecraft Fabric mod implementing scientifically-based animal behaviors'
}

const navbar = (
  <Navbar
    logo={<span style={{ fontWeight: 700, fontSize: '1.2rem' }}>Better Ecology</span>}
    projectLink="https://github.com/neilhuang007/better-ecology"
  />
)

const footer = <Footer>MIT {new Date().getFullYear()} © Better Ecology.</Footer>

export default async function RootLayout({ children }) {
  const pageMap = await getPageMap()
  return (
    <html lang="en" dir="ltr" suppressHydrationWarning>
      <Head />
      <body>
        <Layout
          navbar={navbar}
          pageMap={pageMap}
          docsRepositoryBase="https://github.com/neilhuang007/better-ecology/tree/main/nextra-docs"
          footer={footer}
          sidebar={{ defaultMenuCollapseLevel: 1, toggleButton: true }}
          toc={{ backToTop: true }}
          editLink={<>Edit this page on GitHub</>}
        >
          {children}
        </Layout>
      </body>
    </html>
  )
}
