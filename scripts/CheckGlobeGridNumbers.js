/* Exercise the actual shared live flat-map numbering without browser or network dependencies. */
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm'),assert=require('node:assert/strict');
const root=process.argv[2]||path.resolve(__dirname,'..');
const source=fs.readFileSync(path.join(root,'app/src/main/assets/globe/globe-navigation.js'),'utf8');
const slice=(a,b)=>source.slice(source.indexOf(a),source.indexOf(b,source.indexOf(a)));
function context(width,height){
 const c={Math,Set,viewWidth:width,viewHeight:height,globeScene:{flat:true,pose:[0,0,1,0],viewport:[0,0,width,height],exclusions:[],grid:true,gridNumbers:true},
  globeHits:[],globeCoordinateCurves:[],drawn:[],orbitInk:{font:'',save(){},restore(){},measureText(text){return {width:text.length*4.5}},strokeText(){},fillText(text,x,y){c.drawn.push({text,x,y,width:text.length*4.5})}},
  globeText:{boxes:[]},earthViewVector:()=>{throw Error('Flat numbering must not use spherical geometry')}};
 vm.createContext(c);
 vm.runInContext(slice('function globePoint(','function globeStroke(')+slice('function globeGuideViewport(','function globeGuideRuns(')+
  slice('function globeFlatRect(','let globeMapTexture=null,')+
  source.slice(source.indexOf('function globeFlatGridNumberRows('),source.indexOf('\n}\n',source.indexOf('function drawGlobeGridNumbers('))+3),c);
 c.render=(labels)=>{c.drawn.length=0;c.globeText.boxes=[];c.globeScene.coordinateLabels=labels;c.drawGlobeGridNumbers();return c.drawn};
 return c;
}
const vector=(longitude,latitude)=>{const a=longitude*Math.PI/180,b=latitude*Math.PI/180;return [Math.cos(b)*Math.sin(a),Math.sin(b),Math.cos(b)*Math.cos(a)]};
const near=(actual,expected)=>assert.ok(Math.abs(actual-expected)<1e-7,`${actual} != ${expected}`);
const box=p=>({x:p.x-p.width/2-2,y:p.y-6,w:p.width+4,h:12});
const intersects=(a,b)=>a.x<b.x+b.w+3&&a.x+a.w>b.x-3&&a.y<b.y+b.h+3&&a.y+a.h>b.y-3;
let cases=0;
for(const sky of [false,true])for(const [width,height] of [[400,600],[800,380]])for(const zoom of [.5,1,2,4,8])for(const controls of [false,true]){
 const c=context(width,height);c.globeScene.sky=sky;c.globeScene.skyCenter=180;c.globeScene.pose=[.17,.1,zoom,0];
 c.globeScene.exclusions=controls?[[0,0,60,64],[width-60,0,60,64],[width/2-105,0,210,64]]:[];
 const labels=Array.from({length:24},(_,i)=>{const degrees=-180+i*15;return {latitude:false,degrees,text:sky?((degrees+360)%360)+'°':Math.abs(degrees)+'°'+(degrees<0?'W':degrees>0?'E':'')}});
 const rendered=c.render(labels),view=c.globeGuideViewport(),rect=c.globeFlatRect(),middle=(Math.max(view.y,rect.y)+Math.min(view.bottom,rect.y+rect.h))/2;
 const top=rendered.filter(p=>p.y<middle);assert.ok(top.length>0,'Visible meridians keep upper numbers at every zoom, even under controls');
 if(controls&&zoom>=4)for(const p of top){const b=box(p);
  if(c.globeScene.exclusions.some(([x,y,w,h])=>b.x<x+w+3&&b.x+b.w>x-3))assert.ok(p.y>=73,'Use a clear band beneath a blocking toolbar');
 }
 for(const p of rendered){
  const b=box(p);assert.ok(b.x>=view.x&&b.x+b.w<=view.right&&b.y>=view.y&&b.y+b.h<=view.bottom);
  assert.ok(!c.globeScene.exclusions.some(([x,y,w,h])=>intersects(b,{x,y,w,h})),'Numbers never cover controls');
  const info=labels.find(info=>info.text===p.text);near(p.x,c.globePoint(vector(info.degrees,0)).x);
  assert.ok(rendered.filter(q=>q.text===p.text&&(q.y<middle)===(p.y<middle)).length===1,'One number per coordinate and edge');
 }
 for(let i=0;i<rendered.length;i++)for(let j=i+1;j<rendered.length;j++)assert.ok(!intersects(box(rendered[i]),box(rendered[j])),'Numbers do not collide');
 cases++;
}
for(const zoom of [.5,1,4,8]){
 const c=context(400,600);c.globeScene.pose=[0,0,zoom,0];
 const rect=c.globeFlatRect(),view=c.globeGuideViewport();
 const labels=[{latitude:true,degrees:0,text:'0°'},{latitude:true,degrees:30,text:'30°N'},{latitude:true,degrees:-30,text:'30°S'}];
 const rendered=c.render(labels);assert.equal(rendered.length,6);
 for(const info of labels){const pair=rendered.filter(p=>p.text===info.text).sort((a,b)=>a.x-b.x);
  const left=Math.max(view.x,rect.x+8),right=Math.min(view.right,rect.x+rect.w-8);
  near(pair[0].x-pair[0].width/2,left+2);near(pair[1].x+pair[1].width/2,right-2);
  near(pair[0].y,c.globePoint(vector(0,info.degrees)).y);near(pair[1].y,pair[0].y);
 }
}
{
 const c=context(400,600),labels=[{latitude:false,degrees:0,text:'0°'}];c.globeScene.pose=[0,0,4,0];
 c.globeScene.exclusions=[[0,0,400,600]];assert.equal(c.render(labels).length,0,'Completely blocked views do not force text over controls');
 c.globeScene.exclusions=[];c.globeScene.grid=false;assert.equal(c.render(labels).length,0);
 c.globeScene.grid=true;c.globeScene.gridNumbers=false;assert.equal(c.render(labels).length,0);
 c.globeScene.gridNumbers=true;c.globeText.boxes=[{x:0,y:20,w:400,h:40}];c.globeScene.coordinateLabels=labels;c.drawn=[];c.drawGlobeGridNumbers();
 assert.ok(c.drawn.every(p=>p.y>60),'Existing object captions retain priority');
}
// Native layout divided by WebView density produces fractional control edges.
for(let i=0;i<120;i++){
 const c=context(400,600);c.globeScene.pose=[0,0,4,0];
 c.globeScene.exclusions=[[0,0,400,48+i*.1377]];
 assert.ok(c.render([{latitude:false,degrees:0,text:'0°'}]).some(p=>p.y<300),'Fractional toolbar edges retain a clear upper value');
}
// Native optical insets and extended-island side panels do not swallow latitude values.
for(const [leftBand,rightBand] of [[12.3636,12.3636],[12.3636,170.25],[12.3636,197.25]]){
 const c=context(400,600);c.globeScene.pose=[0,0,4,0];
 c.globeScene.exclusions=[[0,0,leftBand,600],[400-rightBand,0,rightBand,600]];
 const labels=[{latitude:true,degrees:0,text:'0°'},{latitude:true,degrees:30,text:'30°N'},{latitude:true,degrees:-30,text:'30°S'}];
 const rendered=c.render(labels);assert.equal(rendered.length,6,'Both latitude columns survive native safety bands');
 for(const info of labels){const pair=rendered.filter(p=>p.text===info.text).sort((a,b)=>a.x-b.x);
  near(pair[0].x-pair[0].width/2,leftBand+6);near(pair[1].x+pair[1].width/2,400-rightBand-6);
  near(pair[0].y,c.globePoint(vector(0,info.degrees)).y);near(pair[1].y,pair[0].y);
  for(const p of pair)assert.ok(!c.globeScene.exclusions.some(([x,y,w,h])=>intersects(box(p),{x,y,w,h})));
 }
}
// Short control groups do not pull side values unnecessarily toward the map centre.
{
 const c=context(400,600);c.globeScene.pose=[0,0,4,0];
 c.globeScene.exclusions=[[0,0,60,64],[340,0,60,64]];
 assert.equal(c.render([{latitude:true,degrees:60,text:'60°N'}]).length,0);
}
console.log(`Live flat-map grid numbers: ${cases} Sky/Globe orientation/zoom/control cases, visible top bands, aligned meridians, compact symmetric side margins and exclusions passed.`);
