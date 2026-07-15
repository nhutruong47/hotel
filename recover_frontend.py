import json
import os

transcript_path = r'C:\Users\TTN\.gemini\antigravity-ide\brain\cfa42463-3e84-4a6d-b2c8-650c2bbbced6\.system_generated\logs\transcript_full.jsonl'
frontend_dir = r'd:\d\1\hotelNew\frontend'

if not os.path.exists(frontend_dir):
    os.makedirs(frontend_dir)

with open(transcript_path, 'r', encoding='utf-8') as f:
    for line in f:
        try:
            step = json.loads(line)
            if step.get('type') == 'PLANNER_RESPONSE' and 'tool_calls' in step:
                for call in step['tool_calls']:
                    if call['name'] == 'write_to_file':
                        args = call['args']
                        target_file = args.get('TargetFile', '')
                        if 'frontend' in target_file.lower():
                            print(f'Recovering: {target_file}')
                            content = args.get('CodeContent', '')
                            # Create directories if they don't exist
                            os.makedirs(os.path.dirname(target_file), exist_ok=True)
                            with open(target_file, 'w', encoding='utf-8') as out_f:
                                out_f.write(content)
        except Exception as e:
            pass
