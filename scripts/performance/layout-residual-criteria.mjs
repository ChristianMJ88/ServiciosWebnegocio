import assert from 'node:assert/strict';
export function classifyNavShifts(shifts,{fontsReleasedAt,releasedAt}){
 const groups={parser:[],font:[],hydration:[]};
 for(const shift of shifts)if(shift.sources.some(s=>['nav','actions'].includes(s.region))){
  const group=shift.time>=releasedAt?'hydration':fontsReleasedAt!==undefined&&shift.time>=fontsReleasedAt?'font':'parser';groups[group].push(shift);
 }return groups;
}
export function classifyIconTransitions(states,order){
 for(const state of states){assert.ok(state.icons.length>0&&state.icons.length===state.articles.length,'Nonempty icon/article geometry');assert.ok(state.workflow&&state.pillars,'Required workflow and pillars');}
 const equal=(a,b)=>Math.abs(a-b)<.5;
 const relative=s=>s.workflow.top-s.pillars.top;
 const widthsStable=states.every(s=>s.icons.every(i=>equal(i.width,22))),iconTransitions=[],textResidual=[];
 for(let i=0;i<order.length;i++){
  const before=states[i],after=states[i+1];const transition={from:i,to:i+1,articleDeltas:after.articles.map((a,n)=>({x:a.x-before.articles[n].x,height:a.height-before.articles[n].height})),workflowRelativeDelta:relative(after)-relative(before),workflowGlobalDelta:after.workflow.top-before.workflow.top};
  if(order[i]==='icons')iconTransitions.push({...transition,stable:transition.articleDeltas.every(d=>equal(d.x,0)&&equal(d.height,0))&&equal(transition.workflowRelativeDelta,0)});else textResidual.push(transition);
 }return{widthsStable,iconTransitions,textResidual,stable:widthsStable&&iconTransitions.every(t=>t.stable)};
}
