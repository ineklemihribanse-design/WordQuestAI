// Trusted-server connection state reducer. No device clocks, Firebase SDK or client claims.
// The caller MUST authenticate connection events, assign session epochs and use server time.
// Persist the returned state and match mutation in the same server transaction.
export const GRACE_MS=Object.freeze([60000,30000,0]);
const validTime=t=>Number.isSafeInteger(t)&&t>=0;
const copy=x=>JSON.parse(JSON.stringify(x));
const remaining=(deadline,now)=>deadline===null?null:Math.max(0,deadline-now);
const player=(state,id)=>state.players.find(p=>p.id===id);

export function createConnectionState({kind,players,now,matchDeadline=null,questionDeadline=null}){
  if(!["duel","quiz","coop","team"].includes(kind)||!validTime(now)||
    !Array.isArray(players)||players.length!==(kind==="team"?4:2)||
    players.some(p=>typeof p.id!=="string"||!p.id||!Number.isSafeInteger(p.epoch)||p.epoch<1)||
    new Set(players.map(p=>p.id)).size!==players.length||
    [matchDeadline,questionDeadline].some(d=>d!==null&&(!validTime(d)||d<now)))
    throw new Error("Invalid connection state");
  if(kind==="team"&&(new Set(players.map(p=>p.team)).size!==2||
    players.some(p=>typeof p.team!=="string"||!p.team||
      players.filter(q=>q.team===p.team).length!==2)))throw new Error("Invalid team roster");
  return {kind,phase:"playing",revision:0,now,players:players.map(p=>({
    id:p.id,team:kind==="team"?p.team:p.id,epoch:p.epoch,connected:true,
    drops:0,until:null,removed:false,reason:null})),
    deadlines:{match:matchDeadline,question:questionDeadline},frozen:null,result:null};
}
function freeze(s,at){
  if(s.phase!=="playing")return;
  s.frozen={match:remaining(s.deadlines.match,at),question:remaining(s.deadlines.question,at)};
  s.deadlines={match:null,question:null};s.phase="paused";
}
function settle(s,at){
  const alive=s.players.filter(p=>!p.removed);
  if(s.kind==="coop"&&alive.length<2){s.phase="finished";s.result={kind:"incomplete"};}
  else if(s.kind!=="coop"&&new Set(alive.map(p=>p.team)).size<2){
    s.phase="finished";
    s.result=alive.length?{kind:"forfeit",winner:s.kind==="team"?alive[0].team:alive[0].id}:{kind:"no-contest"};
  }
  if(s.phase==="finished"){s.deadlines={match:null,question:null};s.frozen=null;return;}
  if(alive.some(p=>!p.connected)){freeze(s,at);return;}
  if(s.phase==="paused"){
    s.deadlines={match:s.frozen.match===null?null:at+s.frozen.match,
      question:s.frozen.question===null?null:at+s.frozen.question};
    s.frozen=null;s.phase="playing";
  }
}
function remove(p,reason){p.connected=false;p.removed=true;p.until=null;p.reason=reason;}
// Advancing the trusted clock expires any due disconnects even if nobody reconnects.
// At the exact boundary the deadline wins; a late reconnect cannot revive a player.
function expire(s,at){
  for(const p of s.players)if(!p.removed&&!p.connected&&p.until!==null&&p.until<=at)remove(p,"timeout");
  settle(s,at);
}
export function connectionTransition(state,event,at){
  if(!validTime(at)||at<state.now)throw new Error("Non-monotonic server time");
  if(!event||!["lost","restored","leave","tick"].includes(event.type))throw new Error("Invalid event");
  if(state.phase==="finished")return state;
  const s=copy(state);
  // Process scheduled expirations in chronological order, not at the next client's arrival.
  const due=[...new Set(s.players.filter(p=>!p.removed&&!p.connected&&p.until!==null&&p.until<=at)
    .map(p=>p.until))].sort((a,b)=>a-b);
  for(const time of due){expire(s,time);if(s.phase==="finished")break;}
  if(s.phase!=="finished"&&event.type!=="tick"){
    const p=player(s,event.id);
    if(!p||!Number.isSafeInteger(event.epoch)||event.epoch<1)throw new Error("Invalid identity");
    if(!p.removed){
      if(event.type==="lost"&&event.epoch===p.epoch&&p.connected){
        p.connected=false;p.drops++;
        if(p.drops>=3)remove(p,"third-disconnect");
        else p.until=at+GRACE_MS[p.drops-1];
      }else if(event.type==="restored"&&event.epoch>p.epoch){
        p.epoch=event.epoch;p.connected=true;p.until=null;
      }else if(event.type==="leave"&&event.epoch===p.epoch)remove(p,"voluntary");
      settle(s,at);
    }
  }
  // Repeated transport messages do not mutate gameplay or increment its revision.
  const changed=JSON.stringify({...s,now:0})!==JSON.stringify({...state,now:0});
  if(!changed)return state;
  s.now=at;s.revision=state.revision+1;return s;
}
export function gameplayAllowed(s,actor){
  const p=player(s,actor);return s.phase==="playing"&&!!p&&p.connected&&!p.removed;
}
