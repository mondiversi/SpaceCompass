"""Generate matching science-fiction descriptions and search aliases; proper names retain international aliases."""
from pathlib import Path
import html,json,sys
repo=Path(__file__).resolve().parents[1]
check='--check' in sys.argv
data=json.loads((repo/'catalogue/science-fiction-translations.json').read_text(encoding='utf-8'))
dirs={'en':'values','id':'values-in','he':'values-iw','ko':'values-ko-rKR','zh_CN':'values-zh-rCN'}
def put(path,text):
    if check:assert path.read_text(encoding='utf-8')==text,str(path)
    else:path.write_text(text,encoding='utf-8',newline='\n')
def escape(value):return html.escape(value,quote=False).replace("'","\\'").replace('"','\\"')
for lang,strings in data.items():
    assert strings.keys()==data['en'].keys()
    folder=repo/'app/src/main/res'/dirs.get(lang,'values-'+lang)
    lines=['<?xml version="1.0" encoding="utf-8"?>','<resources>']
    lines += ['    <string name="'+key+'">'+escape(value)+'</string>' for key,value in strings.items()]
    put(folder/'science_fiction.xml','\n'.join(lines+['</resources>','']))
ids=[key.removeprefix('scifi_name_') for key in data['en'] if key.startswith('scifi_name_')]
lines=['package me.mondiversi.spacecompass','','/** Generated international/localized search aliases. */',
    'internal fun spaceCompassFictionalAliases(body: SpaceCompassCelestialBody): List<String> = when (body) {']
for key in ids:
    names=list(dict.fromkeys(lang['scifi_name_'+key] for lang in data.values()))
    if key=='arrakis':names+=['Dune','Arrakis (Dune)']
    lines+=['    SpaceCompassCelestialBody.'+key.upper()+' -> listOf('+', '.join(json.dumps(name,ensure_ascii=False) for name in names)+')']
lines += ['    else -> emptyList()','}','']
put(repo/'app/src/main/java/me/mondiversi/spacecompass/SpaceCompassFictionalAliases.kt','\n'.join(lines))
print('Verified' if check else 'Generated',len(data),'science-fiction languages and international aliases.')
