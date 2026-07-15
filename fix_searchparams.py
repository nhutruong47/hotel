import os

files_to_update = [
    'frontend/src/features/villas/VillasPage.tsx',
    'frontend/src/features/booking/BookingSuccessPage.tsx',
    'frontend/src/features/auth/AuthPages.tsx'
]

for filepath in files_to_update:
    if not os.path.exists(filepath):
        continue
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    content = content.replace('const [searchParams] = useSearchParams();', 'const searchParams = useSearchParams();')
    content = content.replace('const [params] = useSearchParams();', 'const params = useSearchParams();')
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f'Updated {filepath}')
