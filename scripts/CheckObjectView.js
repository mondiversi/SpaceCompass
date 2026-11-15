// Exercise the actual object-camera ray/projection code without a browser or network.
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm'),assert=require('node:assert/strict');
const root=process.argv[2]||path.resolve(__dirname,'..');
const src=fs.readFileSync(path.join(root,'app/src/main/assets/globe/globe-navigation.js'),'utf8');
const c={window:{},globeScene:{pose:[0,0,1],objectCamera:null},viewWidth:400,viewHeight:600,
 earthViewVector:v=>({x:v[0],y:v[1],depth:v[2]}),earthDiameter:()=>200,spaceCompassGlobeCenter:()=>[200,300]};
vm.createContext(c);vm.runInContext(src.slice(src.indexOf('function globeObjectPoint('),src.indexOf('function globePoint(')),c);
let count=0;
for(const eye of [[0,0,1.5],[.9,0,1.2],[1.5,0,0],[0,1.5,0],[0,0,-1.5]]) {
 const distance=Math.hypot(...eye),focal=Math.sqrt(distance*distance-1),depth=eye[2]*eye[2]-1;
 c.globeScene.objectCamera={eye,target:[0,0,0],focal,framing:[0,0,1]};
 for(let lat=-75;lat<=75;lat+=15)for(let lon=-180;lon<180;lon+=15) {
  const a=lat*Math.PI/180,b=lon*Math.PI/180,v=[Math.cos(a)*Math.sin(b),Math.sin(a),Math.cos(a)*Math.cos(b)];
  const p=c.globeObjectPoint(v);if(p.depth<.01)continue;
  const n=c.window.spaceCompassObjectNormal((p.x-200)/100,(300-p.y)/100);
  n.forEach((value,i)=>assert.ok(Math.abs(value-v[i])<1e-8));count++;
 }
 assert.ok(c.globeObjectPoint([0,0,-1]).hidden);
}
c.globeScene.objectCamera={eye:[0,0,-1.5],target:[0,0,0],focal:Math.sqrt(1.25),framing:[0,0,1]};
assert.ok(Number.isNaN(c.window.spaceCompassObjectNormal(0,0)[2]));
assert.ok(c.globeObjectPoint([0,0,1]).behindEye);
c.globeScene.objectCamera={eye:null,target:[.6,0,.8],focal:1};
assert.ok(Math.abs(c.globeObjectPoint([.6,0,.8]).x-200)<1e-8);
assert.deepEqual(Array.from(c.window.spaceCompassObjectNormal(0,0)),[.6,0,.8]);
console.log('Object camera JS: '+count+' finite projection/ray round trips, Earth occultation, outward free look and legacy parallel-ray support passed.');
