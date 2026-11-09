// Interactive Earth view. The trusted native host owns astronomy, selection and the clock.
// No network, DOM navigation, Android bridge or decorative Solar System.
stopWorldAnimation();
scheduleWorldAnimation=function(){};
let globeScene={tracks:[],pose:[0,.3,1],extent:1.55,guides:false};
const orbitCanvas=document.createElement('canvas');document.body.appendChild(orbitCanvas);
const orbitInk=orbitCanvas.getContext('2d');let globeHits=[],globeLabelSegments=[];
const globeLabelSides=new Map();
earthPose=function(){return {rotation:globeScene.pose[0],orientation:[globeScene.pose[3]||0,globeScene.pose[1]]}};
window.spaceCompassGlobeCenter=function(w,h){const v=globeScene.viewport||[0,0,w,h];return [w/2,v[1]+v[3]/2];};
// 1x fits the entire rotation-invariant trajectory envelope inside the available frame.
// Keep the Earth centre fixed and reserve room for current-object miniatures at the edge.
earthDiameter=function(w,h){const v=globeScene.viewport||[0,0,w,h];
 return Math.max(1,Math.min(v[2],v[3])/2-24)*2/Math.max(1,globeScene.extent)*globeScene.pose[2]};
function globePoint(v){ const p=earthViewVector(v,globeScene.pose[0],[globeScene.pose[3]||0,globeScene.pose[1]]),r=earthDiameter(viewWidth,viewHeight)/2,center=spaceCompassGlobeCenter(viewWidth,viewHeight);
 return {x:center[0]+p.x*r,y:center[1]-p.y*r,depth:p.depth,hidden:p.depth<0&&p.x*p.x+p.y*p.y<1};}
function globeStroke(a,b,color,width,dashed){const p=globePoint(a),q=globePoint(b);
 if(globeScene.labels&&(!p.hidden||!q.hidden))globeLabelSegments.push([p,q]);
 orbitInk.globalAlpha=p.hidden&&q.hidden ? .17 : 1;orbitInk.strokeStyle=color;orbitInk.lineWidth=width;
 orbitInk.setLineDash(dashed||p.hidden&&q.hidden?[4,4]:[]);orbitInk.beginPath();orbitInk.moveTo(p.x,p.y);orbitInk.lineTo(q.x,q.y);orbitInk.stroke();}
