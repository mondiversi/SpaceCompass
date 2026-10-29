"""Generate bounded identity-table chunks; preserve released names and ordinals."""
from pathlib import Path
import json, sys
repo=Path(__file__).resolve().parents[1]
src=repo/'app/src/main/java/me/mondiversi/spacecompass'
definitions=json.loads((repo/'catalogue/body-identities.json').read_text())
check='--check' in sys.argv
def put(name,text):
    path=src/name
    if check: assert path.read_text(encoding='utf-8')==text,name
    else: path.write_text(text,encoding='utf-8',newline='\n')
builders=[]
for start in range(0,len(definitions),80):
    part=definitions[start:start+80]; name=f'spaceCompassBodyDefinitions{start//80+1:03}'
    builders.append(name)
    lines=['package me.mondiversi.spacecompass','', 'import io.github.cosinekitty.astronomy.Body','',
        '/** Generated from catalogue/body-identities.json. */',
        f'internal fun {name}(): List<SpaceCompassBodyDefinition> = listOf(']
    lines += [f'    SpaceCompassBodyDefinition("{d["name"]}", {d["resource"]}, {d["engine"] or "null"}),' for d in part]
    put(f'SpaceCompassBodyDefinitions{start//80+1:03}.kt','\n'.join(lines+[')','']))
put('SpaceCompassBodyDefinitions.kt','package me.mondiversi.spacecompass\n\n'+
    'internal data class SpaceCompassBodyDefinition(val name: String, val nameResource: Int,\n'+
    '    val engine: io.github.cosinekitty.astronomy.Body?)\n\n'+
    'internal fun spaceCompassBodyDefinitions(): List<SpaceCompassBodyDefinition> = buildList {\n'+
    ''.join('    addAll('+name+'())\n' for name in builders)+'}\n')
print('Verified' if check else 'Generated',len(definitions),'stable identities.')
