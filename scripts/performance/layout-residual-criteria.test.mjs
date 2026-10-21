import test from 'node:test';import assert from 'node:assert/strict';
import{classifyNavShifts,classifyIconTransitions}from'./layout-residual-criteria.mjs';
test('classifies descendant regions across parser, font and hydration without dropping events',()=>{
 const shifts=[{time:10,sources:[{region:'nav'}]},{time:20,sources:[{region:'actions'}]},{time:30,sources:[{region:'nav'}]},{time:31,sources:[{region:'workflow'}]}];
 const out=classifyNavShifts(shifts,{fontsReleasedAt:20,releasedAt:30});assert.deepEqual(out,{parser:[shifts[0]],font:[shifts[1]],hydration:[shifts[2]]});
 assert.equal(classifyNavShifts(shifts,{releasedAt:30}).parser.length,2);
});
test('text changes above pillars are reported separately from icon-only geometry',()=>{
 const s=top=>({icons:[{width:22}],articles:[{x:20,height:50}],workflow:{top:top+200},pillars:{top}});
 const out=classifyIconTransitions([s(100),s(70),s(70)],['manrope','icons']);assert.equal(out.stable,true);assert.equal(out.textResidual[0].workflowGlobalDelta,-30);assert.equal(out.iconTransitions[0].workflowRelativeDelta,0);
 const changed=s(70);changed.articles[0].height=60;assert.equal(classifyIconTransitions([s(100),s(70),changed],['manrope','icons']).stable,false);
});
test('missing geometry cannot pass vacuously',()=>{
 assert.throws(()=>classifyIconTransitions([{icons:[],articles:[]}],[]),/Nonempty/);
 assert.throws(()=>classifyIconTransitions([{icons:[{width:22}],articles:[{}]}],[]),/Required/);
});

import{inspectNavCtaResidual}from'./nav-cta-geometry.mjs';
test('CTA residual requires exact node identity, stable neighbours, changed label and centered movement',()=>{
 const rect=(x,width)=>({x,y:10,width,height:20,left:x,right:x+width,top:10,bottom:30});const before={label:'long',text:rect(10,50),icon:rect(70,13),boxes:{cta:rect(0,120),brand:rect(0,30)}};const after={label:'short',text:rect(16,38),icon:rect(64,13),boxes:structuredClone(before.boxes)};const shift={value:.001,sources:[{region:'actions',knownNavCta:true}]};
 assert.equal(inspectNavCtaResidual(before,after,[shift],{width:1200,height:800}).structural.length,0);
 const moved=structuredClone(after);moved.boxes.brand.x+=1;assert.equal(inspectNavCtaResidual(before,moved,[shift],{width:1200,height:800}).structural.length,1);
 assert.equal(inspectNavCtaResidual(before,after,[{...shift,sources:[{region:'actions',knownNavCta:false}]}],{width:1200,height:800}).structural.length,1);
 const wrong=structuredClone(after);wrong.icon.x+=2;assert.equal(inspectNavCtaResidual(before,wrong,[shift],{width:1200,height:800}).structural.length,1);
 const sameLabel=structuredClone(after);sameLabel.label=before.label;assert.equal(inspectNavCtaResidual(before,sameLabel,[shift],{width:1200,height:800}).structural.length,1);
 const vertical=structuredClone(after);vertical.icon.y+=1;assert.equal(inspectNavCtaResidual(before,vertical,[shift],{width:1200,height:800}).structural.length,1);
 const resized=structuredClone(after);resized.icon.width+=1;assert.equal(inspectNavCtaResidual(before,resized,[shift],{width:1200,height:800}).structural.length,1);
});

import{probePage}from'./probe-page.mjs';
test('invalid derived fixture fails before reading an artifact or opening Chrome',async()=>{await assert.rejects(probePage({trialFixture:'invalid'}),/Invalid derived trial fixture/);});
