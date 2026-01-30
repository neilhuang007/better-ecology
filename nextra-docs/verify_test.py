from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={'width': 1400, 'height': 900})

    print("=" * 60)
    print("TEST 1: Home Page Cards")
    print("=" * 60)
    page.goto('http://localhost:3000')
    page.wait_for_load_state('networkidle')

    # Check for home cards
    cards = page.locator('.home-card').all()
    print(f"Number of home cards: {len(cards)}")
    for i, card in enumerate(cards):
        title = card.locator('.home-card-title').text_content()
        print(f"  Card {i+1}: {title}")

    print()
    print("=" * 60)
    print("TEST 2: Wiki Page Sidebar")
    print("=" * 60)
    page.goto('http://localhost:3000/docs/wiki')
    page.wait_for_load_state('networkidle')

    # Get sidebar content
    sidebar = page.locator('aside').first
    if sidebar:
        sidebar_text = sidebar.inner_text()
        # Look for Player Wiki text in sidebar
        if "Player Wiki" in sidebar_text:
            print("[OK] Found 'Player Wiki' section in sidebar")
        else:
            print("[MISSING] 'Player Wiki' section NOT found in sidebar")

        # Print sidebar structure
        print("\nSidebar contents:")
        lines = sidebar_text.split('\n')
        for line in lines[:30]:  # First 30 lines
            if line.strip():
                print(f"  {line.strip()}")
    else:
        print("Sidebar not found, trying nav element...")
        nav = page.locator('nav').all()
        for n in nav:
            print(n.inner_text()[:200])

    print()
    print("=" * 60)
    print("TEST 3: Cow Page - Hint Box Count")
    print("=" * 60)
    page.goto('http://localhost:3000/docs/wiki/animals/cow')
    page.wait_for_load_state('networkidle')

    # Get the page HTML to check for HintBox components
    page_html = page.content()

    # Count HintBox occurrences in rendered HTML
    hint_count = page_html.count('hint-box')
    callout_count = page_html.count('callout')

    print(f"hint-box classes in HTML: {hint_count}")
    print(f"callout classes in HTML: {callout_count}")

    # Get main content text
    main_content = page.locator('main article').first
    if main_content:
        content_text = main_content.inner_text()
        lines = content_text.split('\n')
        tip_lines = [l for l in lines if 'tip' in l.lower() or 'did you know' in l.lower()]
        print(f"\nLines with tips/did you know: {len(tip_lines)}")
        for line in tip_lines[:5]:
            print(f"  - {line[:80]}...")

    # Take screenshot
    page.screenshot(path='D:/projects/better-ecology/cow_verify.png', full_page=True)
    print("\nCow page screenshot saved")

    browser.close()
    print("\n" + "=" * 60)
    print("All verification tests completed!")
    print("=" * 60)
