import os

files_to_update = [
    'frontend/src/features/profile/ProfilePages.tsx',
    'frontend/src/features/profile/ProfileSettingsPage.tsx',
    'frontend/src/features/booking/CheckoutPage.tsx',
    'frontend/src/features/booking/BookingPage.tsx',
    'frontend/src/features/auth/AuthPages.tsx',
    'frontend/src/features/booking/BookingSuccessPage.tsx',
    'frontend/src/features/content/NotFoundPage.tsx'
]

for filepath in files_to_update:
    if not os.path.exists(filepath):
        continue
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Add 'use client'
    if 'use client' not in content:
        content = "'use client';\n\n" + content
        
    # Replace useNavigate with useRouter
    content = content.replace('const navigate = useNavigate();', 'const router = useRouter();')
    
    # Replace navigate(xxx) with router.push(xxx) or replace(xxx)
    content = content.replace('navigate(', 'router.push(')
    content = content.replace('router.push(-1)', 'router.back()')
    content = content.replace(', { replace: true }', '')
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f'Updated {filepath}')
