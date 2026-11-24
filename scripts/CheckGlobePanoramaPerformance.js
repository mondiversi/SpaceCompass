/* Exact geometry/style regressions for the actual bundled panorama renderer. */
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict'),path=require('node:path');
const root=process.argv[2]||path.resolve(__dirname,'..');
const source=fs.readFileSync(path.join(root,'app/src/main/assets/globe/globe-navigation.js'),'utf8');
const near=(a,b)=>assert.ok(Math.abs(a-b)<1e-8,`${a} != ${b}`);
const vector=(lon,lat,r=1)=>{lon*=Math.PI/180;lat*=Math.PI/180;return [r*Math.cos(lat)*Math.sin(lon),r*Math.sin(lat),r*Math.cos(lat)*Math.cos(lon)];};
function context(){
 const c={Math,viewWidth:400,viewHeight:700,globeScene:{flat:true,sky:false,pose:[0,0,2,0],viewport:[0,30,400,450],labels:true},globeLabelSegments:[]};
 vm.createContext(c);
 vm.runInContext(source.slice(source.indexOf('function globePoint('),source.indexOf('// Clip a segment')),c);
 vm.runInContext(source.slice(source.indexOf('function globeFlatRect('),source.indexOf('let globeMapTexture=')),c);
 return c;
}
function reference(c,v){
 const p=c.globeScene.pose,view=c.globeScene.viewport||[0,0,c.viewWidth,c.viewHeight],w=view[2]*p[2],h=w/2;
 const limit=Math.max(0,(h-view[3])/2),offset=Math.max(-limit,Math.min(limit,p[1]/Math.PI*h));
 const x=view[0]+(view[2]-w)/2,y=view[1]+(view[3]-h)/2+offset;
 const angle=Math.atan2(v[0],v[2])-p[0]-(c.globeScene.sky?(c.globeScene.skyCenter||0)*Math.PI/180:0);
 return {x:x+((angle+Math.PI)%(2*Math.PI)+2*Math.PI)%(2*Math.PI)/(2*Math.PI)*w,
  y:y+(Math.PI/2-Math.asin(Math.max(-1,Math.min(1,v[1]/(Math.hypot(...v)||1)))))/Math.PI*h};
}
{
 const c=context(),v=vector(179,34,3);
 assert.equal(c.globeFlatPoint(v),c.globeFlatPoint(v),'An unchanged point should reuse its projected frame');
 const changes=[()=>c.globeScene.pose[0]+=.7,()=>c.globeScene.pose[1]=.6,()=>c.globeScene.pose[2]=8,
  ()=>c.globeScene.viewport[2]=690,()=>c.globeScene.viewport[3]=300,()=>c.globeScene.sky=true,
  ()=>c.globeScene.skyCenter=180,()=>{delete c.globeScene.viewport;c.viewWidth=900;c.viewHeight=400;}];
 for(const change of changes){change();for(const point of [v,vector(-179,-34),vector(0,90),vector(30,-90),[0,0,0]]){
  const got=c.globeFlatPoint(point),wanted=reference(c,point);near(got.x,wanted.x);near(got.y,wanted.y);assert.equal(got.hidden,false);
 }}
 let seed=412;const random=()=>((seed=(Math.imul(seed,1664525)+1013904223)>>>0)/4294967296);
 for(let i=0;i<500;i++){
  c.globeScene.pose=[random()*Math.PI*2-Math.PI,random()*Math.PI-Math.PI/2,.5+random()*7.5,0];
  const point=vector(random()*360-180,random()*180-90,.1+random()*10),got=c.globeFlatPoint(point),wanted=reference(c,point);
  near(got.x,wanted.x);near(got.y,wanted.y);
 }
}
function recorder(c){
 const segments=[];let from=null,pathSegments=[],dash=[],strokes=0;
 const ink={setLineDash(v){dash=[...v]},beginPath(){from=null;pathSegments=[]},moveTo(x,y){from={x,y}},lineTo(x,y){
  pathSegments.push({a:from,b:{x,y}});from={x,y};},stroke(){strokes++;for(const s of pathSegments)segments.push({...s,color:this.strokeStyle,width:this.lineWidth,dash:[...dash],alpha:this.globalAlpha})}};
 c.orbitInk=ink;return {segments,strokes:()=>strokes};
}
function relevant(s,c){return Math.max(s.a.x,s.b.x)>=-16&&Math.min(s.a.x,s.b.x)<=c.viewWidth+16&&Math.max(s.a.y,s.b.y)>=-16&&Math.min(s.a.y,s.b.y)<=c.viewHeight+16;}
for(const comparison of [false,true])for(const zoom of [.5,1,3,8])for(const center of [0,90,180,270]){
 const legacy=context(),batch=context();
 for(const c of [legacy,batch]){c.globeScene.pose=[.21,.3,zoom,0];c.globeScene.sky=true;c.globeScene.skyCenter=center;}
 const curve=Array.from({length:241},(_,i)=>[vector(-180+i*1.5,Math.sin(i/240*Math.PI*2)*55),i<30||i>=150]);
 const track={curve,color:'#e3a441',belowColor:'#786338',comparison};
 const before=recorder(legacy);for(let i=1;i<curve.length;i++)legacy.globeStroke(curve[i-1][0],curve[i][0],curve[i][1]?track.belowColor:track.color,1.6,curve[i][1]||comparison);
 const after=recorder(batch);batch.drawGlobeFlatTrack(track);
 // Every original subpath and dash reset is retained, including both seam fragments.
 assert.deepEqual(JSON.parse(JSON.stringify(after.segments)),JSON.parse(JSON.stringify(before.segments.filter(s=>relevant(s,legacy)))));
 const retained=legacy.globeLabelSegments.filter(([a,b])=>relevant({a,b},legacy));
 assert.deepEqual(JSON.parse(JSON.stringify(batch.globeLabelSegments)),JSON.parse(JSON.stringify(retained)));
 assert.ok(after.strokes()<=3,'The contiguous above/below style runs should each submit at most one stroke');
}
{
 const c=context();let measurements=0;
 c.orbitInk={font:'',measureText:t=>{measurements++;return {width:t.length*6}}};
 c.globeLabelSides=new Map();c.globeLabelDetails=new Map();c.globeHits=[];
 vm.runInContext(source.slice(source.indexOf('function globeLineTouchesBox('),source.indexOf('function globeLabelLeader(')),c);
 const label='Luna · Punto 12\n12:00 · T−1h0m\n↔ 180° · ↕ +20°';
 const initial=c.globeTextLayout(label),count=measurements;
 assert.equal(c.globeTextLayout(label),initial);assert.equal(measurements,count);
 c.viewWidth=160;assert.notEqual(c.globeTextLayout(label),initial);assert.ok(measurements>count);
 for(const text of ['قمر · ١٢','月 · 12','Ένα πολύ μακρύ όνομα','Unbroken'.repeat(90),'']){
  const layout=c.globeTextLayout(text);assert.ok(layout.w<=c.viewWidth-16);assert.ok(layout.lines.length>=1);
 }
 for(let i=0;i<1000;i++)c.globeTextLayout('Point '+i);
 assert.ok(vm.runInContext('globeTextLayouts.size',c)<=256,'Changing clocks/countdowns cannot grow the text cache without a bound');
}
{
 const c=context();vm.runInContext(source.slice(source.indexOf('function globePointNearBox('),source.indexOf('// Font/width/text')),c);
 const box={x:0,y:0,w:10,h:10};
 assert.equal(c.globePointNearBox({x:13,y:14},box,5),false,'Exact clearance boundary remains strict');
 assert.equal(c.globePointNearBox({x:13,y:14},box,5.01),true);
 assert.equal(c.globePointNearBox({x:5,y:5},box,0),false);
 assert.equal(c.globePointNearBox({x:5,y:5},box,-5),false);
 assert.equal(c.globePointNearBox({x:5,y:5},box,1),true);
 let seed=711;const random=()=>((seed=(Math.imul(seed,1664525)+1013904223)>>>0)/4294967296);
 for(let i=0;i<2000;i++){
  const p={x:random()*2000-1000,y:random()*2000-1000},b={x:random()*500-250,y:random()*500-250,w:random()*300,h:random()*300},r=random()*100;
  const x=Math.max(b.x,Math.min(b.x+b.w,p.x)),y=Math.max(b.y,Math.min(b.y+b.h,p.y));
  assert.equal(c.globePointNearBox(p,b,r),Math.hypot(x-p.x,y-p.y)<r,'Label and point clearance must match the previous renderer');
 }
}
console.log('Panorama performance regressions passed: exact pan/zoom/viewport/centre/pole coordinates, seam/dash/style preservation, offscreen-only culling and bounded Unicode text caches.');
