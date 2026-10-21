// Explicitly derived trial fixtures exercise existing API-driven presentation.
import{parseArgs}from'node:util';import{mkdir,writeFile}from'node:fs/promises';import path from'node:path';import assert from'node:assert/strict';
import{fingerprintPublicBuild}from'./build-fingerprint.mjs';import{publicPreview}from'./public-preview.mjs';import{probePage}from'./probe-page.mjs';import{finishProbe}from'./browser-probe.mjs';import{navGeometry,inspectNavCtaResidual}from'./nav-cta-geometry.mjs';
const{values}=parseArgs({options:{root:{type:'string',default:'dist/marketing-hydration-probe/production'},output:{type:'string',default:'/private/tmp/fluora-m3-r3-cta-fixtures'}}});
const root=path.resolve(values.root,'browser'),build=await fingerprintPublicBuild(root),results=[],violations=[],server=await publicPreview({root,apiOrigin:'http://localhost:8080',spaShell:'csr'});let page,error;await mkdir(values.output,{recursive:true,mode:0o700});
try{
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));const origin='http://localhost:'+server.address().port;
 for(const width of[1280,1440])for(const fixture of['positive','zero','plans503'])for(const held of[false,true]){
  page=await probePage({origin,root,width,height:900,scenario:held?'fontHold':fixture==='plans503'?'plans503':'normal',trialFixture:fixture==='plans503'?undefined:fixture,failPlans:fixture==='plans503'});
  const before=await page.cdp.evaluate(navGeometry);await page.release();await page.settled();const after=await page.cdp.evaluate(navGeometry);let afterFonts,fontsReleasedAt;
  if(held){fontsReleasedAt=await page.cdp.evaluate('performance.now()');await page.releaseFonts();afterFonts=await page.cdp.evaluate(navGeometry);}page.assertConsole();const final=await page.snapshot();
  const apiShifts=final.shifts.filter(s=>s.time>=final.releasedAt&&(fontsReleasedAt===undefined||s.time<fontsReleasedAt)&&s.sources.some(v=>['nav','actions'].includes(v.region)));
  const check=inspectNavCtaResidual(before,after,apiShifts,{width,height:900});
  const expectedChange=fixture==='positive',sameBox=(a,b)=>Object.keys(a.boxes).every(k=>['x','y','width','height'].every(p=>Math.abs(a.boxes[k][p]-b.boxes[k][p])<=.5));
  const labelChange=before.label!==after.label,valid=labelChange===expectedChange&&sameBox(before,after)&&check.structural.length===0&&(expectedChange||apiShifts.length===0);
  if(!valid)violations.push({width,fixture,held,labelChange,boxesStable:sameBox(before,after)});
  results.push({width,fixture,derivedFixtures:page.derivedFixtures,held,beforeFontState:held?'fallback':'loaded',before,after,afterFonts,labelChange,valid,fontsReleasedAt,releasedAt:final.releasedAt,apiShifts,shifts:final.shifts,check});
  await writeFile(path.join(values.output,'results.json'),JSON.stringify({build,results,violations},null,2),{mode:0o600});console.log(JSON.stringify({width,fixture,held,labelChange,valid}));await page.close();page=null;
 }
 if(violations.length)throw new Error(`${violations.length} CTA fixture violations`);
}catch(e){error=e;await writeFile(path.join(values.output,'failure.json'),JSON.stringify({build,error:e.stack,results,violations,diagnostic:page?await page.diagnostic(e):e.diagnostic},null,2),{mode:0o600});}
await finishProbe(error,[()=>page?.close(),()=>{server.closeAllConnections();return new Promise(resolve=>server.close(resolve));}]);
