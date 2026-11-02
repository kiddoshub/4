#!/usr/bin/env python3
"""
Fix Common Errors in Art Archive System
Improves robustness and handles common issues
"""

import os
import json
import sys
from pathlib import Path

class ErrorFixer:
    def __init__(self, base_path='/workspace/app'):
        self.base_path = Path(base_path)
        self.fixes_applied = []
        
    def fix_file_permissions(self):
        """Fix file permissions issues"""
        try:
            for root, dirs, files in os.walk(self.base_path):
                for f in files:
                    if f.endswith(('.py', '.sh', '.js')):
                        path = os.path.join(root, f)
                        try:
                            os.chmod(path, 0o755)
                            self.fixes_applied.append(f"Fixed permissions: {f}")
                        except:
                            pass
        except Exception as e:
            print(f"Permission fix error: {e}")
    
    def create_missing_files(self):
        """Create missing required files"""
        required = [
            'analysis_config.json',
            'leads_database.json',
            'scan_cache.json'
        ]
        for f in required:
            path = self.base_path / f
            if not path.exists():
                data = {"created": "2026-11-02", "version": "1.0", "data": []}
                with open(path, 'w') as fh:
                    json.dump(data, fh, indent=2)
                self.fixes_applied.append(f"Created missing file: {f}")
    
    def fix_json_errors(self):
        """Validate and fix JSON files"""
        for json_file in self.base_path.glob('*.json'):
            try:
                with open(json_file) as f:
                    json.load(f)
            except json.JSONDecodeError:
                try:
                    # Try to fix common issues
                    with open(json_file, 'r') as f:
                        content = f.read()
                    # Basic fixes
                    content = content.strip()
                    if not content:
                        with open(json_file, 'w') as f:
                            json.dump({}, f)
                        self.fixes_applied.append(f"Fixed empty JSON: {json_file.name}")
                    else:
                        self.fixes_applied.append(f"Checked JSON: {json_file.name} (needs manual review)")
                except:
                    pass
    
    def fix_import_issues(self):
        """Create compatibility modules"""
        compat = self.base_path / 'compat.py'
        if not compat.exists():
            code = '''# Compatibility module
import sys
import os

def ensure_paths():
    """Ensure all paths are available"""
    base = os.path.dirname(os.path.abspath(__file__))
    if base not in sys.path:
        sys.path.insert(0, base)
    return base
'''
            with open(compat, 'w') as f:
                f.write(code)
            self.fixes_applied.append("Created compat.py")
    
    def validate_archive_structure(self):
        """Validate archive structure"""
        checks = []
        if (self.base_path / 'index.html').exists():
            checks.append("✓ index.html present")
        if (self.base_path / 'app.js').exists():
            checks.append("✓ app.js present")
        if len(list(self.base_path.glob('IMG_*'))) > 0:
            checks.append(f"✓ {len(list(self.base_path.glob('IMG_*')))} images found")
        if (self.base_path / 'archive_scout').exists():
            checks.append("✓ archive_scout tools present")
        return checks
    
    def run_all_fixes(self):
        print("🔧 FIXING COMMON ERRORS...")
        print("=" * 50)
        self.fix_file_permissions()
        self.create_missing_files()
        self.fix_json_errors()
        self.fix_import_issues()
        
        print("\n📊 VALIDATION:")
        for check in self.validate_archive_structure():
            print(f"  {check}")
        
        print("\n✅ FIXES APPLIED:")
        for fix in self.fixes_applied:
            print(f"  • {fix}")
        if not self.fixes_applied:
            print("  • No issues found or all already fixed")
        
        print("\n" + "=" * 50)
        print("Done!")

if __name__ == '__main__':
    fixer = ErrorFixer()
    fixer.run_all_fixes()
