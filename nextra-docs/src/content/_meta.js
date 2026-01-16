export default {
  index: {
    title: 'Home'
  },
  '-- player-wiki': {
    type: 'separator',
    title: 'Player Wiki'
  },
  wiki: {
    title: 'Wiki',
    type: 'menu',
    items: {
      index: {
        title: 'Welcome',
        href: '/docs/wiki'
      },
      animals: {
        title: 'Animals',
        href: '/docs/wiki/animals'
      },
      pathfinding: {
        title: 'How Animals Navigate',
        href: '/docs/wiki/pathfinding'
      }
    }
  },
  '---': {
    type: 'separator',
    title: 'Developer Docs'
  },
  'getting-started': 'Getting Started',
  animals: 'Animals',
  systems: 'Systems',
  research: 'Research',
  api: 'API',
  citations: 'Citations'
}
