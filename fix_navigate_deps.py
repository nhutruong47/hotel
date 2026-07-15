import os

files_to_update = [
    'frontend/src/features/profile/ProfileSettingsPage.tsx',
    'frontend/src/features/profile/ProfilePages.tsx',
]

for filepath in files_to_update:
    if not os.path.exists(filepath):
        continue
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    content = content.replace('navigate]', 'router]')
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f'Updated {filepath}')
