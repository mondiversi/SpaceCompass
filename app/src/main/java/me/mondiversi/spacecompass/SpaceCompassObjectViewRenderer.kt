package me.mondiversi.spacecompass

/** Adapt only the interactive host. The standalone screensaver and its pinned source stay unchanged. */
internal fun spaceCompassObjectRendererHtml(source: String): String {
    var html=source
    fun replace(old: String,new: String) { require(old in html);html=html.replace(old,new) }
    replace("precision mediump float;uniform vec2 resolution", "precision highp float;uniform float objectView,objectFocal;uniform vec3 objectEye,objectTarget,objectFraming;uniform vec2 resolution")
    val start="float d=dot(q,q);vec3 sun=normalize(sunlight);"
    val end="float roll=axisOrientation.x,pitch=axisOrientation.y;"
    val from=html.indexOf(start);val to=html.indexOf(end,from)
    require(from>=0&&to>from)
    val ordinary=html.substring(from+start.length,to).replace("vec3 n=vec3(q,sqrt(1.-d));","n=vec3(q,sqrt(1.-d));")
    val branch="""float d=dot(q,q);vec3 sun=normalize(sunlight);vec3 n;
if(objectView<.5){$ordinary}else if(objectView<1.5){
vec2 p=q+objectTarget.xy;float h=1.-dot(p,p);if(h<0.)discard;n=vec3(p,sqrt(h));
}else{
vec2 pixel=q/objectFraming.z+objectFraming.xy;
vec3 ray=normalize(vec3(pixel/objectFocal,-1.));float t=-dot(objectEye,ray);if(t<=0.)discard;
vec3 p=objectEye+ray*t;float h=1.-dot(p,p);if(h<-.000001)discard;n=p-ray*sqrt(max(h,0.));
}"""
    html=html.substring(0,from)+branch+html.substring(to)
    replace("['resolution','center','radius','rotation','sunlight','axisOrientation']",
        "['resolution','center','radius','rotation','sunlight','axisOrientation','objectView','objectFocal','objectEye','objectTarget','objectFraming']")
    replace("gl.drawArrays(gl.TRIANGLES,0,6)", """const camera=window.spaceCompassObjectCamera?.();
gl.uniform1f(locations.objectView,camera?(camera.eye?2:1):0);gl.uniform1f(locations.objectFocal,camera?.focal||1);
gl.uniform3fv(locations.objectEye,camera?.eye||[0,0,0]);gl.uniform3fv(locations.objectTarget,camera?.target||[0,0,0]);gl.uniform3fv(locations.objectFraming,camera?.framing||[0,0,1]);gl.drawArrays(gl.TRIANGLES,0,6)""")
    replace("const pixel=i*4,k=i*3,nx=normals[k],ny=normals[k+1],nz=normals[k+2];", """const pixel=i*4,k=i*3,objectNormal=window.spaceCompassObjectSoftwareNormal?.(normals[k],normals[k+1]);
const nx=objectNormal?objectNormal[0]:normals[k],ny=objectNormal?objectNormal[1]:normals[k+1],nz=objectNormal?objectNormal[2]:normals[k+2];""")
    replace("const key=nativeEarthOrientation.join(',')+','+nativeSunDirection.join(',')+", "const objectEye=window.spaceCompassObjectCamera?.()?.eye;const key=JSON.stringify(window.spaceCompassObjectCamera?.())+nativeEarthOrientation.join(',')+','+nativeSunDirection.join(',')+")
    replace("if(nz<0){output.data[pixel+3]=0;continue}","if(!Number.isFinite(nz)||(!objectNormal&&nz<0)){output.data[pixel+3]=0;continue}")
    replace("asin(v.y)/PI","asin(clamp(v.y,-1.,1.))/PI")
    replace("vec3 halfV=sun+vec3(0,0,1);", "vec3 viewer=objectView>1.5?normalize(objectEye-n):vec3(0,0,1);vec3 halfV=sun+viewer;")
    replace("pow(1.-n.z,3.2)","pow(max(0.,1.-dot(n,viewer)),3.2)")
    replace("const shine=lit>0?Math.pow(Math.max(0,nx*hx+ny*hy+nz*hz),55)*waterT*waterT*(3-2*waterT)*.55:0;", """let facing=nz,halfDot=nx*hx+ny*hy+nz*hz;
if(objectEye&&objectNormal){const vx=objectEye[0]-nx,vy=objectEye[1]-ny,vz=objectEye[2]-nz,length=Math.hypot(vx,vy,vz);
const x=vx/length,y=vy/length,z=vz/length,hx=sx+x,hy=sy+y,hz=sz+z,half=Math.max(.0001,Math.hypot(hx,hy,hz));
facing=nx*x+ny*y+nz*z;halfDot=(nx*hx+ny*hy+nz*hz)/half;}
const shine=lit>0?Math.pow(Math.max(0,halfDot),55)*waterT*waterT*(3-2*waterT)*.55:0;""")
    replace("const rim=Math.pow(1-nz,3.2)","const rim=Math.pow(Math.max(0,1-facing),3.2)")
    // Interactive views use full existing textures and a finer settled raster. Keep the
    // standalone screensaver at its original low-power resolution.
    replace("ctx=canvas.getContext('2d'),size=256,mapWidth=512,mapHeight=256;",
        "ctx=canvas.getContext('2d'),mapWidth=2048,mapHeight=1024;let size=256;")
    replace("const output=ctx.createImageData(size,size),normals=new Float32Array(size*size*3),rimColor=[.1,.32,.61];",
        "let output=ctx.createImageData(size,size),normals=new Float32Array(size*size*3);const rimColor=[.1,.32,.61];")
    replace("let day=null,night=null,lastTime=NaN,lastKey='',lastPose=null;", """const detailBuffers=new Map([[size,{output,normals}]]);
function detailResolution(next){if(next===size)return;
 let cached=detailBuffers.get(next);if(!cached){const pixels=ctx.createImageData(next,next),vectors=new Float32Array(next*next*3);
 for(let y=0;y<next;y++)for(let x=0;x<next;x++){const i=(y*next+x)*3,nx=(x+.5)*2/next-1,ny=1-(y+.5)*2/next,d=nx*nx+ny*ny;
 vectors[i]=nx;vectors[i+1]=ny;vectors[i+2]=d<1?Math.sqrt(1-d):-1;}
 cached={output:pixels,normals:vectors};detailBuffers.set(next,cached);}
 size=next;canvas.width=canvas.height=size;output=cached.output;normals=cached.normals;lastKey='';}
let day=null,night=null,lastTime=NaN,lastKey='',lastPose=null;""")
    replace("if(!day)day=load(document.querySelector('.earth-day'),true);",
        "detailResolution(window.spaceCompassGlobeDetailSize?.()||640);if(!day)day=load(document.querySelector('.earth-day'),true);")
    return html
}
