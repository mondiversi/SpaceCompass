const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const root=require('node:path').resolve(__dirname,'..');
const source=fs.readFileSync(root+'/app/src/main/assets/globe/globe-navigation.js','utf8');
function context(){const c={viewWidth:800,viewHeight:600,globeScene:{viewport:[0,0,800,600],exclusions:[]},
 orbitInk:{measureText:s=>({width:s.length*6})},globeText:{boxes:[]},globeHits:[]};
 vm.createContext(c);vm.runInContext(source.slice(source.indexOf('const globeGuideAnchors='),source.indexOf('function drawGlobeOverlays(')),c);return c;}
const line=(y=300)=>[{x:-1000,y,depth:1,u:0},{x:1600,y,depth:1,u:1}];
const info={latitude:23.44,name:'Tropic of Cancer',value:'+23.4°'};
{
 const c=context(),runs=c.globeGuideRuns(line());
 assert.equal(runs.length,1);assert.ok(Math.abs(runs[0][0].x-8)<1e-6);assert.ok(Math.abs(runs[0].at(-1).x-792)<1e-6);
 const label=c.globeGuideLayout(info,line());assert.ok(label,'A visible arc remains named even when both samples are offscreen');
 assert.match(label.text,/Tropic of Cancer/);assert.ok(label.samples.every(s=>s.p.y===300),'Text must follow the parallel, without a detached bubble');
 const first=JSON.stringify(label.samples);c.globeText.boxes=[];
 assert.equal(JSON.stringify(c.globeGuideLayout(info,line()).samples),first,'Stable pose retains the same anchor');
}
{
 const c=context();c.globeScene.exclusions=[[250,250,300,100]];
 const label=c.globeGuideLayout(info,line());assert.ok(label,'Move along the same line around an obstruction');
 assert.ok(c.globeText.boxes.every(b=>b.x+b.w<250||b.x>550));
 c.globeText.boxes=[];c.globeScene.exclusions=[[0,0,800,600]];
 assert.equal(c.globeGuideLayout(info,line()),null,'Do not paint over controls');
}
{
 const c=context();c.globeHits=Array.from({length:80},(_,i)=>({x:i*10,y:285,current:false}));
 assert.ok(c.globeGuideLayout(info,line()),'An adjacent orbit of hourly dots must not erase a geographic name');
}
{
 const c=context();assert.equal(c.globeGuideRuns(line().map(p=>({...p,depth:-1}))).length,0,'Do not name hidden back-face parallels');
 c.globeScene.viewport=[0,100,800,200];assert.equal(c.globeGuideRuns(line(400)).length,0,'Respect the real scene viewport');
}
{
 const c=context();const points=Array.from({length:41},(_,i)=>{const x=80+i*16;return {x,y:250+.0004*(x-400)**2,depth:1,u:i/40};});
 const label=c.globeGuideLayout(info,points);assert.ok(label);assert.ok(new Set(label.samples.map(s=>s.p.angle)).size>2,'Lettering bends with the projected parallel');
}
{
 const c=context();const points=Array.from({length:241},(_,i)=>{const t=i*Math.PI/120;return {x:400+150*Math.sin(t),y:300+130*Math.cos(t),depth:1,u:i/240};});
 const label=c.globeGuideLayout(info,points);assert.ok(label);assert.ok(label.samples.every(s=>Math.cos(s.p.angle)>0),'A closed polar circle must not turn its name upside down');
}
{
 const c=context();const points=Array.from({length:241},(_,i)=>{const t=i*Math.PI/120;return {x:400+35*Math.sin(t),y:300+8*Math.cos(t),depth:1,u:i/240};});
 assert.equal(c.globeGuideLayout({latitude:66.56,name:'Arctic polar circle long name',value:'+66.6°'},points),null,'Do not wrap a long name around a tiny ellipse into overlapping letters');
}
console.log('Geographic guide names: high-zoom clipping, curved placement, stable anchors, viewport/control exclusions and hidden hemisphere passed.');

// Pole directions stay on the physical Earth axis, independent of selectable astronomical objects.
{
 const c=context();c.globeScene.guides=true;c.globeScene.poleLabels=[{latitude:90,name:'North Pole',value:'+90°'},{latitude:-90,name:'South Pole',value:'−90°'}];
 c.globePoint=v=>({x:400,y:v[1]>0?150:450,depth:v[1]});
 const guides=c.globePoleGuides();assert.equal(guides.length,1);assert.match(guides[0].label.text,/North Pole/);
 assert.equal(c.globeHits.length,0,'Geographic vertices are not selectable astronomical orbit points');
 c.globeText.boxes=[];c.globePoint=v=>({x:400,y:v[1]>0?150:450,depth:-v[1]});
 assert.match(c.globePoleGuides()[0].label.text,/South Pole/,'Turning Earth reveals the opposite physical pole');
 c.globeScene.guides=false;assert.equal(c.globePoleGuides().length,0,'The existing Guides switch controls poles too');
}
{
 const c=context(),info={latitude:90,name:'North Pole',value:'+90°'},p={x:400,y:100,depth:1};
 c.globeScene.exclusions=[[300,60,200,40]];
 const label=c.globePoleLayout(info,p);assert.ok(label);assert.ok(label.box.y>=110,'Use the clear side near a toolbar');
 c.globeText.boxes=[];c.globeScene.exclusions=[[0,0,800,600]];
 assert.equal(c.globePoleLayout(info,p),null,'Pole captions do not cover controls');
 assert.equal(c.globePoleLayout(info,{x:-5,y:100,depth:1}),null,'No detached edge label for an offscreen pole');
 assert.equal(c.globePoleLayout(info,{x:400,y:100,depth:-1}),null,'No label through the hidden hemisphere');
}
console.log('Geographic pole vertices: front/back hemispheres, guide visibility, control exclusion and nonselectable markers passed.');
