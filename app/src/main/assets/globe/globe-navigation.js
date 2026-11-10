// Interactive Earth view. The trusted native host owns astronomy, selection and the clock.
// No network, DOM navigation, Android bridge or decorative Solar System.
stopWorldAnimation();
scheduleWorldAnimation=function(){};
let globeScene={tracks:[],pose:[0,.3,1],extent:1.55,guides:false};
const orbitCanvas=document.createElement('canvas');document.body.appendChild(orbitCanvas);
const orbitInk=orbitCanvas.getContext('2d');let globeHits=[],globeLabelSegments=[];
const globeLabelSides=new Map(),globeLabelDetails=new Map();
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
 for(const relaxed of (selected?[false,true]:[false]))for(const index of order){const [cx,cy]=candidates[index];
  const box={x:Math.max(4,Math.min(viewWidth-w-4,cx-w/2)),y:Math.max(4,Math.min(viewHeight-h-4,cy-h/2)),w,h};
  const nearestX=Math.max(box.x,Math.min(box.x+w,point.x)),nearestY=Math.max(box.y,Math.min(box.y+h,point.y));
  if(Math.hypot(nearestX-point.x,nearestY-point.y)<radius+24)continue;
  if(globeScene.exclusions?.some(e=>box.x<e[0]+e[2]+4&&box.x+w>e[0]-4&&box.y<e[1]+e[3]+4&&box.y+h>e[1]-4))continue;
  if(globeText.boxes.some(b=>box.x<b.x+b.w+6&&box.x+w>b.x-6&&box.y<b.y+b.h+6&&box.y+h>b.y-6))continue;
  if(!relaxed&&globeHits.some(p=>{const x=Math.max(box.x,Math.min(box.x+w,p.x)),y=Math.max(box.y,Math.min(box.y+h,p.y));
   return Math.hypot(x-p.x,y-p.y)<(p.current?16:7)+6;}))continue;
  // A small hysteresis keeps a clear side stable while the view moves by a few pixels.
  if(!relaxed&&globeLabelSegments.some(s=>globeLineTouchesBox(box,s,index===cached?6:10)))continue;
  globeText.boxes.push(box);globeLabelSides.set(key,index);
  if(globeLabelSides.size>128)globeLabelSides.delete(globeLabelSides.keys().next().value);
  return {box,point,color,selected,lines,radius};
 }return null;
}
// Keep a reduced level stable while the pose is unchanged; gestures and viewport changes recover detail.
function globeAdaptiveText(dot,point,color,key,radius,count){
 const variants=[dot.labelFull||dot.label,dot.labelCompact||dot.label,dot.label];
 const view=globeScene.viewport||[0,0,viewWidth,viewHeight],diameter=earthDiameter(viewWidth,viewHeight);
 const budget=Math.max(1,view[2]*view[3]*.45/Math.max(1,count));
 const area=text=>Math.min(280,Math.max(...text.split('\n').map(line=>line.length*6))+12)*(text.split('\n').length*16+8);
 let level=diameter>=(dot.selected?100:180)&&(dot.selected||area(variants[0])*1.3<=budget)?0:1;
 if(!dot.selected&&area(variants[1])>budget)level=2;
 const signature=[...view,viewWidth,viewHeight,...globeScene.pose.slice(0,3).map(v=>v.toFixed(2)),count,!!dot.selected].join(':');
 const previous=globeLabelDetails.get(key);if(previous?.signature===signature)level=Math.max(level,previous.level);
 const tried=new Set();for(let i=level;i<variants.length;i++){
  const value=variants[i];if(!value||tried.has(value))continue;tried.add(value);
  const label=globeText(value,point,color,!!dot.selected,key,radius);
  if(label){globeLabelDetails.set(key,{level:i,signature});if(globeLabelDetails.size>256)globeLabelDetails.delete(globeLabelDetails.keys().next().value);return label;}
 }
 globeLabelDetails.set(key,{level:2,signature});return null;
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
 lines.forEach((line,i)=>{orbitInk.font=(i===0?'600':'400')+' 12px sans-serif';orbitInk.fillText(line,x+w/2,y+17+i*16,w-8);});
}
// Clip visible front-face arcs to the actual scene, including segments whose samples are off screen.
// Guide names are shaped as whole strings before bending the raster along their parallel (also for RTL).
const globeGuideAnchors=new Map(),globeGuideRasters=new Map();
function globeGuideViewport(){const v=globeScene.viewport||[0,0,viewWidth,viewHeight];
 return {x:Math.max(8,v[0]+8),y:Math.max(20,v[1]+20),right:Math.min(viewWidth-8,v[0]+v[2]-8),bottom:Math.min(viewHeight-8,v[1]+v[3]-8)};
}
function globeGuideRuns(points){const view=globeGuideViewport(),runs=[];let run=[];
 const mix=(a,b,t)=>({x:a.x+(b.x-a.x)*t,y:a.y+(b.y-a.y)*t,u:a.u+(b.u-a.u)*t});
 const finish=()=>{if(run.length>1)runs.push(run);run=[];};
 for(let i=1;i<points.length;i++){let a=points[i-1],b=points[i];
  if(a.depth<0&&b.depth<0){finish();continue;}
  if(a.depth<0)a=mix(a,b,-a.depth/(b.depth-a.depth));
  else if(b.depth<0)b=mix(a,b,a.depth/(a.depth-b.depth));
  const dx=b.x-a.x,dy=b.y-a.y;let first=0,last=1,visible=true;
  for(const [p,q] of [[-dx,a.x-view.x],[dx,view.right-a.x],[-dy,a.y-view.y],[dy,view.bottom-a.y]]){
   if(Math.abs(p)<1e-9){if(q<0)visible=false;continue;}
   const t=q/p;if(p<0)first=Math.max(first,t);else last=Math.min(last,t);
  }
  if(!visible||first>last){finish();continue;}
  const start=mix(a,b,first),end=mix(a,b,last),previous=run.at(-1);
  if(previous&&Math.hypot(start.x-previous.x,start.y-previous.y)>1)finish();
  if(!run.length)run.push(start);run.push(end);
  if(last<1)finish();
 }
 finish();if(runs.length>1){const first=runs[0],last=runs.at(-1);
  if(Math.hypot(last.at(-1).x-first[0].x,last.at(-1).y-first[0].y)<1){runs[0]=last.concat(first.slice(1));runs.pop();}}
 return runs;
}
function globeGuidePath(points){if(points.at(-1).x<points[0].x)points=[...points].reverse();
 const distances=[0];for(let i=1;i<points.length;i++)distances.push(distances.at(-1)+Math.hypot(points[i].x-points[i-1].x,points[i].y-points[i-1].y));
 return {points,distances,length:distances.at(-1)};
}
function globeGuidePoint(path,distance){const {points,distances}=path;let i=1;
 while(i<distances.length-1&&distances[i]<distance)i++;
 const a=points[i-1],b=points[i],t=Math.max(0,Math.min(1,(distance-distances[i-1])/Math.max(.001,distances[i]-distances[i-1])));
 return {x:a.x+(b.x-a.x)*t,y:a.y+(b.y-a.y)*t,u:a.u+(b.u-a.u)*t,angle:Math.atan2(b.y-a.y,b.x-a.x)};
}
function globeGuideLayout(info,points){const paths=globeGuideRuns(points).map(globeGuidePath).sort((a,b)=>b.length-a.length),view=globeGuideViewport();
 const overlaps=(a,b)=>a.x<b.x+b.w+3&&a.x+a.w>b.x-3&&a.y<b.y+b.h+3&&a.y+a.h>b.y-3;
 const exclusions=(globeScene.exclusions||[]).map(([x,y,w,h])=>({x,y,w,h}));
 const cached=globeGuideAnchors.get(info.latitude);
 for(const avoidDots of [true,false])for(const size of [12,11,10])for(const text of [info.name+'  '+info.value,info.name]){
  orbitInk.font='600 '+size+'px sans-serif';const width=orbitInk.measureText(text).width+6;
  for(const path of paths){if(path.length<width+12)continue;
   const low=width/2+5,high=path.length-low,centers=[];
   if(cached!==undefined){for(let i=1;i<path.points.length;i++){const a=path.points[i-1].u,b=path.points[i].u;
    if(Math.abs(b-a)<.1&&cached>=Math.min(a,b)&&cached<=Math.max(a,b)){
     centers.push(Math.max(low,Math.min(high,path.distances[i-1]+(path.distances[i]-path.distances[i-1])*(cached-a)/(b-a||1))));break;}}}
   centers.push(path.length/2);for(let step=32;step<path.length/2;step+=32)centers.push(path.length/2-step,path.length/2+step);
   for(const center of centers){if(center<low||center>high)continue;
    const samples=[],boxes=[],middle=globeGuidePoint(path,center),reverse=Math.cos(middle.angle)<0;let clear=true;
    for(let x=0;x<width;x+=8){const slice=Math.min(8,width-x),offset=-width/2+x+slice/2,p=globeGuidePoint(path,center+(reverse?-offset:offset));
     const turn=Math.atan2(Math.sin(p.angle-middle.angle),Math.cos(p.angle-middle.angle));
     if(Math.abs(turn)>Math.PI/3){clear=false;break;}
     if(reverse)p.angle+=Math.PI;const c=Math.cos(p.angle),s=Math.sin(p.angle);
     const cy=-(size/2+8),cx=p.x-s*cy,y=p.y+c*cy,halfW=Math.abs(c)*slice/2+Math.abs(s)*(size/2+3),halfH=Math.abs(s)*slice/2+Math.abs(c)*(size/2+3);
     const box={x:cx-halfW,y:y-halfH,w:halfW*2,h:halfH*2};
     if(box.x<view.x||box.x+box.w>view.right||box.y<view.y||box.y+box.h>view.bottom||
        exclusions.some(b=>overlaps(box,b))||globeText.boxes.some(b=>overlaps(box,b))||
        boxes.slice(0,-2).some(b=>overlaps(box,b))||
        globeHits.some(p=>{if(!avoidDots&&!p.current)return false;const x=Math.max(box.x,Math.min(box.x+box.w,p.x)),y=Math.max(box.y,Math.min(box.y+box.h,p.y));return Math.hypot(x-p.x,y-p.y)<(p.current?19:p.selected?11:4);})){clear=false;break;}
     boxes.push(box);samples.push({x,slice,p});
    }
    if(clear){globeText.boxes.push(...boxes);globeGuideAnchors.set(info.latitude,globeGuidePoint(path,center).u);return {text,size,width,samples};}
   }
  }
 }
 return null;
}
function drawGlobeGuideName(label){const {text,size,width,samples}=label,key=size+':'+text;
 let raster=globeGuideRasters.get(key);
 if(!raster){const canvas=document.createElement('canvas'),scale=pixelRatio;canvas.width=Math.ceil(width*scale);canvas.height=Math.ceil((size+10)*scale);
  const ink=canvas.getContext('2d');ink.scale(scale,scale);ink.font='600 '+size+'px sans-serif';ink.textAlign='center';ink.textBaseline='alphabetic';
  ink.lineJoin='round';ink.strokeStyle='#101418';ink.lineWidth=3;ink.strokeText(text,width/2,size+2);ink.fillStyle='#d8e7f2';ink.fillText(text,width/2,size+2);
  raster={canvas,scale};globeGuideRasters.set(key,raster);if(globeGuideRasters.size>32)globeGuideRasters.delete(globeGuideRasters.keys().next().value);
 }
 orbitInk.globalAlpha=1;orbitInk.setLineDash([]);
 for(const {x,slice,p} of samples){orbitInk.save();orbitInk.translate(p.x,p.y);orbitInk.rotate(p.angle);
  orbitInk.drawImage(raster.canvas,x*raster.scale,0,slice*raster.scale,raster.canvas.height,-slice/2,-size-10,slice,size+10);orbitInk.restore();}
}

