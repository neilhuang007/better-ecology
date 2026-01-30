from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={'width': 1400, 'height': 900})
    page.goto('http://localhost:3000/docs')
    page.wait_for_load_state('networkidle')

    # Take full page screenshot
    page.screenshot(path='D:/projects/better-ecology/nextra-docs/screenshot_home.png', full_page=True)
    print("Full screenshot saved")

    # Get the sidebar HTML
    sidebar = page.locator('nav').first
    if sidebar:
        sidebar_html = sidebar.inner_html()
        with open('D:/projects/better-ecology/nextra-docs/sidebar_dump.html', 'w', encoding='utf-8') as f:
            f.write(sidebar_html)
        print("Sidebar HTML saved")

    # Get Cards section HTML
    cards = page.locator('.nextra-cards, [class*="Cards"]').first
    if cards:
        cards_html = cards.inner_html()
        with open('D:/projects/better-ecology/nextra-docs/cards_dump.html', 'w', encoding='utf-8') as f:
            f.write(cards_html)
        print("Cards HTML saved")

    # Also get the main content area
    main_content = page.locator('main').first
    if main_content:
        main_html = main_content.inner_html()
        with open('D:/projects/better-ecology/nextra-docs/main_dump.html', 'w', encoding='utf-8') as f:
            f.write(main_html)
        print("Main content HTML saved")

    browser.close()
