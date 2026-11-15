const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const root=process.argv[2]||require('node:path').resolve(__dirname,'..');
const source=fs.readFileSync(root+'/app/src/main/assets/globe/globe-navigation.js','utf8');
let now=0,paints=0,timer=0;const timers=new Map();
const c={window:{},globeScene:{sky:false},performance:{now:()=>now},document:{hidden:false},
 setTimeout(fn,ms){timers.set(++timer,{fn,ms});return timer},clearTimeout(id){timers.delete(id)},requestGlobePaint(){paints++}};
vm.createContext(c);vm.runInContext(source.slice(source.indexOf('let globeDetailUntil='),source.indexOf('let globeCanvasLayout=')),c);
assert.equal(c.window.spaceCompassGlobeDetailSize(),640);
c.window.setGlobeInteracting(true);assert.equal(c.window.spaceCompassGlobeDetailSize(),256);
now=1000;assert.equal(c.window.spaceCompassGlobeDetailSize(),256);
c.window.setGlobeInteracting(false);assert.equal(c.window.spaceCompassGlobeDetailSize(),256);
now+=200;for(const entry of timers.values())entry.fn();timers.clear();assert.equal(c.window.spaceCompassGlobeDetailSize(),640);assert.ok(paints>0);
c.window.spaceCompassGlobeDetailActivity();assert.equal(c.window.spaceCompassGlobeDetailSize(),256);assert.equal(timers.size,1);
now+=200;assert.equal(c.window.spaceCompassGlobeDetailSize(),640);
c.globeScene.sky=true;assert.equal(c.window.spaceCompassGlobeDetailSize(),256);
console.log('Interactive detail: higher settled quality, light gestures/layout, one refinement and unaffected sky passed.');
