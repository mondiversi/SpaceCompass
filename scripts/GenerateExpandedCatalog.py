from pathlib import Path
import json, re, html, shutil, sys

repo=Path(__file__).resolve().parents[1]
stage=repo/'catalogue'
CHECK='--check' in sys.argv
src=repo/'app/src/main/java/me/mondiversi/spacecompass'
rows=json.loads((stage/'expanded-catalog.json').read_text())
def string(s):return json.dumps(s,ensure_ascii=False).replace('$','\\$')
def value(v):
    if v is None:return 'null'
    if isinstance(v,bool):return str(v).lower()
    if isinstance(v,(int,float)):return repr(float(v))
    return string(v)
def ref(r):
    return 'SpaceCompassDeepSkyReference('+', '.join(value(r[k]) for k in ['ra','dec'])+', '+(value(r['distanceLy']) if r['distanceLy'] is not None else 'Double.NaN')+', '+', '.join(value(r[k]) for k in ['pmRa','pmDec'])+(', hasMeasuredDistance = false' if r['distanceLy'] is None else '')+')'
def facts(r):
    fields=[]
    for k,v in r.items():
        if v is not None:fields.append(k+' = '+('SpaceCompassCelestialBody.'+v if k=='parent' else value(v)))
    return 'SpaceCompassCelestialFacts('+', '.join(fields)+')'
def put(name,text):
    content='package me.mondiversi.spacecompass\n\n'+text
    if CHECK:assert (src/name).read_text(encoding='utf-8')==content,name
    else:(src/name).write_text(content,encoding='utf-8',newline='\n')
def edit(name,fn):
    path=src/name;text=path.read_text(encoding='utf-8');updated=fn(text)
    if CHECK:assert updated==text,name
    elif updated!=text:path.write_text(updated,encoding='utf-8',newline='\n')

# Identity definitions are independently append-only; never reassign saved ordinals.
identities=json.loads((stage/'body-identities.json').read_text())
assert {r['id'] for r in rows} <= {d['name'] for d in identities}

