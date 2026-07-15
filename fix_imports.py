import os

files_to_update = [
    'frontend/src/features/profile/ProfileSettingsPage.tsx',
    'frontend/src/features/profile/ProfilePages.tsx',
    'frontend/src/features/booking/CheckoutPage.tsx',
    'frontend/src/features/booking/BookingSuccessPage.tsx',
    'frontend/src/features/booking/BookingPage.tsx',
    'frontend/src/features/admin/AdminDashboardPage.tsx'
]

for filepath in files_to_update:
    if not os.path.exists(filepath):
        continue
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if 'AdminDashboardPage.tsx' in filepath and 'use client' not in content:
        content = "'use client';\n\n" + content
    
    content = content.replace("import { Link, useNavigate } from 'react-router-dom';", "import Link from 'next/link';\nimport { useRouter } from 'next/navigation';")
    content = content.replace("import { Link, useSearchParams } from 'react-router-dom';", "import Link from 'next/link';\nimport { useSearchParams } from 'next/navigation';")
    content = content.replace("import { Link, useNavigate, useParams } from 'react-router-dom';", "import Link from 'next/link';\nimport { useRouter, useParams } from 'next/navigation';")
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f'Updated {filepath}')
