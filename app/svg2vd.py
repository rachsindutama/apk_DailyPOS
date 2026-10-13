import xml.etree.ElementTree as ET
import re

def convert_style_to_fill_color(style):
    if not style:
        return None
    # match fill:#XXXXXX or fill:rgb(...)
    match = re.search(r'fill:\s*(#[0-9a-fA-F]+)', style)
    if match:
        return match.group(1)
    return None

def rect_to_path(attrib):
    x = float(attrib.get('x', 0))
    y = float(attrib.get('y', 0))
    w = float(attrib.get('width', 0))
    h = float(attrib.get('height', 0))
    return f"M {x},{y} h {w} v {h} h {-w} Z"

def polygon_to_path(attrib):
    points = attrib.get('points', '').strip()
    if not points:
        return ""
    # Replace commas/spaces to get numbers
    coords = [c for c in re.split(r'[\s,]+', points) if c]
    if len(coords) < 2:
        return ""
    path_parts = [f"M {coords[0]},{coords[1]}"]
    for i in range(2, len(coords)-1, 2):
        path_parts.append(f"L {coords[i]},{coords[i+1]}")
    path_parts.append("Z")
    return " ".join(path_parts)

# Parse SVG
tree = ET.parse('app/store_logo.svg')
root = tree.getroot()

# Vector Drawable header
vd_lines = [
    '<?xml version="1.0" encoding="utf-8"?>',
    '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
    '    android:width="291.775dp"',
    '    android:height="291.775dp"',
    '    android:viewportWidth="380"',
    '    android:viewportHeight="380">',
    '    <group',
    '        android:translateX="44.1125"',
    '        android:translateY="44.1125">'
]

def traverse(element):
    # Strip namespace tag
    tag = element.tag.split('}')[-1] if '}' in element.tag else element.tag
    
    fill = convert_style_to_fill_color(element.attrib.get('style', ''))
    
    if tag == 'path':
        d = element.attrib.get('d', '')
        if fill and d:
            # Clean up whitespace in d
            d_clean = " ".join(d.split())
            vd_lines.append(f'        <path\n            android:fillColor="{fill}"\n            android:pathData="{d_clean}" />')
    elif tag == 'rect':
        d = rect_to_path(element.attrib)
        if fill and d:
            vd_lines.append(f'        <path\n            android:fillColor="{fill}"\n            android:pathData="{d}" />')
    elif tag == 'polygon':
        d = polygon_to_path(element.attrib)
        if fill and d:
            vd_lines.append(f'        <path\n            android:fillColor="{fill}"\n            android:pathData="{d}" />')
            
    for child in element:
        traverse(child)

traverse(root)
vd_lines.append('    </group>')
vd_lines.append('</vector>')

with open('app/src/main/res/drawable/ic_cash_register.xml', 'w') as f:
    f.write('\n'.join(vd_lines))

print("Conversion complete!")
