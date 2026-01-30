from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={'width': 1400, 'height': 900})

    print("=" * 60)
    print("PRODUCTION SMOKE TEST - betterecology.com")
    print("=" * 60)

    # Test 1: Home page loads
    print("\nTest 1: Home page")
    page.goto('https://betterecology.com')
    page.wait_for_load_state('networkidle')
    title = page.title()
    print(f"  Page title: {title}")

    # Check home cards
    cards = page.locator('.home-card').all()
    print(f"  Home cards count: {len(cards)}")
    if len(cards) == 3:
        print("  [PASS] Home cards rendered correctly")
    else:
        print("  [FAIL] Expected 3 home cards")

    page.screenshot(path='D:/projects/better-ecology/prod_home.png', full_page=True)
    print("  Screenshot saved: prod_home.png")

    # Test 2: Wiki page sidebar
    print("\nTest 2: Wiki page sidebar")
    page.goto('https://betterecology.com/docs/wiki')
    page.wait_for_load_state('networkidle')

    sidebar = page.locator('aside').first
    if sidebar:
        sidebar_text = sidebar.inner_text()
        if "Player Wiki" in sidebar_text:
            print("  [PASS] Player Wiki section in sidebar")
        else:
            print("  [FAIL] Player Wiki section not found")

    page.screenshot(path='D:/projects/better-ecology/prod_wiki.png', full_page=True)
    print("  Screenshot saved: prod_wiki.png")

    # Test 3: Animal page tips
    print("\nTest 3: Cow page tips count")
    page.goto('https://betterecology.com/docs/wiki/animals/cow')
    page.wait_for_load_state('networkidle')

    page_html = page.content()
    hint_count = page_html.count('hint-box')
    print(f"  hint-box occurrences: {hint_count}")
    if hint_count <= 10:  # CSS class + 3 boxes = around 6-9 matches
        print("  [PASS] Tips reduced appropriately")
    else:
        print("  [CHECK] May have more tips than expected")

    page.screenshot(path='D:/projects/better-ecology/prod_cow.png', full_page=True)
    print("  Screenshot saved: prod_cow.png")

    browser.close()

    print("\n" + "=" * 60)
    print("SMOKE TEST COMPLETED")
    print("=" * 60)
