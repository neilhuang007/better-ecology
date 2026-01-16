// Category Links Reference
// Import this in your MDX files to use predefined category links

export const CATEGORY_LINKS = {
  PASSIVE_MOBS: {
    title: 'Passive Mobs',
    description: 'Overview of all passive animals and their behaviors',
    href: '/docs/wiki/animals'
  },
  PREDATORS: {
    title: 'Predators',
    description: 'Hunting behaviors and predator mechanics',
    href: '/docs/wiki/predators'
  },
  AQUATIC_MOBS: {
    title: 'Aquatic Mobs',
    description: 'Fish and aquatic animal behaviors',
    href: '/docs/wiki/aquatic'
  },
  BIRDS_FLYING: {
    title: 'Birds & Flying',
    description: 'Flight mechanics and avian behaviors',
    href: '/docs/wiki/birds'
  },
  HERDING_ANIMALS: {
    title: 'Herding Animals',
    description: 'Animals that form herds and move together',
    href: '/docs/wiki/herding'
  },
  PARENT_OFFSPRING: {
    title: 'Parent-Offspring Behaviors',
    description: 'Breeding, following, and family dynamics',
    href: '/docs/wiki/parenting'
  }
};

// Usage example in MDX:
// import { CATEGORY_LINKS } from '../../mdx-components'
//
// <RelatedPages pages={[
//   CATEGORY_LINKS.PASSIVE_MOBS,
//   CATEGORY_LINKS.HERDING_ANIMALS,
//   {
//     title: "Custom Link",
//     description: "Custom description",
//     href: "/custom/path"
//   }
// ]} />
