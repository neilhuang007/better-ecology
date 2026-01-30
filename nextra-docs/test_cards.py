from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page()
    page.goto('http://localhost:3000')
    page.wait_for_load_state('networkidle')

    # Take screenshot of the home page
    page.screenshot(path='home_screenshot.png', full_page=True)

    # Get the page content to inspect cards
    content = page.content()

    # Find cards section
    cards_section = page.locator('text=Getting Started').first
    if cards_section:
        print("Found 'Getting Started' section")

    # Look for Cards components
    cards = page.locator('[class*="card"], [class*="Card"]').all()
    print(f"Found {len(cards)} card elements")

    # Check for any elements with "Player Wiki" text
    player_wiki = page.locator('text=Player Wiki').all()
    print(f"Found {len(player_wiki)} 'Player Wiki' elements")

    # Check for any elements with "Developer Docs" text
    dev_docs = page.locator('text=Developer Docs').all()
    print(f"Found {len(dev_docs)} 'Developer Docs' elements")

    # Print all links on the page
    links = page.locator('a').all()
    print(f"\nAll links on page ({len(links)} total):")
    for link in links[:20]:  # First 20 links
        href = link.get_attribute('href')
        text = link.inner_text()[:50] if link.inner_text() else "(no text)"
        print(f"  - {text}: {href}")

    browser.close()
    print("\nScreenshot saved to home_screenshot.png")
