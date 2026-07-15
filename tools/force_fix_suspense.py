import os
import re

for root, dirs, files in os.walk('frontend/src/app'):
    for f in files:
        if f == 'page.tsx':
            filepath = os.path.join(root, f)
            with open(filepath, 'r', encoding='utf-8') as file:
                content = file.read()
            
            # Match `return <ComponentName />` or `return <ComponentName prop="val" />`
            if '<Suspense' not in content and 'return <' in content:
                content = re.sub(
                    r'return <([A-Z][a-zA-Z0-9_]*)\s*(.*?)\s*/>;',
                    r'return (\n    <Suspense fallback={null}>\n      <\1 \2/>\n    </Suspense>\n  );',
                    content
                )
                # Ensure Suspense is imported
                if 'import { Suspense } from \'react\';' not in content:
                    content = "import { Suspense } from 'react';\n" + content
                
                with open(filepath, 'w', encoding='utf-8') as file:
                    file.write(content)
                print(f'Fixed {filepath}')
