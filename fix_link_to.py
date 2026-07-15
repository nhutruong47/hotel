import os, glob, re

for root, dirs, files in os.walk('frontend/src'):
    for file in files:
        if file.endswith('.tsx') or file.endswith('.ts'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Use regex to find <Link ... to=...
            # But wait, it's simpler: just replace `to=` with `href=` inside `<Link` or just any `to=` if it's safe.
            # Let's replace `to="` with `href="` and `to={` with `href={` where there is whitespace before it.
            # Even better, regex: r'\bto=(["{])' -> r'href=\1'
            new_content = re.sub(r'\bto=(["{])', r'href=\1', content)
            
            if content != new_content:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                print(f'Fixed to= in {filepath}')
