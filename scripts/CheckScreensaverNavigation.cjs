// Run with Node.js: node scripts/CheckScreensaverNavigation.cjs
// Exercises the bundled controller itself; no browser packages or duplicate implementation.
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm'),assert=require('node:assert/strict');
const html=fs.readFileSync(path.join(__dirname,'../app/src/main/assets/screensaver/mondiversi-space.html'),'utf8');
new vm.Script(html.match(/<script>([\s\S]+)<\/script>/)[1]);
const source=html.split('// BEGIN GLOBE_NAVIGATION')[1].split('// END GLOBE_NAVIGATION')[0];
const context=vm.createContext({});vm.runInContext(source,context);
const create=context.createGlobeNavigation,near=(a,b)=>assert.ok(Math.abs(a-b)<1e-10,`${a} != ${b}`);
const equivalent=(a,b)=>near(Math.atan2(Math.sin(a-b),Math.cos(a-b)),0);
const normal=t=>1-t*.000013;
let checks=0;
{
 const globe=create();near(globe.sample(100,normal(100)).rotation,normal(100));
 globe.begin(100,normal(100));globe.move(.25,.1,200);globe.end(300);
 const held=globe.sample(300,normal(300));assert.ok(held.rotation<normal(100),'Rightward drag must move the surface right');
 for(const t of [500,5000,10299,10300]){equivalent(globe.sample(t,normal(t)).rotation,held.rotation);near(globe.sample(t,normal(t)).pitch,held.pitch)}
 const midway=globe.sample(10900,normal(10900));near(midway.pitch,held.pitch/2);
 const restored=globe.sample(11500,normal(11500));equivalent(restored.rotation,normal(11500));near(restored.pitch,0);assert.equal(globe.active,false);
 equivalent(globe.sample(20000,normal(20000)).rotation,normal(20000));checks++;
}
{
 const globe=create();globe.begin(0,normal(0));globe.move(.2,.2,10);
 assert.ok(globe.sample(90000,normal(90000)).pitch>.1,'Holding a drag must not trigger a return');
 globe.end(90000);const held=globe.sample(90000,normal(90000));
 equivalent(globe.sample(99999,normal(99999)).rotation,held.rotation);checks++;
}
{
 const globe=create();globe.begin(0,normal(0));globe.move(.4,.2,10);globe.end(20);
 const returning=globe.sample(10620,normal(10620));
 globe.begin(10620,normal(10620));const grabbed=globe.sample(10620,normal(10620));
 equivalent(grabbed.rotation,returning.rotation);near(grabbed.pitch,returning.pitch);
 globe.move(-.05,-.05,10630);globe.end(10640);
 const held=globe.sample(10640,normal(10640));equivalent(globe.sample(20000,normal(20000)).rotation,held.rotation);checks++;
}
{
 const globe=create();globe.begin(0,0);globe.move(1000,1000,10);globe.end(20);
 const pose=globe.sample(20,0);assert.ok(Math.abs(pose.rotation)<=Math.PI);near(pose.pitch,Math.PI/2);
 globe.begin(30,0);globe.move(NaN,0,40);assert.ok(Number.isFinite(globe.sample(40,0).rotation));checks++;
}
{
 const globe=create();globe.begin(0,normal(0));globe.move(.2,.1,10);globe.end(20);
 const held=globe.sample(100,normal(100));globe.resume(100);
 equivalent(globe.sample(100,normal(100)).rotation,held.rotation);
 near(globe.sample(700,normal(700)).pitch,held.pitch/2);globe.resume(700);
 near(globe.sample(700,normal(700)).pitch,held.pitch/2);
 const restored=globe.sample(1300,normal(1300));equivalent(restored.rotation,normal(1300));near(restored.pitch,0);
 assert.equal(globe.active,false,'A tap must bypass the ten-second wait');checks++;
}
// A shared view transform must preserve the angle to the real Earth-fixed Sun in every pose.
const helpers=html.slice(html.indexOf('function earthViewVector('),html.indexOf('function earthMarkerStyle('));
vm.runInContext('var nativeEarthOrientation=[0,0],nativeSunDirection=[.7,.2,Math.sqrt(.47)];'+helpers,context);
const sun=[.7,.2,Math.sqrt(.47)],dot=(a,b)=>a.x*b.x+a.y*b.y+a.depth*b.depth;
for(const rotation of [-3,-1,0,1,3])for(const roll of [-.4,0,.4])for(const pitch of [-1.9,-.4,0,1.9]){
 const vector=[.6,0,.8],surface=context.earthViewVector(vector,rotation,[roll,pitch]);
 const light=context.earthViewVector(sun,rotation,[roll,pitch]);
 near(dot(surface,light),vector[0]*sun[0]+vector[1]*sun[1]+vector[2]*sun[2]);
 const marker=context.earthSurfacePoint([0,0],rotation,[roll,pitch]);
 const centre=context.earthViewVector([0,0,1],rotation,[roll,pitch]);near(marker.x,centre.x);near(marker.y,centre.y);near(marker.depth,centre.depth);checks++;
}
console.log(JSON.stringify({status:'passed',checks,ten_second_hold:true,smooth_return_to_current_phase:true,
 regrab_without_jump:true,release_resets_idle_deadline:true,rightward_drag_direction:true,
 marker_and_sunlight_share_transform:true,offline_script_syntax:true}));