// Geographic poles are the ends of Earth's rotation axis, not the celestial-pole sky directions.
const globePoleSides=new Map();
function globePoleLayout(info,point){const view=globeGuideViewport();
 if(point.depth< -1e-6||point.x<view.x||point.x>view.right||point.y<view.y||point.y>view.bottom)return null;
 const overlaps=(a,b)=>a.x<b.x+b.w+3&&a.x+a.w>b.x-3&&a.y<b.y+b.h+3&&a.y+a.h>b.y-3;
 orbitInk.font='600 12px sans-serif';
 for(const text of [info.name+'  '+info.value,info.name]){const w=orbitInk.measureText(text).width+8,h=20;
  const candidates=[{x:point.x-w/2,y:point.y-h-10,w,h},{x:point.x-w/2,y:point.y+10,w,h},
   {x:point.x+10,y:point.y-h/2,w,h},{x:point.x-w-10,y:point.y-h/2,w,h}];
  const cached=globePoleSides.get(info.latitude),order=cached===undefined?[]:[cached];
  order.push(...candidates.map((_,i)=>i).filter(i=>i!==cached));
  for(const index of order){const b=candidates[index];b.x=Math.max(view.x,Math.min(view.right-w,b.x));
   if(b.x<view.x||b.x+w>view.right||b.y<view.y||b.y+h>view.bottom)continue;
   if((globeScene.exclusions||[]).some(([x,y,w,h])=>overlaps(b,{x,y,w,h}))||globeText.boxes.some(a=>overlaps(b,a)))continue;
   if(globeHits.some(p=>p.current&&Math.hypot(Math.max(b.x,Math.min(b.x+w,p.x))-p.x,Math.max(b.y,Math.min(b.y+h,p.y))-p.y)<19))continue;
   globeText.boxes.push(b);globePoleSides.set(info.latitude,index);return {text,box:b};
  }
 }
 return null;
}
function globePoleGuides(){if(!globeScene.guides)return [];
 return (globeScene.poleLabels||[]).map(info=>{const point=globePoint([0,info.latitude>0?1:-1,0]),view=globeGuideViewport();
  if(point.depth< -1e-6||point.x<view.x||point.x>view.right||point.y<view.y||point.y>view.bottom)return null;
  const label=globePoleLayout(info,point);globeText.boxes.push({x:point.x-6,y:point.y-6,w:12,h:12});
  return {point,label};
 }).filter(Boolean);
}
function drawGlobePoleGuide({point,label}){orbitInk.globalAlpha=1;orbitInk.setLineDash([]);
 orbitInk.beginPath();orbitInk.moveTo(point.x-4,point.y);orbitInk.lineTo(point.x+4,point.y);
 orbitInk.moveTo(point.x,point.y-4);orbitInk.lineTo(point.x,point.y+4);
 orbitInk.strokeStyle='#101418';orbitInk.lineWidth=3.5;orbitInk.stroke();
 orbitInk.strokeStyle='#d8e7f2';orbitInk.lineWidth=1.2;orbitInk.stroke();
 if(label){const {text,box}=label;orbitInk.font='600 12px sans-serif';orbitInk.textAlign='center';orbitInk.textBaseline='alphabetic';
  orbitInk.lineJoin='round';orbitInk.strokeStyle='#101418';orbitInk.lineWidth=3;orbitInk.strokeText(text,box.x+box.w/2,box.y+14);
  orbitInk.fillStyle='#d8e7f2';orbitInk.fillText(text,box.x+box.w/2,box.y+14);}
}

