/* Exercise the real offline renderer's separated update channels and hit freshness. */
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm'),assert=require('node:assert/strict');
const root=process.argv[2]||path.resolve(__dirname,'..');
const source=fs.readFileSync(path.join(root,'app/src/main/assets/globe/globe-navigation.js'),'utf8');
let styleWrites=0;
const style=()=>new Proxy({}, {set(o,k,v){styleWrites++;o[k]=v;return true;}});
const tracks=[{icon:'unchanged-image',curve:[[1,2,3]],dots:[]}];
const context={window:{},document:{documentElement:{style:style()},body:{style:style()}},
 globeScene:{tracks,pose:[0,0,1],canvasSize:[800,400]},spacePalettes:{light:['','','light'],dark:['','','dark']},
 requestGlobePaint(){},globePaintDirty:false,performance:{now:()=>1000},globeHits:[],
 paintWorld(){this.globePaintDirty=false;this.globeHits=[{id:27,x:10,y:20,current:false}];}};
context.paintWorld=()=>{context.globePaintDirty=false;context.globeHits=[{id:27,x:10,y:20,current:false}];};
vm.createContext(context);
vm.runInContext(source.slice(source.indexOf('window.updateGlobe='),source.indexOf('// Android\'s embedded WebView')),context);
context.window.updateGlobe({content:{dark:true,background:false,tracks},frame:{viewport:[0,30,800,340]},pose:[1,0,1]});
const writesAfterContent=styleWrites;
for(let i=0;i<120;i++)context.window.updateGlobe({frame:{canvasSize:[800-i,400]},pose:[i,0,1]});
assert.equal(context.globeScene.tracks,tracks,'Geometry/image references must survive every intermediate layout or drag');
assert.equal(context.globeScene.tracks[0].icon,'unchanged-image');
assert.equal(styleWrites,writesAfterContent,'Frame/pose must not touch theme DOM styles');
assert.equal(context.globeScene.pose[0],119);
assert.equal(context.globeScene.canvasSize[0],681);
assert.equal(context.window.pickGlobePoint(10,20),27,'Tap must paint pending changes before reading hit IDs');
assert.equal(context.globePaintDirty,false);
console.log('Globe updates: retained tracks/images, light layout/pose channels and fresh hit IDs passed.');
