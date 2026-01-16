import { useMDXComponents as getDocsMDXComponents } from 'nextra-theme-docs'
import { HintBox } from './src/components/HintBox'
import { AnimalInfoBox } from './src/components/AnimalInfoBox'
import { RelatedPages, CATEGORY_LINKS } from './src/components/RelatedPages'

const docsComponents = getDocsMDXComponents()

export const useMDXComponents = components => ({
  ...docsComponents,
  HintBox,
  AnimalInfoBox,
  RelatedPages,
  ...components
})

export { CATEGORY_LINKS }