function drawGlobeOverlays(){const d=pixelRatio;if(orbitCanvas.width!==Math.round(viewWidth*d)||orbitCanvas.height!==Math.round(viewHeight*d)){orbitCanvas.width=Math.round(viewWidth*d);orbitCanvas.height=Math.round(viewHeight*d);}
 orbitInk.setTransform(d,0,0,d,0,0);orbitInk.clearRect(0,0,viewWidth,viewHeight);globeHits=[];globeText.boxes=[];globeLabelSegments=[];orbitInk.lineCap='round';
 const guideCurves=[];
 if(globeScene.guides){for(const lat of [-66.56,-23.44,0,23.44,66.56]){const phi=lat*Math.PI/180;let last=null;const points=[];
  for(let i=0;i<=240;i++){const lon=i*Math.PI/120,v=[Math.cos(phi)*Math.sin(lon),Math.sin(phi),Math.cos(phi)*Math.cos(lon)];
   if(last)globeStroke(last,v,'#a8bdd0',.8,false);last=v;
   points.push({...globePoint(v),u:i/240});
  }
  const info=globeScene.guideLabels?.find(g=>Math.abs(g.latitude-lat)<.01);
  if(info?.name)guideCurves.push({info,points});
 }
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
 // Geographic names follow the visible parallel, independently of point-label scope and curve collisions.
 const poleGuides=globePoleGuides();
 const guideNames=guideCurves.map(({info,points})=>globeGuideLayout(info,points)).filter(Boolean);
 // Place every point label before drawing any leader, then paint all pills above the connectors.
 const placed=[];
 const pointLabels=globeScene.labels?globeScene.tracks.flatMap(track=>track.dots.filter(dot=>!!dot.label)
  .map(dot=>({track,dot:{...dot,p:globePoint(dot.v)}})))
  .filter(({dot})=>dot.selected||(dot.p.x>=-20&&dot.p.x<=viewWidth+20&&dot.p.y>=-20&&dot.p.y<=viewHeight+20)):[];
 const labelDots=pointLabels.sort((a,b)=>(b.dot.selected?4:b.dot.current?3:b.dot.guide?2:1)-(a.dot.selected?4:a.dot.current?3:a.dot.guide?2:1));
 for(const {track,dot} of labelDots){
  const p=dot.p||globePoint(dot.v);if(!dot.label)continue;
  const below=track.belowLabelColor||track.belowColor||track.color;
  const tint=p.hidden?below:dot.selected?globeScene.selection:dot.below?below:track.color;
  const key=(track.key||track.color)+':'+dot.id;
  const label=globeAdaptiveText(dot,p,tint,key,dot.current?16:dot.guide?3:7,labelDots.length);
  if(label){label.behind=p.hidden;placed.push(label);}
 }
 placed.forEach(globeLabelLeader);guideNames.forEach(drawGlobeGuideName);placed.forEach(drawGlobeText);poleGuides.forEach(drawGlobePoleGuide);
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
