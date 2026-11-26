import {createHash} from 'node:crypto';
const avatars=new Set(['astra','piko','nobi','tera','lopi','tomba','foksi','moka','pera','gigi']);
export function profileMembers(s){
 if(s?.phase==='lobby')return s.members.map(p=>p.uid);
 if(s?.game)return (s.connection?.players??[]).filter(p=>!p.removed).map(p=>p.id);
 return [];
}
export function safeNickname(raw){
 return typeof raw==='string'&&/^[\p{L}\p{N} ._-]{2,24}$/u.test(raw.trim())?raw.trim():'Oyuncu';
}
export function validatePortrait(input){
 if(!input||typeof input!=='object'||Object.keys(input).some(k=>!['avatar','preview'].includes(k)))throw Error('Invalid profile');
 if(avatars.has(input.avatar)&&input.preview==null)return {avatar:input.avatar,preview:null};
 if(typeof input.avatar!=='string'||!/^custom_[A-Za-z0-9_-]{8,80}$/.test(input.avatar)||typeof input.preview!=='string'||
  input.preview.length>180050||!input.preview.startsWith(input.avatar+':'))throw Error('Invalid portrait');
 const data=input.preview.slice(input.avatar.length+1);
 if(!/^[A-Za-z0-9+/]+={0,2}$/.test(data)||data.length%4!==0)throw Error('Invalid portrait encoding');
 const b=Buffer.from(data,'base64');
 if(b.length<80||b.length>130000||b.toString('ascii',0,4)!=='RIFF'||b.toString('ascii',8,12)!=='WEBP'||
  b.readUInt32LE(4)+8!==b.length)throw Error('Invalid WebP');
 // The existing Android codec also checks actual decoded dimensions before allocation.
 return {avatar:input.avatar,preview:input.preview};
}
export function profileVersion(profiles){
 return createHash('sha256').update(JSON.stringify(profiles)).digest('hex');
}
export async function readProfiles(db,room,members){
 const snap=await db.ref('arenaV150Profiles/'+room).get(),saved=snap.val()??{};
 const profiles=Object.fromEntries(await Promise.all(members.map(async uid=>{
  const nickname=await db.ref('users/'+uid+'/nickname').get();
  return [uid,{name:safeNickname(nickname.val()),avatar:saved[uid]?.avatar??'piko',preview:saved[uid]?.preview??null}];
 })));
 return {profiles,version:profileVersion(profiles)};
}
