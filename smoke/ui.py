import sys, re, xml.etree.ElementTree as ET
# usage: ui.py file.xml (text|desc|textprefix|descprefix) needle [index]
path, mode, needle = sys.argv[1], sys.argv[2], sys.argv[3]
idx = int(sys.argv[4]) if len(sys.argv) > 4 else 0
try:
    nodes = list(ET.parse(path).iter('node'))
except Exception:
    sys.exit(0)
hits = []
for n in nodes:
    v = n.get('content-desc', '') if mode.startswith('desc') else n.get('text', '')
    ok = v.startswith(needle) if mode.endswith('prefix') else v == needle
    if ok:
        m = re.findall(r'\d+', n.get('bounds', ''))
        if len(m) == 4:
            x1, y1, x2, y2 = map(int, m)
            hits.append(((x1 + x2) // 2, (y1 + y2) // 2))
if len(hits) > idx:
    print(*hits[idx])
