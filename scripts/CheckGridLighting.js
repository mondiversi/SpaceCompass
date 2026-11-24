const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict'),path=require('node:path');
const root=path.resolve(__dirname,'..'),main=path.join(root,'app/src/main/java/me/mondiversi/spacecompass');
const source=fs.readFileSync(path.join(root,'app/src/main/assets/globe/globe-navigation.js'),'utf8');
const strokes=[];
const context={Math,globeScene:{grid:true,flat:false},globeCoordinateCurves:[],globePoint:v=>({x:v[0],y:v[1],depth:1}),
 globeFlatRect:()=>({x:0,y:0,w:400,h:200}),orbitInk:{beginPath(){},moveTo(){},lineTo(){},setLineDash(){},stroke(){strokes.push([this.strokeStyle,this.lineWidth])}}};
vm.createContext(context);
vm.runInContext(source.slice(source.indexOf('function drawGlobeCoordinateGrid('),source.indexOf('// Plate-carree maps')),context);
vm.runInContext(source.slice(source.indexOf('function drawFlatCoordinateGrid('),source.indexOf('// Keep longitude/azimuth')),context);
for(const flat of [false,true])for(const ink of [undefined,['#ffffffff'],['#ffffffcc'],['#ffffff80'],['#ffffff00']]) {
 context.globeScene.flat=flat;context.globeScene.gridStyle=ink;strokes.length=0;context.drawGlobeCoordinateGrid();
 assert.deepEqual(strokes,[[ink?.[0]||'#ffffff80',.30]]);
}
for(const name of ['SpaceCompassCameraGridRenderer.kt','SpaceCompassCameraPhotoRenderer.kt','SpaceCompassGlobeCoordinateDrawing.kt','SpaceCompassGlobeMapGrid.kt','SpaceCompassPanoramaRenderer.kt']) {
 const s=fs.readFileSync(path.join(main,name),'utf8');assert.ok(s.includes('gridPalette=spaceCompassGridPalette(snapshot)')||s.includes('spaceCompassSkyGridPalette(snapshot)')||s.includes('spaceCompassSkyGridPalette(snapshot,enabled=simulatedSky)')||s.includes('spaceCompassAngularGridPalette(darkGrid,snapshot.gridOpacityPercent)'),name);
 assert.ok(!/SpaceCompassAngularGridAppearance\.(lineArgb|majorArgb|shadowArgb)/.test(s),name+' bypasses scenario palette');
}
const host=fs.readFileSync(path.join(main,'SpaceCompassGlobeView.kt'),'utf8');
assert.ok(host.includes('put("gridStyle",JSONArray(listOf(spaceCompassGridCss(gridPalette.liveLineArgb))))'));
const live=fs.readFileSync(path.join(main,'SpaceCompassSunFinderScreen.kt'),'utf8');
assert.ok(live.includes('opacityPercent=gridOpacityPercent'),'live grid retains its saved opacity');
const palette=fs.readFileSync(path.join(main,'SpaceCompassAngularGridAppearance.kt'),'utf8');
assert.ok(palette.includes('spaceCompassGridPalette(snapshot.gridOpacityPercent)'),'capture must retain its frozen opacity');
assert.ok(!palette.includes('calculateSpaceCompassSunPosition'),'grid colour must not request solar data');
// Daytime Sky draws one black ink across both sides, including after pan/zoom.
const paths=[];let points=[];
context.orbitInk.beginPath=()=>{points=[]};
context.orbitInk.moveTo=(x,y)=>points.push([x,y]);
context.orbitInk.lineTo=(x,y)=>points.push([x,y]);
context.orbitInk.stroke=function(){paths.push({ink:this.strokeStyle,points:[...points]})};
for(const horizon of [-150,0,100,550]) {
 context.globeFlatRect=()=>({x:-50,y:horizon-100,w:400,h:200});
 context.globeScene={flat:true,sky:true,grid:true,gridStyle:['#00000080']};
 paths.length=0;context.drawFlatCoordinateGrid();
 assert.equal(paths.length,1);assert.equal(paths[0].ink,'#00000080');
 assert.ok(paths[0].points.some(p=>p[1]<horizon));assert.ok(paths[0].points.some(p=>p[1]>horizon));
}
assert.ok(host.includes('spaceCompassSkyGridPalette(snapshot,enabled=sky)'),'geographic Globe must not use Sky ink');
assert.ok(live.includes('darkGrid=!cameraEnabled && spaceCompassSkyGridBlack(phase)'),'real camera must retain its white grid');
assert.ok(!palette.includes('weatherEffectsEnabled'),'weather visibility must not control Sky ink');
console.log('Grid checks passed: the whole Sky grid is black in morning/afternoon, independently of weather. Other phases, geographic maps and real camera retain white grids.');
