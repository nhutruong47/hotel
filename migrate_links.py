import os, glob

def replace_in_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    new_content = content.replace("import { Link } from 'react-router-dom';", "import Link from 'next/link';")
    new_content = new_content.replace("import { Link, NavLink, useNavigate, useLocation } from 'react-router-dom';", "import Link from 'next/link';\nimport { useRouter, usePathname, useSearchParams } from 'next/navigation';")
    new_content = new_content.replace("import { Link, useNavigate, useSearchParams } from 'react-router-dom';", "import Link from 'next/link';\nimport { useRouter, useSearchParams } from 'next/navigation';")
    new_content = new_content.replace("import { useNavigate } from 'react-router-dom';", "import { useRouter } from 'next/navigation';")
    new_content = new_content.replace("import { useNavigate, useParams, useSearchParams } from 'react-router-dom';", "import { useRouter, useParams, useSearchParams } from 'next/navigation';")
    new_content = new_content.replace("import { Link, useParams } from 'react-router-dom';", "import Link from 'next/link';\nimport { useParams } from 'next/navigation';")
    new_content = new_content.replace("import { Outlet } from 'react-router-dom';", "")
    
    # Generic replacements for Link
    new_content = new_content.replace('<Link to="', '<Link href="')
    new_content = new_content.replace('<Link to={`', '<Link href={`')
    
    if content != new_content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f'Updated {filepath}')

for root, dirs, files in os.walk('frontend/src'):
    for file in files:
        if file.endswith('.tsx') or file.endswith('.ts'):
            replace_in_file(os.path.join(root, file))
