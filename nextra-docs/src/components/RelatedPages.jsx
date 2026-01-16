import Link from 'next/link';

// Predefined category links for common use cases
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

export function RelatedPages({ pages }) {
  if (!pages || pages.length === 0) {
    return null;
  }

  return (
    <div className="related-pages-container">
      <h3 className="related-pages-title">Related Pages</h3>
      <div className="related-pages-grid">
        {pages.map((page, index) => (
          <Link
            key={index}
            href={page.href || page.path || '#'}
            className="related-page-card"
          >
            <div className="related-page-content">
              <h4 className="related-page-card-title">{page.title || page.name}</h4>
              <p className="related-page-card-description">{page.description || ''}</p>
            </div>
            <div className="related-page-arrow">→</div>
          </Link>
        ))}
      </div>
    </div>
  );
}
