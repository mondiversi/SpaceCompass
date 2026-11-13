const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm'),assert=require('node:assert/strict');
const root=process.argv[2]||path.resolve(__dirname,'..');
const source=fs.readFileSync(path.join(root,'app/src/main/assets/globe/globe-navigation.js'),'utf8');
const canvas=()=>({style:{},width:0,height:0});
const worldCanvas=canvas(),earth=canvas(),orbitCanvas=canvas(),fallback={style:{}};
const context={Math,innerWidth:711,innerHeight:634,devicePixelRatio:2.75,viewWidth:711,viewHeight:634,pixelRatio:1,
 worldCanvas,orbitCanvas,window:{},document:{documentElement:{style:{}},body:{style:{}},
  getElementById:id=>id==='earth-canvas'?earth:fallback},
 globeScene:{pose:[0,.4,1,0],extent:1.55,fitBounds:[1.55,1.13],canvasSize:[382.4,341.3],viewport:[0,50,382.4,246]},
 requestGlobePaint(){}};
vm.createContext(context);
vm.runInContext(source.slice(source.indexOf('window.spaceCompassGlobeCenter='),source.indexOf('function globePoint(')),context);
context.spaceCompassGlobeCenter=context.window.spaceCompassGlobeCenter;
vm.runInContext(source.slice(source.indexOf('let globeCanvasLayout=')),context);
// A shrunken native pane must win over a stale/enlarged mobile CSS viewport.
assert.equal(context.viewWidth,382.4);assert.equal(context.viewHeight,341.3);
assert.equal(worldCanvas.style.width,'100%');assert.equal(earth.style.height,'341.3px');
assert.equal(worldCanvas.width,Math.round(382.4*1.5));assert.equal(orbitCanvas.height,Math.round(341.3*1.5));
const radius=Math.min((382.4/2-24)/1.55,(246/2-24)/1.13);
assert.ok(Math.abs(context.earthDiameter(382.4,341.3)-radius*2)<1e-9);
context.globeScene.canvasSize=[711.6,341.3];context.globeScene.viewport=[0,30,711.6,240];
context.globeScene.pose[2]=1.5;context.resizeWorld();
assert.equal(context.viewWidth,711.6);assert.equal(context.viewHeight,341.3);
assert.equal(worldCanvas.width,Math.round(711.6*1.5));
assert.equal(context.spaceCompassGlobeCenter(711.6,341.3)[0],355.8);
assert.equal(context.spaceCompassGlobeCenter(711.6,341.3)[1],150);
const oldHeight=context.innerHeight;
context.globeScene.canvasSize=[391.6,638.2];context.globeScene.viewport=[0,50,391.6,480];context.resizeWorld();
assert.equal(context.innerHeight,oldHeight);assert.equal(context.viewHeight,638.2);
// Motion without new native bounds must not write layout styles or reset backing buffers.
const before=[worldCanvas.width,worldCanvas.height];
context.globeScene.pose[0]=2;context.resizeWorld();
assert.deepEqual([worldCanvas.width,worldCanvas.height],before);
// A full-scene canvas keeps the Earth on the inset pointing column's optical axis.
context.globeScene.canvasSize=[900,700];context.globeScene.viewport=[10,50,500,320];context.resizeWorld();
assert.equal(context.spaceCompassGlobeCenter(900,700)[0],260);
assert.equal(context.spaceCompassGlobeCenter(900,700)[1],210);
assert.ok(Math.abs(context.earthDiameter(900,700)-Math.min((500/2-24)/1.55,(320/2-24)/1.13)*2*1.5)<1e-9);
console.log('Globe viewport: native bounds, responsive canvases, rotation/column resizing, fit and zoom passed.');
