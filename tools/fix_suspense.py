import os

for root, dirs, files in os.walk('frontend/src/app'):
    if 'page.tsx' in files:
        filepath = os.path.join(root, 'page.tsx')
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
            
        if 'Suspense' not in content:
            # We want to wrap the return value in Suspense
            # E.g. `return <AboutPage />;` -> `return <Suspense fallback={null}><AboutPage /></Suspense>;`
            # or `return ( <RequireAuth> ... </RequireAuth> );` -> `return ( <Suspense fallback={null}><RequireAuth> ... </RequireAuth></Suspense> );`
            
            # Simple approach: Replace `export default function Page() { return ` with `import { Suspense } from 'react';\n\nexport default function Page() { return <Suspense fallback={null}>`
            # and append `</Suspense>` before the semicolon or end of line.
            
            if 'export default function Page() {\n  return (\n    <RequireAuth>' in content:
                content = "import { Suspense } from 'react';\n" + content.replace(
                    '    </RequireAuth>\n  );',
                    '    </RequireAuth>\n    </Suspense>\n  );'
                ).replace(
                    '  return (\n    <RequireAuth>',
                    '  return (\n    <Suspense fallback={null}>\n    <RequireAuth>'
                )
            elif 'export default function Page() {\n  return <' in content:
                content = "import { Suspense } from 'react';\n" + content.replace(
                    '  return <',
                    '  return <Suspense fallback={null}><'
                ).replace(
                    ' />;\n}',
                    ' /></Suspense>;\n}'
                )
                
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f'Added Suspense to {filepath}')
