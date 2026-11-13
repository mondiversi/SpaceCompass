const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm'),assert=require('node:assert/strict');
const root=process.argv[2]||path.resolve(__dirname,'..');
const source=fs.readFileSync(path.join(root,'app/src/main/assets/globe/globe-navigation.js'),'utf8');
const context={Math,viewWidth:800,viewHeight:600,globeScene:{flat:true,sky:false,pose:[0,0,1,0],viewport:[10,20,600,360]}};
vm.createContext(context);
vm.runInContext(source.slice(source.indexOf('function globeFlatRect('),source.indexOf('let globeMapTexture=null,')),context);
vm.runInContext(source.slice(source.indexOf('function globePoint('),source.indexOf('function globeStroke(')),context);
const near=(a,b)=>assert.ok(Math.abs(a-b)<1e-8,`${a} != ${b}`);
let r=context.globeFlatRect();near(r.x,10);near(r.y,50);near(r.w,600);near(r.h,300);
const v=(longitude,latitude)=>{const theta=longitude*Math.PI/180,phi=latitude*Math.PI/180;return [Math.cos(phi)*Math.sin(theta),Math.sin(phi),Math.cos(phi)*Math.cos(theta)];};
let p=context.globePoint(v(0,0));near(p.x,310);near(p.y,200);assert.equal(p.hidden,false);
near(context.globePoint(v(90,0)).x,460);near(context.globePoint(v(-90,0)).x,160);
near(context.globePoint(v(0,90)).y,50);near(context.globePoint(v(0,-90)).y,350);
// The date-line seam creates two short edge segments, never a chord across the map.
let segments=context.globeFlatSegment(context.globePoint(v(179,20)),context.globePoint(v(-179,20)));
assert.equal(segments.length,2);near(segments.reduce((total,[a,b])=>total+Math.hypot(a.x-b.x,a.y-b.y),0),600*2/360);
for(const [a,b] of segments)for(const point of [a,b])assert.ok(point.x>=r.x-1e-8&&point.x<=r.x+r.w+1e-8);
// Sky uses azimuth/elevation in the same 360x180 projection as captures, not geographic longitude.
context.globeScene.sky=true;context.globeScene.skyCenter=180;
for(const azimuth of [0,30,90,180,270,359])for(const elevation of [-90,-30,0,30,90]){
 p=context.globePoint(v(azimuth,elevation));near(p.x,10+azimuth/360*600);near(p.y,50+(90-elevation)/180*300);
}
context.globeScene.skyCenter=0;near(context.globePoint(v(0,0)).x,310);
context.globeScene.pose[0]=Math.PI/2;near(context.globePoint(v(90,0)).x,310);
context.globeScene.pose=[0,0,2,0];r=context.globeFlatRect();near(r.w,1200);near(r.h,600);near(r.x,-290);near(r.y,-100);
vm.runInContext(source.slice(source.indexOf('const globeCoordinateCurves=[];'),source.indexOf('function drawGlobeCoordinateGrid(')),context);
const curves=vm.runInContext('globeCoordinateCurves',context);assert.equal(curves.length,35);
assert.equal(curves.filter(v=>v.latitude).length,11);assert.equal(curves.filter(v=>!v.latitude).length,24);
for(const curve of curves)for(const point of curve.vertices)near(Math.hypot(...point),1);
console.log('Live projections: geographic/angular axes, poles, seam splitting, map pan/zoom and 15-degree grid passed.');

// Grid visibility is independent from named reference guides in both Globe projections.
vm.runInContext(source.slice(source.indexOf('function drawGlobeCoordinateGrid('),source.indexOf('// Plate-carree maps')),context);
vm.runInContext(source.slice(source.indexOf('function drawFlatCoordinateGrid('),source.indexOf('function drawGlobeOverlays(')),context);
context.globePoint=v=>({x:v[0],y:v[1],depth:1});
let strokes=0;
context.orbitInk={beginPath(){},moveTo(){},lineTo(){},setLineDash(){},stroke(){strokes++;}};
for(const flat of [false,true])for(const guides of [false,true])for(const grid of [false,true]){
 context.globeScene.flat=flat;context.globeScene.guides=guides;context.globeScene.grid=grid;strokes=0;
 context.drawGlobeCoordinateGrid();assert.equal(strokes,grid?2:0,'Grid must not follow named-reference visibility');
}
console.log('Independent grid/reference visibility passed in spherical and flat projections.');

// Globe-map centring and pole limits use the real renderer in tall, wide and split columns.
context.globeScene.sky=false;
for(const [w,h] of [[400,800],[900,400],[495,400],[855,400]])for(const zoom of [.5,1,1.5,3,8])for(const latitude of [-Math.PI/2,0,Math.PI/2]){
 context.globeScene.viewport=[10,20,w,h];context.globeScene.pose=[.2,latitude,zoom,0];
 const rect=context.globeFlatRect();near(rect.x+rect.w/2,10+w/2);
 near(rect.w,w*zoom);near(rect.h,rect.w/2);
 if(zoom>=1){assert.ok(rect.x<=10+1e-8);assert.ok(rect.x+rect.w>=10+w-1e-8);}
 if(rect.h<=h)near(rect.y+rect.h/2,20+h/2);
 else {assert.ok(rect.y<=20+1e-8);assert.ok(rect.y+rect.h>=20+h-1e-8);}
 if(latitude===0)near(rect.y+rect.h/2,20+h/2);
 if(zoom===8)assert.ok(rect.h>=h);
}
console.log('Globe map: balanced margins, split columns, 8x coverage and bounded vertical pan passed.');

// Sky and Globe panoramas share width fit and bounded vertical pan.
context.globeScene.viewport=[0,0,1000,400];context.globeScene.pose=[0,0,1,0];
context.globeScene.sky=true;near(context.globeFlatRect().w,1000);
context.globeScene.sky=false;near(context.globeFlatRect().w,1000);
console.log('Shared Globe/Sky panorama width fit passed.');
