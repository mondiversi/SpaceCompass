/* Runtime regressions for the actual offline Globe label code, without a browser or network. */
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm'),assert=require('node:assert/strict');
const root=process.argv[2]||path.resolve(__dirname,'..');
const source=fs.readFileSync(path.join(root,'app/src/main/assets/globe/globe-navigation.js'),'utf8');
function context(){
 const c={viewWidth:800,viewHeight:600,liveDiameter:300,globeScene:{pose:[0,.3,1],viewport:[0,0,800,600],exclusions:[]},
  orbitInk:{measureText:t=>({width:t.length*6})},globeLabelSides:new Map(),globeLabelDetails:new Map(),
  globeHits:[],globeLabelSegments:[]};
 c.earthDiameter=()=>c.liveDiameter;
 vm.createContext(c);vm.runInContext(source.slice(source.indexOf('function globeLineTouchesBox('),source.indexOf('function globeLabelLeader(')),c);
 c.globeText.boxes=[];return c;
}
{
 const c=context();
 for(let y=0;y<=600;y+=10)c.globeLabelSegments.push([{x:0,y},{x:800,y}]);
 const p={x:400,y:300};
 assert.equal(c.globeText('14:00 · T−1h0m',p,'#fff',false,'ordinary',7),null);
 const selected=c.globeText('14:00 · T−1h0m',p,'#fff',true,'selected',7);
 assert.ok(selected,'Selected label must survive nearby curves');
 c.globeText.boxes=[];
 const stable=c.globeText('14:00 · T−1h0m',p,'#fff',true,'selected',7);
 assert.equal(JSON.stringify(stable.box),JSON.stringify(selected.box));
 c.globeText.boxes=[];c.globeScene.exclusions=[[0,0,800,600]];
 assert.equal(c.globeText('Selected',p,'#fff',true,'blocked',7),null,'Never cover controls');
}
{
 const c=context(),requests=[];
 c.globeText=(text,p,color,selected,key,radius)=>{requests.push(text);return {lines:[text],box:{x:0,y:0,w:50,h:30},point:p,color,radius};};
 const dot={selected:false,labelFull:'Sun · Rise\n14:00 · T−1h0m\n↔ 100° · ↕ +1°',labelCompact:'Rise\n14:00 · T−1h0m',label:'Rise · 14:00 · T−1h0m'};
 const draw=count=>c.globeAdaptiveText(dot,{x:400,y:300},'#fff','point',7,count);
 draw(1);assert.equal(requests.at(-1),dot.labelFull);
 c.liveDiameter=70;c.globeScene.pose[2]=.5;draw(1);assert.equal(requests.at(-1),dot.labelCompact);
 c.liveDiameter=300;c.globeScene.pose[2]=2;draw(1);assert.equal(requests.at(-1),dot.labelFull,'Zooming back must restore full details');
 c.globeScene.pose[2]=3;draw(1000);assert.equal(requests.at(-1),dot.label,'Crowding must retain clock/T in the minimal tier');
}
{
 const c=context(),order=[];c.guideLabels=[];
 c.globeScene.labels=true;c.globeScene.selection='#0ff';
 c.globeScene.tracks=[{key:'a',color:'#fff',dots:[
  {id:1,label:'ordinary',current:false,selected:false,v:[0,0,1]},
  {id:4,label:'offscreen-ordinary',current:false,selected:false,v:[99,0,1]},
  {id:2,label:'current',current:true,selected:false,v:[0,0,1]}]},
  {key:'b',color:'#fc0',dots:[{id:3,label:'selected-other-object',current:false,selected:true,v:[99,0,1]}]}];
 c.globePoint=v=>({x:v[0]===99?-500:400,y:300,hidden:false});c.globeAdaptiveText=dot=>{order.push(dot.label);return null;};
 const a=source.indexOf(' const placed=[];'),b=source.indexOf(' placed.forEach(globeLabelLeader)',a);
 vm.runInContext('(()=>{'+source.slice(a,b)+'})()',c);
 assert.deepEqual(order,['selected-other-object','current','ordinary']);
}
console.log('Globe labels: selected-point fallback, control exclusion, stable placement, adaptive detail recovery and cross-track priority passed.');
