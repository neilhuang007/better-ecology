# RelatedPages Component

A clean card-based component for linking between related documentation pages.

## Features

- Responsive grid layout (automatically adjusts from 1-3 columns based on screen size)
- Hover effects with smooth transitions
- Dark mode support
- Predefined category links for common use cases
- Arrow indicator that animates on hover

## Usage

### Basic Usage

```jsx
<RelatedPages pages={[
  {
    title: "Sheep",
    description: "Learn about flock behavior and grazing patterns",
    href: "/docs/wiki/animals/sheep"
  },
  {
    title: "Passive Mobs",
    description: "Overview of passive animals",
    href: "/docs/wiki/animals"
  },
  {
    title: "Herding Behavior",
    description: "How animals form groups",
    href: "/docs/research/herd-movement"
  }
]} />
```

### Using Predefined Category Links

The component exports `CATEGORY_LINKS` with predefined links for common categories:

```jsx
import { CATEGORY_LINKS } from '../../mdx-components'

<RelatedPages pages={[
  CATEGORY_LINKS.PASSIVE_MOBS,
  CATEGORY_LINKS.HERDING_ANIMALS,
  {
    title: "Custom Link",
    description: "Custom description",
    href: "/custom/path"
  }
]} />
```

### Available Category Links

- `CATEGORY_LINKS.PASSIVE_MOBS` - Overview of all passive animals
- `CATEGORY_LINKS.PREDATORS` - Hunting behaviors and predator mechanics
- `CATEGORY_LINKS.AQUATIC_MOBS` - Fish and aquatic animal behaviors
- `CATEGORY_LINKS.BIRDS_FLYING` - Flight mechanics and avian behaviors
- `CATEGORY_LINKS.HERDING_ANIMALS` - Animals that form herds
- `CATEGORY_LINKS.PARENT_OFFSPRING` - Breeding and family dynamics

## Props

### pages (required)

An array of page objects. Each object should have:

- `title` (string, required) - The page title displayed in the card
- `description` (string, required) - Brief description of the page content
- `href` (string, required) - The relative or absolute path to the page

## Styling

The component uses CSS classes defined in `globals.css`:

- `.related-pages-container` - Main container with gradient background
- `.related-pages-title` - "Related Pages" heading
- `.related-pages-grid` - Responsive grid layout
- `.related-page-card` - Individual card with hover effects
- `.related-page-arrow` - Animated arrow indicator

All styles include dark mode support via `@media (prefers-color-scheme: dark)`.

## Example Output

The component renders a visually appealing grid of cards with:
- Clean white background (dark gray in dark mode)
- Red accent color matching Better Ecology branding
- Subtle hover animation (lift + shadow)
- Right-pointing arrow that slides on hover
- Responsive layout that stacks on mobile devices
