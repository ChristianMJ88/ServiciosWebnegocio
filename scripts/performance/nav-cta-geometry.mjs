import assert from 'node:assert/strict';
export const navGeometry=`(()=>{const box=e=>e?e.getBoundingClientRect().toJSON():null;const cta=document.querySelector('.top-cta');const text=[...cta.childNodes].find(n=>n.nodeType===3&&n.textContent.trim());const range=document.createRange();range.selectNodeContents(text);return{label:text.textContent.trim(),fonts:[...document.fonts].map(f=>({family:f.family,status:f.status})),text:range.getBoundingClientRect().toJSON(),icon:box(cta.querySelector('i')),boxes:Object.fromEntries(['.top-cta','.menu-trigger','.desktop-login','.desktop-links','.brand','.top-actions'].map(s=>[s,box(document.querySelector(s))]))};})()`;
export function inspectNavCtaResidual(before,after,shifts,viewport){
 assert.ok(before?.icon&&after?.icon,'Required CTA icon geometry');
 const near=(a,b)=>Math.abs(a-b)<=.5;
 const stableRect=(a,b)=>a&&b&&['x','y','width','height'].every(k=>near(a[k],b[k]));
 const boxesStable=Object.entries(before.boxes).every(([k,rect])=>stableRect(rect,after.boxes[k]));
 const dx=after.icon.x-before.icon.x,expectedDx=(after.text.width-before.text.width)/2;
 const centered=near(dx,expectedDx)&&near(after.icon.y,before.icon.y)&&near(after.icon.width,before.icon.width)&&near(after.icon.height,before.icon.height);
 const allowed=[],structural=[];
 for(const shift of shifts){const sources=shift.sources.filter(s=>['nav','actions'].includes(s.region));
  if(boxesStable&&centered&&before.label!==after.label&&sources.length>0&&sources.every(s=>s.knownNavCta===true))allowed.push(shift);else structural.push(shift);
 }
 const p=before.icon,c=after.icon;const overlap=Math.max(0,Math.min(p.right,c.right)-Math.max(p.left,c.left))*Math.max(0,Math.min(p.bottom,c.bottom)-Math.max(p.top,c.top));const union=p.width*p.height+c.width*c.height-overlap;
 const contribution=union/(viewport.width*viewport.height)*Math.max(Math.abs(dx),Math.abs(c.y-p.y))/Math.max(viewport.width,viewport.height);
 return{boxesStable,centered,dx,expectedDx,allowed,structural,contentResidual:{navCta:contribution,totalEventValues:allowed.map(s=>s.value)}};
}
