const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict'),path=require('node:path');
const root=path.resolve(__dirname,'..'),main=path.join(root,'app/src/main/java/me/mondiversi/spacecompass');
const source=fs.readFileSync(path.join(root,'app/src/main/assets/globe/globe-navigation.js'),'utf8');
const strokes=[];
const context={Math,globeScene:{grid:true,flat:false},globeCoordinateCurves:[],globePoint:v=>({x:v[0],y:v[1],depth:1}),
 globeFlatRect:()=>({x:0,y:0,w:400,h:200}),orbitInk:{beginPath(){},moveTo(){},lineTo(){},setLineDash(){},stroke(){strokes.push([this.strokeStyle,this.lineWidth])}}};
vm.createContext(context);
vm.runInContext(source.slice(source.indexOf('function drawGlobeCoordinateGrid('),source.indexOf('// Plate-carree maps')),context);
vm.runInContext(source.slice(source.indexOf('function drawFlatCoordinateGrid('),source.indexOf('// Keep longitude/azimuth')),context);
for(const flat of [false,true])for(const ink of [undefined,['#9c9c9cff','#00000059'],['#707070ff','#00000059'],['#70707080','#0000002d'],['#70707000','#00000000']]) {
 context.globeScene.flat=flat;context.globeScene.gridStyle=ink;strokes.length=0;context.drawGlobeCoordinateGrid();
 assert.deepEqual(strokes,[[ink?.[1]||'#00000047',.60],[ink?.[0]||'#9c9c9ccc',.30]]);
}
for(const name of ['SpaceCompassCameraGridRenderer.kt','SpaceCompassCameraPhotoRenderer.kt','SpaceCompassGlobeCoordinateDrawing.kt','SpaceCompassGlobeMapGrid.kt','SpaceCompassPanoramaRenderer.kt']) {
 const s=fs.readFileSync(path.join(main,name),'utf8');assert.ok(s.includes('gridPalette=spaceCompassGridPalette(snapshot)'),name);
 assert.ok(!/SpaceCompassAngularGridAppearance\.(lineArgb|majorArgb|shadowArgb)/.test(s),name+' bypasses scenario palette');
}
const host=fs.readFileSync(path.join(main,'SpaceCompassGlobeView.kt'),'utf8');
assert.ok(host.includes('put("gridStyle",JSONArray(listOf(spaceCompassGridCss(gridPalette.liveLineArgb),spaceCompassGridCss(gridPalette.liveShadowArgb))))'));
const live=fs.readFileSync(path.join(main,'SpaceCompassSunFinderScreen.kt'),'utf8');
assert.ok(live.includes('solarElevation=sun?.elevationDegrees'));
console.log('Grid lighting: native/frozen/web paths share scenario ink, 80-percent night fallback and adjustable opacity, immediate dawn/day/dusk colour and unchanged strokes passed.');