// Clip a segment against a padded label rectangle, after a cheap bounding-box rejection.
function globeLineTouchesBox(box,segment,pad){
 const [a,b]=segment,l=box.x-pad,r=box.x+box.w+pad,t=box.y-pad,bottom=box.y+box.h+pad;
 if(Math.max(a.x,b.x)<l||Math.min(a.x,b.x)>r||Math.max(a.y,b.y)<t||Math.min(a.y,b.y)>bottom)return false;
 const dx=b.x-a.x,dy=b.y-a.y;let first=0,last=1;
 for(const [p,q] of [[-dx,a.x-l],[dx,r-a.x],[-dy,a.y-t],[dy,bottom-a.y]]){
  if(Math.abs(p)<1e-9){if(q<0)return false;continue;}
  const v=q/p;if(p<0){if(v>last)return false;first=Math.max(first,v)}else{if(v<first)return false;last=Math.min(last,v)}
 }return true;
}
function globeText(label,point,color,selected,key,radius){if(!label)return null;
 orbitInk.font='600 12px sans-serif';const limit=Math.min(280,viewWidth-24),lines=[];
 for(const paragraph of label.split('\n')){let line='';for(const word of paragraph.split(/\s+/)){
  const next=line?line+' '+word:word;if(orbitInk.measureText(next).width>limit-12&&line){lines.push(line);line=word}else line=next;
 }lines.push(line);}
 const w=Math.min(Math.max(...lines.map(v=>orbitInk.measureText(v).width))+12,viewWidth-16),h=lines.length*16+8;
 if(h>viewHeight-8)return null;
 const candidates=[];
 for(const extra of [28,44,64,88,116,148]){
  const gap=radius+extra;
  candidates.push([point.x,point.y-gap-h/2],[point.x,point.y+gap+h/2],
   [point.x-gap-w/2,point.y],[point.x+gap+w/2,point.y],
   [point.x+gap/Math.SQRT2+w/2,point.y-gap/Math.SQRT2-h/2],
   [point.x-gap/Math.SQRT2-w/2,point.y-gap/Math.SQRT2-h/2],
   [point.x+gap/Math.SQRT2+w/2,point.y+gap/Math.SQRT2+h/2],
   [point.x-gap/Math.SQRT2-w/2,point.y+gap/Math.SQRT2+h/2]);
 }
 const cached=globeLabelSides.get(key),order=cached===undefined?[]:[cached];
 order.push(...candidates.map((_,i)=>i).filter(i=>i!==cached));
 for(const index of order){const [cx,cy]=candidates[index];
  const box={x:Math.max(4,Math.min(viewWidth-w-4,cx-w/2)),y:Math.max(4,Math.min(viewHeight-h-4,cy-h/2)),w,h};
  const nearestX=Math.max(box.x,Math.min(box.x+w,point.x)),nearestY=Math.max(box.y,Math.min(box.y+h,point.y));
  if(Math.hypot(nearestX-point.x,nearestY-point.y)<radius+24)continue;
  if(globeScene.exclusions?.some(e=>box.x<e[0]+e[2]+4&&box.x+w>e[0]-4&&box.y<e[1]+e[3]+4&&box.y+h>e[1]-4))continue;
  if(globeText.boxes.some(b=>box.x<b.x+b.w+6&&box.x+w>b.x-6&&box.y<b.y+b.h+6&&box.y+h>b.y-6))continue;
  if(globeHits.some(p=>{const x=Math.max(box.x,Math.min(box.x+w,p.x)),y=Math.max(box.y,Math.min(box.y+h,p.y));
   return Math.hypot(x-p.x,y-p.y)<(p.current?16:7)+6;}))continue;
  // A small hysteresis keeps a clear side stable while the view moves by a few pixels.
  if(globeLabelSegments.some(s=>globeLineTouchesBox(box,s,index===cached?6:10)))continue;
  globeText.boxes.push(box);globeLabelSides.set(key,index);
  if(globeLabelSides.size>128)globeLabelSides.delete(globeLabelSides.keys().next().value);
  return {box,point,color,selected,lines,radius};
 }return null;
}
function globeLabelLeader(label){const {box,point,color,radius}=label;
 const x=Math.max(box.x,Math.min(box.x+box.w,point.x)),y=Math.max(box.y,Math.min(box.y+box.h,point.y));
 const dx=x-point.x,dy=y-point.y,length=Math.max(1,Math.hypot(dx,dy));
 orbitInk.globalAlpha=label.behind?.6:.85;orbitInk.setLineDash(label.behind?[3,3]:[]);orbitInk.strokeStyle=color;orbitInk.lineWidth=.8;
 orbitInk.beginPath();orbitInk.moveTo(point.x+dx/length*(radius+3),point.y+dy/length*(radius+3));orbitInk.lineTo(x,y);orbitInk.stroke();
}
function drawGlobeText(label){const {box,color,selected,lines}=label,{x,y,w,h}=box;
 orbitInk.globalAlpha=1;orbitInk.setLineDash([]);orbitInk.fillStyle='#101418d9';
 const radius=4;orbitInk.beginPath();orbitInk.moveTo(x+radius,y);
 orbitInk.lineTo(x+w-radius,y);orbitInk.quadraticCurveTo(x+w,y,x+w,y+radius);
 orbitInk.lineTo(x+w,y+h-radius);orbitInk.quadraticCurveTo(x+w,y+h,x+w-radius,y+h);
 orbitInk.lineTo(x+radius,y+h);orbitInk.quadraticCurveTo(x,y+h,x,y+h-radius);
 orbitInk.lineTo(x,y+radius);orbitInk.quadraticCurveTo(x,y,x+radius,y);orbitInk.closePath();
 orbitInk.fill();orbitInk.strokeStyle=color;orbitInk.lineWidth=selected?1.5:1;orbitInk.stroke();
 orbitInk.font='600 12px sans-serif';orbitInk.fillStyle='#fff';orbitInk.textAlign='center';
 lines.forEach((line,i)=>orbitInk.fillText(line,x+w/2,y+17+i*16,w-8));
}
function drawGlobeOverlays(){const d=pixelRatio;if(orbitCanvas.width!==Math.round(viewWidth*d)||orbitCanvas.height!==Math.round(viewHeight*d)){orbitCanvas.width=Math.round(viewWidth*d);orbitCanvas.height=Math.round(viewHeight*d);}
 orbitInk.setTransform(d,0,0,d,0,0);orbitInk.clearRect(0,0,viewWidth,viewHeight);globeHits=[];globeText.boxes=[];globeLabelSegments=[];orbitInk.lineCap='round';
 if(globeScene.guides){for(const lat of [-66.56,-23.44,0,23.44,66.56]){const phi=lat*Math.PI/180;let last=null;
  for(let i=0;i<=120;i++){const lon=i*Math.PI/60,v=[Math.cos(phi)*Math.sin(lon),Math.sin(phi),Math.cos(phi)*Math.cos(lon)];
   if(last)globeStroke(last,v,'#a8bdd0',.8,false);last=v;}}
  for(let lon=0;lon<180;lon+=30){const l=lon*Math.PI/180;let last=null;for(let i=0;i<=120;i++){
   const phi=i*Math.PI/60,v=[Math.cos(phi)*Math.sin(l),Math.sin(phi),Math.cos(phi)*Math.cos(l)];
   if(last)globeStroke(last,v,'#7892ab',.5,false);last=v;}}}
 for(const track of globeScene.tracks){for(let i=1;i<track.curve.length;i++)globeStroke(track.curve[i-1][0],track.curve[i][0],track.curve[i][1]?(track.belowColor||track.color):track.color,1.6,track.curve[i][1]||!!track.comparison);
  for(const dot of [...track.dots].sort((a,b)=>Number(a.current)-Number(b.current))){const p=globePoint(dot.v);if(p.hidden)continue;
   const tint=dot.selected?globeScene.selection:dot.below?(track.belowColor||track.color):track.color;
   orbitInk.globalAlpha=1;orbitInk.setLineDash([]);orbitInk.strokeStyle=tint;orbitInk.lineWidth=dot.selected?1.8:1;
   if(dot.current&&!dot.antipode){
    const image=globeImage(track.icon),radius=16;
    orbitInk.save();orbitInk.beginPath();orbitInk.arc(p.x,p.y,radius,0,Math.PI*2);orbitInk.clip();
    orbitInk.fillStyle='#000b';orbitInk.fillRect(p.x-radius,p.y-radius,radius*2,radius*2);
    if(image){orbitInk.globalAlpha=dot.below ? .7 : 1;orbitInk.filter=dot.below?'grayscale(1)':'none';orbitInk.drawImage(image,p.x-radius,p.y-radius,radius*2,radius*2);}
    orbitInk.restore();orbitInk.strokeStyle=tint;orbitInk.lineWidth=dot.selected?1.8:.8;
    orbitInk.beginPath();orbitInk.arc(p.x,p.y,radius,0,Math.PI*2);orbitInk.stroke();
   }else if(dot.event&&dot.event!=='HOUR'&&!dot.antipode){globeEventMarker(dot,p,tint)}
   else {orbitInk.fillStyle=dot.antipode?'transparent':tint;
    orbitInk.beginPath();orbitInk.arc(p.x,p.y,dot.selected?5:2.8,0,Math.PI*2);if(!dot.antipode)orbitInk.fill();orbitInk.stroke();}
   if(dot.selected&&!dot.current){orbitInk.beginPath();orbitInk.arc(p.x,p.y,9,0,Math.PI*2);orbitInk.stroke();}
   if(dot.id>=0)globeHits.push({id:dot.id,x:p.x,y:p.y,depth:p.depth,current:dot.current});
  }}
 // Place every label before drawing any leader, then paint all pills above the connectors.
 const placed=[];
 if(globeScene.labels)for(const track of globeScene.tracks)for(const dot of [...track.dots].sort((a,b)=>Number(b.selected)-Number(a.selected)||Number(b.current)-Number(a.current))){
  const p=globePoint(dot.v);if(!dot.label)continue;
  const below=track.belowLabelColor||track.belowColor||track.color;
  const tint=p.hidden?below:dot.selected?globeScene.selection:dot.below?below:track.color;
  const label=globeText(dot.label,p,tint,dot.selected,track.color+':'+dot.id,dot.current?16:7);
  if(label){label.behind=p.hidden;placed.push(label);}
 }
 placed.forEach(globeLabelLeader);placed.forEach(drawGlobeText);
 orbitInk.globalAlpha=1;
}
paintWorld=function(time){lastGlobePaint=performance.now();globeFrameNow=performance.now();applyGlobeLayout();const w=viewWidth,h=viewHeight;if(!w||!h)return;
 world.setTransform(pixelRatio,0,0,pixelRatio,0,0);world.clearRect(0,0,w,h);
 if(globeScene.background!==false){world.fillStyle=nativeNight?'#02050c':'#15253b';world.fillRect(0,0,w,h);
  for(const star of stars){world.fillStyle='#abc4dd55';world.beginPath();world.arc(star.x*w,star.y*h,star.r*.6,0,Math.PI*2);world.fill();}}
 if(earthRenderer&&earthRenderer.available)earthRenderer.render(time,w,h,pixelRatio);
 else if(earthSoftwareRenderer)earthSoftwareRenderer.render(time);
 drawEarthLocation(time,w,h,earthPose(time));drawGlobeOverlays();};
