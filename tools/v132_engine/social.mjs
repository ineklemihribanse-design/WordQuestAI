// Server-authoritative social actions. No game revision changes for reactions.
export const reactions=Object.freeze(['👏','👍','😊','😄','😮','🔥','🎉','💪','🤔','😢','❤️','🏆']);
export const socialTypes=new Set(['reaction','finish-offer','finish-accept','finish-reject','finish-withdraw']);
const fail=m=>{throw Error(m)};
const clone=x=>JSON.parse(JSON.stringify(x));
export function emptySocial(){return {revision:0,reactions:{},offer:null,lastOfferAt:null,notice:null,result:null};}
export function expireSocial(state,now){
 if(!state.offer||state.offer.status!=='pending'||now<state.offer.expiresAt)return state;
 const s=clone(state);s.offer.status='expired';s.notice={id:s.offer.id,kind:'expired',actor:null,at:now};s.revision++;return s;
}
export function applySocial(state,uid,cmd,now,eligible){
 if(!eligible.includes(uid)||!socialTypes.has(cmd.type))fail('Unauthorized social action');
 const s=clone(expireSocial(state,now));
 if(cmd.type==='reaction'){
  if(!reactions.includes(cmd.symbol))fail('Invalid reaction');
  const last=s.reactions[uid];
  if(last&&now-last.at<1500)fail('Reaction rate limit');
  s.reactions[uid]={id:cmd.id,symbol:cmd.symbol,at:now};
 }else if(cmd.type==='finish-offer'){
  if(eligible.length<2||s.offer?.status==='pending'||s.lastOfferAt!==null&&now-s.lastOfferAt<60000)fail('Offer unavailable');
  s.lastOfferAt=now;s.offer={id:cmd.id,from:uid,createdAt:now,expiresAt:now+30000,status:'pending',required:[...eligible],accepted:[uid]};
  s.notice=null;
 }else{
  const o=s.offer;
  if(!o||o.status!=='pending'||cmd.offerId!==o.id||!o.required.includes(uid))fail('Offer unavailable');
  if(cmd.type==='finish-withdraw'){
   if(uid!==o.from)fail('Only proposer may withdraw');
   o.status='withdrawn';
  }else{
   if(uid===o.from||o.accepted.includes(uid))fail('Already responded');
   if(cmd.type==='finish-reject')o.status='rejected';
   else{
    o.accepted.push(uid);
    if(o.required.every(p=>o.accepted.includes(p))){
     o.status='accepted';s.result={kind:'mutual-finish',winner:null};
    }
   }
  }
  if(o.status!=='pending')s.notice={id:o.id,kind:o.status,actor:uid,at:now};
 }
 s.revision++;return s;
}
export function socialView(state,now){
 return {revision:state.revision,reactions:Object.fromEntries(Object.entries(state.reactions).filter(([,v])=>now-v.at<1200)),
 offer:state.offer,notice:state.notice,cooldownUntil:state.lastOfferAt===null?null:state.lastOfferAt+60000};
}
