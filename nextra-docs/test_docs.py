"""
Playwright test script for Nextra documentation.
Tests that pages load correctly and formatting is proper.
"""
from playwright.sync_api import sync_playwright
import sys

def test_documentation():
    """Test all main documentation pages load correctly."""
    results = []

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()

        # Test pages to visit
        test_pages = [
            ('/', 'Introduction'),
            ('/getting-started/installation/', 'Installation'),
            ('/getting-started/configuration/', 'Configuration'),
            ('/animals/overview/', 'Animals Overview'),
            ('/animals/sheep/', 'Sheep'),
            ('/animals/cow/', 'Cow'),
            ('/animals/wolf/', 'Wolf'),
            ('/animals/fox/', 'Fox'),
            ('/animals/rabbit/', 'Rabbit'),
            ('/systems/architecture/', 'Architecture'),
            ('/research/overview/', 'Research Overview'),
            ('/research/herd-movement/', 'Herd Movement'),
            ('/research/flocking/', 'Flocking'),
            ('/api/overview/', 'API Overview'),
        ]

        base_url = 'http://localhost:3002'

        for path, name in test_pages:
            url = f'{base_url}{path}'
            try:
                page.goto(url, timeout=30000)
                page.wait_for_load_state('networkidle', timeout=30000)

                # Check for error indicators
                error_elements = page.locator('text=404').count()
                build_error = page.locator('text=Build Error').count()
                unhandled_error = page.locator('text=Unhandled Runtime Error').count()

                if error_elements > 0 or build_error > 0 or unhandled_error > 0:
                    results.append((name, 'FAIL', 'Page has errors'))
                else:
                    # Take screenshot for verification
                    screenshot_path = f'/tmp/nextra_{name.lower().replace(" ", "_")}.png'
                    page.screenshot(path=screenshot_path)
                    results.append((name, 'PASS', screenshot_path))

            except Exception as e:
                results.append((name, 'FAIL', str(e)))

        # Take full page screenshot of home
        page.goto(base_url)
        page.wait_for_load_state('networkidle', timeout=30000)
        page.screenshot(path='/tmp/nextra_home_full.png', full_page=True)

        browser.close()

    # Print results
    print('\n=== Nextra Documentation Test Results ===\n')
    passed = 0
    failed = 0
    for name, status, detail in results:
        if status == 'PASS':
            passed += 1
            print(f'[PASS] {name}')
        else:
            failed += 1
            print(f'[FAIL] {name}: {detail}')

    print(f'\nTotal: {passed} passed, {failed} failed')
    print('\nScreenshots saved to /tmp/nextra_*.png')

    return failed == 0

if __name__ == '__main__':
    success = test_documentation()
    sys.exit(0 if success else 1)