window.setGlobeScene=function(scene){globeScene=scene;nativeLocation=scene.location;nativeSunDirection=scene.sun;nativeNight=scene.dark;
 const background=scene.background===false?'transparent':spacePalettes[nativeNight?'dark':'light'][2];
 document.documentElement.style.background=background;document.body.style.background=background;resizeWorld();};
window.setGlobePose=function(pose){globeScene.pose=pose;requestGlobePaint();};
window.pickGlobePoint=function(x,y){
 // Visible current-object miniatures own their actual disc, even when slow-motion event dots coincide.
 const current=globeHits.filter(p=>p.current).map(p=>({p,d:(p.x-x)**2+(p.y-y)**2})).filter(v=>v.d<=16*16).sort((a,b)=>a.d-b.d)[0];
 if(current)return current.p.id;
 let chosen=null,best=22*22;for(const p of globeHits){const distance=(p.x-x)**2+(p.y-y)**2;
 if(distance<best){chosen=p;best=distance;}}return chosen?chosen.id:-1;};
// Android's embedded WebView can report a zero-height initial CSS containing block.
// Size each scene canvas explicitly from the actual viewport, including after panel/rotation changes.
let lastGlobePaint=-1000,globePaintTimer=0,globePaintFrame=0;
function requestGlobePaint(){
 if(document.hidden||globePaintTimer||globePaintFrame)return;
 const delay=Math.max(0,(globeScene.frameInterval||30)-(performance.now()-lastGlobePaint));
 globePaintTimer=setTimeout(()=>{globePaintTimer=0;globePaintFrame=requestAnimationFrame(()=>{
  globePaintFrame=0;if(!document.hidden)paintWorld(performance.now()/1000);
 });},delay);
}
document.addEventListener('visibilitychange',()=>{if(document.hidden){clearTimeout(globePaintTimer);cancelAnimationFrame(globePaintFrame);globePaintTimer=globePaintFrame=0;}});
const globeImages=new Map();
function globeImage(source){if(!source)return null;let image=globeImages.get(source);if(!image){
 image=new Image();globeImages.set(source,image);image.onload=requestGlobePaint;image.src=source;
}return image.complete&&image.naturalWidth?image:null;}
function globeEventMarker(dot,p,tint){
 const radius=7;orbitInk.fillStyle='#0007';orbitInk.beginPath();orbitInk.arc(p.x,p.y,radius+1,0,Math.PI*2);orbitInk.fill();
 orbitInk.fillStyle=tint;orbitInk.beginPath();orbitInk.arc(p.x,p.y,radius,0,Math.PI*2);orbitInk.fill();
 orbitInk.fillStyle='#111';orbitInk.strokeStyle='#111';orbitInk.lineWidth=1.2;const d=3;
 if(dot.event==='CULMINATION'||dot.event==='MINIMUM'){
  orbitInk.beginPath();orbitInk.moveTo(p.x-d,p.y);orbitInk.lineTo(p.x+d,p.y);
  if(dot.event==='CULMINATION'){orbitInk.moveTo(p.x,p.y-d);orbitInk.lineTo(p.x,p.y+d);}orbitInk.stroke();
 }else if(dot.glyph){orbitInk.beginPath();dot.glyph.forEach(([x,y],i)=>{if(i)orbitInk.lineTo(p.x+x*d,p.y+y*d);else orbitInk.moveTo(p.x+x*d,p.y+y*d);});orbitInk.closePath();orbitInk.fill();}
 else if(dot.event==='APPROACH'){orbitInk.beginPath();orbitInk.arc(p.x-1.5,p.y,2.5,0,Math.PI*2);orbitInk.stroke();orbitInk.beginPath();orbitInk.arc(p.x+1.5,p.y,2.5,0,Math.PI*2);orbitInk.stroke();}
}
function applyGlobeLayout(){const w=innerWidth,h=innerHeight;
 document.documentElement.style.height=h+'px';document.body.style.height=h+'px';
 for(const canvas of [worldCanvas,document.getElementById('earth-canvas'),orbitCanvas]){canvas.style.width=w+'px';canvas.style.height=h+'px';}
 const fallback=document.getElementById('earth-fallback'),diameter=earthDiameter(w,h),center=spaceCompassGlobeCenter(w,h);
 fallback.style.left=center[0]+'px';fallback.style.top=center[1]+'px';fallback.style.width=diameter+'px';fallback.style.height=diameter+'px';
}
const resizeGlobeBase=resizeWorld;
resizeWorld=function(){applyGlobeLayout();if(viewWidth!==innerWidth||viewHeight!==innerHeight)resizeGlobeBase();else requestGlobePaint();};
resizeWorld();
