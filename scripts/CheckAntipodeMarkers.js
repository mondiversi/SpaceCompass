const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict'),path=require('node:path');
const root=path.resolve(__dirname,'..');
const source=fs.readFileSync(path.join(root,'app/src/main/assets/globe/globe-navigation.js'),'utf8');
const ops=[];
const ink={beginPath(){},closePath(){},moveTo(...v){ops.push({op:'move',v})},lineTo(...v){ops.push({op:'line',v})},
 arc(...v){ops.push({op:'arc',v})},fill(){ops.push({op:'fill',ink:this.fillStyle})},
 stroke(){ops.push({op:'stroke',ink:this.strokeStyle})},fillRect(...v){ops.push({op:'plate',ink:this.fillStyle,v})},
 drawImage(...v){ops.push({op:'image',v})},save(){},restore(){},clip(){},setTransform(){},clearRect(){},setLineDash(){}};
const image={identity:'original-object-sprite'};
const c={Math,orbitInk:ink,orbitCanvas:{width:800,height:400},pixelRatio:1,viewWidth:800,viewHeight:400,
 globeText:{},globePoint:()=>({x:100,y:90,depth:1}),globeImage:()=>image,
 drawGlobeCoordinateGrid(){},drawGlobeGridNumbers(){},globePoleGuides:()=>[],drawGlobeFlatTrack(){},
 drawGlobeFlatLocation(){},drawGlobeFlatReticle(){},globeLabelLeader(){},drawGlobeGuideName(){},drawGlobeText(){},drawGlobePoleGuide(){}};
vm.createContext(c);
vm.runInContext(source.slice(source.indexOf('function globeEventMarker('),source.indexOf('// More detail only')),c);
vm.runInContext(source.slice(source.indexOf('function drawGlobeOverlays('),source.indexOf('paintWorld=function')),c);
const tint='#29c2e6';
function draw(dot,flat=false){
 ops.length=0;
 c.globeScene={flat,selection:tint,labels:false,tracks:[{icon:'same-sprite',color:tint,curve:[],dots:[{v:[1,0,0],id:123,selected:true,...dot}]}]};
 c.drawGlobeOverlays();return [...ops];
}
for(const flat of [false,true])for(const event of ['SUNRISE','SUNSET','CULMINATION','MINIMUM','APPROACH']){
 const direction=event==='SUNRISE'?-1:1;
 const glyph=Array.from({length:25},(_,i)=>{const a=Math.PI*i/24;return [Math.cos(a),direction*(Math.sin(a)-.5)]});
 const dot={event,current:false,glyph:['SUNRISE','SUNSET'].includes(event)?glyph:undefined};
 const original=draw({...dot,antipode:false},flat),antipode=draw({...dot,antipode:true},flat);
 assert.deepEqual(antipode.filter(x=>['move','line'].includes(x.op)),original.filter(x=>['move','line'].includes(x.op)),event+' changes its source glyph');
 assert.ok(antipode.some(x=>x.op==='arc'&&x.v[2]===7),event+' is missing its source marker');
 assert.ok(!antipode.some(x=>x.op==='fill'&&['#0007','#111'].includes(x.ink)),event+' has an opaque dark plate');
 if(event==='APPROACH')assert.equal(antipode.filter(x=>x.op==='arc'&&x.v[2]===2.5).length,2);
 assert.equal(c.globeHits[0].id,123);assert.equal(c.globeHits[0].current,false);
}
for(const flat of [false,true]){
 const hour=draw({event:'HOUR',current:false,antipode:true},flat);
 assert.ok(hour.some(x=>x.op==='arc'&&x.v[2]===5));assert.ok(!hour.some(x=>x.op==='fill'));
 const current=draw({event:'HOUR',current:true,antipode:true},flat);
 assert.equal(current.filter(x=>x.op==='image').length,1);assert.equal(current.find(x=>x.op==='image').v[0],image);
 assert.ok(!current.some(x=>x.op==='plate'));assert.equal(c.globeHits[0].current,true);
}
console.log('Antipode marker checks passed: inherited rise/set, maximum/minimum and approach glyphs, ordinary markers and current sprites in spherical/flat views; transparent plates and hit identities preserved.');
