#!/usr/bin/env python3
import os
import re
import subprocess
import sys
from datetime import datetime

PATTERN = re.compile(r'^(?:IMG|VID|null)-(\d{8})-WA\d+')
ROOTS = ['Images', 'vid']


def parse_date(name):
    m = PATTERN.match(name)
    if not m:
        return None
    try:
        return datetime.strptime(m.group(1), '%Y%m%d')
    except ValueError:
        return None


def main():
    dry = '--apply' not in sys.argv
    updated = 0
    skipped = []
    for root in ROOTS:
        for dirpath, dirnames, filenames in os.walk(root):
            dirnames[:] = [d for d in dirnames if not d.startswith('.')]
            for fn in sorted(filenames):
                if fn.startswith('.'):
                    continue
                path = os.path.join(dirpath, fn)
                d = parse_date(fn)
                if d is None:
                    skipped.append(path)
                    continue
                if not dry:
                    dt = d.replace(hour=12, minute=0, second=0)
                    ts = dt.timestamp()
                    os.utime(path, (ts, ts))
                    subprocess.run(
                        ['SetFile', '-d', dt.strftime('%m/%d/%Y %H:%M:%S'), path],
                        check=True,
                    )
                updated += 1

    mode = 'DRY RUN' if dry else 'APPLIED'
    verb = 'would be updated' if dry else 'updated'
    print(f'[{mode}] {updated} files {verb}')
    if skipped:
        print(f'skipped {len(skipped)} non-matching files:')
        for p in skipped:
            print(f'  {p}')


if __name__ == '__main__':
    main()
