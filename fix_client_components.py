import os

files_to_update = [
    'frontend/src/features/home/components/Hero.tsx',
    'frontend/src/shared/components/Reveal.tsx',
    'frontend/src/features/home/HomePage.tsx', # Just in case
]

for filepath in files_to_update:
    if not os.path.exists(filepath):
        continue
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if 'use client' not in content:
        content = "'use client';\n\n" + content
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f'Updated {filepath}')
