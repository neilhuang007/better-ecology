from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={'width': 1400, 'height': 900})

    print("=" * 60)
    print("SMOKE TEST - betterecology.com")
    print("=" * 60)

    # Test 1: Home page - no emojis
    print("\nTest 1: Home page (no emojis)")
    page.goto('https://betterecology.com')
    page.wait_for_load_state('networkidle')

    page_html = page.content()

    # Check for common emojis
    import re
    emoji_pattern = re.compile(r'[\U0001F300-\U0001F9FF\u2600-\u26FF\u2700-\u27BF]')
    emojis = emoji_pattern.findall(page_html)

    if emojis:
        print(f"  [FAIL] Found {len(emojis)} emojis: {emojis[:5]}")
    else:
        print("  [PASS] No emojis found on home page")

    # Check cards exist without icons
    cards = page.locator('.home-card').all()
    print(f"  Home cards count: {len(cards)}")

    icons = page.locator('.home-card-icon').all()
    if len(icons) == 0:
        print("  [PASS] No card icons (emojis removed)")
    else:
        print(f"  [CHECK] Found {len(icons)} card icons")

    # Test 2: Wiki sidebar structure
    print("\nTest 2: Wiki sidebar")
    page.goto('https://betterecology.com/docs/wiki')
    page.wait_for_load_state('networkidle')

    sidebar = page.locator('aside').first
    if sidebar:
        sidebar_text = sidebar.inner_text()
        if "Player Wiki" in sidebar_text:
            print("  [PASS] Player Wiki section visible")
        else:
            print("  [FAIL] Player Wiki section not found")

        # Check for wiki subsections
        if "Welcome" in sidebar_text or "Animals" in sidebar_text:
            print("  [PASS] Wiki subsections visible in sidebar")
        else:
            print("  [CHECK] Wiki subsections may not be visible")

    # Test 3: Hint box styling (not uppercase)
    print("\nTest 3: Hint box labels (should not be uppercase)")
    page.goto('https://betterecology.com/docs/wiki/animals/cow')
    page.wait_for_load_state('networkidle')

    hint_labels = page.locator('.hint-box-label').all()
    if hint_labels:
        for label in hint_labels[:2]:
            text = label.inner_text()
            print(f"  Label text: '{text}'")
            # CSS uppercase would render as uppercase but inner_text gets the original
            if text and not text.isupper():
                print("  [PASS] Labels not forced uppercase in HTML")
    else:
        print("  [INFO] No hint box labels found on this page")

    page.screenshot(path='D:/projects/better-ecology/final_prod_test.png', full_page=True)
    print("\n  Screenshot saved: final_prod_test.png")

    browser.close()
    print("\n" + "=" * 60)
    print("SMOKE TEST COMPLETED")
    print("=" * 60)