builders=[]
for start in range(0,len(rows),35):
    part=rows[start:start+35];name=f'SpaceCompassLargeCatalogData{start//35+1:02}'
    builders.append('spaceCompassLargeCatalogData'+str(start//35+1).zfill(2)+'()')
    lines=['/** Generated from catalogue/expanded-catalog.json; regenerate with scripts/GenerateExpandedCatalog.py. */',
        'internal fun '+builders[-1]+': List<SpaceCompassLargeCatalogEntry> = listOf(']
    for r in part:
        note='R.string.catalogue_direction_note' if r.get('reference',{}).get('distanceLy','no') is None else 'R.string.catalogue_reference_note'
        if r['type']=='CONSTELLATION':note='R.string.catalogue_constellation_note'
        elif r['type']=='EXOPLANET':note='R.string.celestial_unresolved_exoplanet_note'
        elif r['type']=='BLACK_HOLE':note='R.string.catalogue_compact_note'
        elif 'target' in r:note='R.string.celestial_horizons_tracking_note'
        elif 'satellite' in r:note='R.string.celestial_satellite_tracking_note'
        fields=['body = SpaceCompassCelestialBody.valueOf('+string(r['id'])+')',
            'entry = SpaceCompassAdditionalCatalogEntry(SpaceCompassCatalogType.'+r['type']+', '+string(r['designation'])+', '+(ref(r['reference']) if 'reference'in r else 'null')+', '+('SpaceCompassHorizonsTarget('+string(r['target']['command'])+', '+string(r['target']['expected'])+', 0.0..'+value(r['target']['maxAu'])+')' if 'target'in r else 'null')+')',
            'facts = '+facts(r.get('facts',{})),
            'aliases = listOf('+', '.join(string(s) for s in dict.fromkeys(r.get('aliases',[])+list(r.get('localizedNames',{}).values())))+')',
            'source = '+string(r['source']), 'sourceUrl = '+string(r['sourceUrl']),
            'galaxy = '+string(r.get('galaxy','UNKNOWN')),'note = '+note]
        if r.get('temperatures'):fields.append('temperatures = listOf('+', '.join('SpaceCompassCelestialTemperature(SpaceCompassCelestialTemperatureKind.'+a['kind']+', '+value(a['celsius'])+')' for a in r['temperatures'])+')')
        if 'pattern'in r:fields.append('pattern = spaceCompassPattern'+r['id'].replace('CONSTELLATION_IAU_','')+'()')
        if 'satellite'in r:
            s=r['satellite'];fields.append('satellite = SpaceCompassCatalogSatellite('+', '.join(string(s[k]) for k in ['norad','international','publicName'])+', '+', '.join(value(s[k][0])+'..'+value(s[k][1]) for k in ['inclination','motion','radius','speed'])+')')
        if r.get('subtype')=='tno-candidate':fields.append('classification = R.string.celestial_identity_candidate')
        if r.get('subtype')=='interstellar':fields.append('classification = R.string.catalogue_interstellar')
        if r.get('evidence')=='compact-binary':fields.append('classification = R.string.catalogue_compact_candidate')
        lines.append('    SpaceCompassLargeCatalogEntry(\n        '+',\n        '.join(fields)+'),')
    lines.append(')\n');put(name+'.kt','\n'.join(lines))

pattern_rows=[r for r in rows if 'pattern'in r]
for start in range(0,len(pattern_rows),7):
    lines=['/** Stellarium western line patterns, HYG ICRS/J2000 star references. CC BY-SA 4.0. */']
    for r in pattern_rows[start:start+7]:
        p=r['pattern'];lines.append('internal fun spaceCompassPattern'+r['id'].replace('CONSTELLATION_IAU_','')+'() = SpaceCompassConstellationPattern(\n    stars = listOf(')
        lines += ['        SpaceCompassConstellationStar('+string(s['name'])+', '+ref(s['reference'])+'),' for s in p['stars']]
        lines += ['    ),\n    edges = listOf('+', '.join(str(a)+' to '+str(b) for a,b in p['edges'])+')\n)\n']
    put(f'SpaceCompassConstellationData{start//7+1:02}.kt','\n'.join(lines))

put('SpaceCompassLargeCatalog.kt','''/** Bundled, typed science registry. Graphics are deliberately not bundled here.
 * Stable enum identities are append-only; all lookups are O(1) after one lazy initialization. */
internal data class SpaceCompassLargeCatalogEntry(
    val body: SpaceCompassCelestialBody,
    val entry: SpaceCompassAdditionalCatalogEntry,
    val facts: SpaceCompassCelestialFacts,
    val aliases: List<String>,
    val source: String,
    val sourceUrl: String,
    val galaxy: String,
    val note: Int,
    val temperatures: List<SpaceCompassCelestialTemperature> = emptyList(),
    val satellite: SpaceCompassCatalogSatellite? = null,
    val pattern: SpaceCompassConstellationPattern? = null,
    val classification: Int? = null
)
internal data class SpaceCompassConstellationPattern(val stars: List<SpaceCompassConstellationStar>,
    val edges: List<Pair<Int, Int>>)

internal val spaceCompassLargeCatalogEntries: List<SpaceCompassLargeCatalogEntry> by lazy {
    buildList {
'''+''.join('        addAll('+b+')\n' for b in builders)+'''    }.also { entries ->
        check(entries.size == '''+str(len(rows))+''' && entries.map { it.body }.toSet().size == entries.size)
    }
}
private val largeCatalogByOrdinal: Array<SpaceCompassLargeCatalogEntry?> by lazy {
    arrayOfNulls<SpaceCompassLargeCatalogEntry>(SpaceCompassCelestialBody.entries.size).also { index ->
        spaceCompassLargeCatalogEntries.forEach { index[it.body.ordinal] = it }
    }
}
internal val SpaceCompassCelestialBody.largeCatalogEntry: SpaceCompassLargeCatalogEntry?
    get() = largeCatalogByOrdinal[ordinal]

/** Retain all previous relative ordering, placing additions after their existing scientific family. */
internal fun spaceCompassIncludeLargeCatalog(previous: List<SpaceCompassCelestialBody>): List<SpaceCompassCelestialBody> {
    val additions = spaceCompassLargeCatalogEntries.groupBy { it.entry.type }
    val last = previous.withIndex().associate { it.value.catalogType to it.index }
    return buildList {
        previous.forEachIndexed { index, body ->
            add(body)
            if (last[body.catalogType] == index) addAll(additions[body.catalogType].orEmpty().map { it.body })
        }
    }.also { check(it.size == '''+str(len(rows)+105)+''' && it.toSet().size == it.size) }
}
''')

print('Verified' if CHECK else 'Generated', len(rows), 'bundled catalogue records.')
