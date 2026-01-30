from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={'width': 1400, 'height': 900})

    # Test home page
    page.goto('http://localhost:3000')
    page.wait_for_load_state('networkidle')
    page.screenshot(path='D:/projects/better-ecology/home_test.png', full_page=True)
    print("Home page screenshot saved")

    # Test wiki page to check sidebar structure
    page.goto('http://localhost:3000/docs/wiki')
    page.wait_for_load_state('networkidle')
    page.screenshot(path='D:/projects/better-ecology/wiki_test.png', full_page=True)
    print("Wiki page screenshot saved")

    # Test a specific animal page to verify reduced tips
    page.goto('http://localhost:3000/docs/wiki/cow')
    page.wait_for_load_state('networkidle')
    page.screenshot(path='D:/projects/better-ecology/cow_test.png', full_page=True)
    print("Cow page screenshot saved")

    # Count hint boxes on cow page
    hint_boxes = page.locator('.hint-box').count()
    print(f"Hint boxes on cow page: {hint_boxes}")

    browser.close()
    print("All tests completed successfully!")
