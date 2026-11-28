// Firebase ID token verification without Admin credentials. Public signing
// keys and Firebase account lookup are the only upstream identity services.
import {createRemoteJWKSet,jwtVerify} from 'jose';
const jwks=createRemoteJWKSet(new URL('https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com'));
export async function authenticate(token,env){
 if(typeof token!=='string'||token.length>8192)throw Error('Invalid token');
 const project=env.FIREBASE_PROJECT_ID;
 if(typeof project!=='string'||!project||!env.FIREBASE_WEB_API_KEY)throw Error('Missing auth configuration');
 const {payload,protectedHeader}=await jwtVerify(token,jwks,{algorithms:['RS256'],issuer:'https://securetoken.google.com/'+project,audience:project,
  requiredClaims:['sub','exp','iat','auth_time']});
 const now=Math.floor(Date.now()/1000),uid=payload.sub;
 if(!protectedHeader.kid||typeof uid!=='string'||!/^[A-Za-z0-9_-]{1,128}$/.test(uid)||
  ['__proto__','constructor','prototype'].includes(uid)||!Number.isInteger(payload.auth_time)||payload.auth_time>now||
  !Number.isInteger(payload.iat)||payload.iat>now||payload.firebase?.sign_in_provider==='anonymous')throw Error('Invalid identity');
 // Unlike signature-only validation, lookup also checks account existence.
 // Firebase returns validSince for revoked sessions; missing data fails closed.
 const res=await fetch('https://identitytoolkit.googleapis.com/v1/accounts:lookup?key='+encodeURIComponent(env.FIREBASE_WEB_API_KEY),
  {method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({idToken:token})});
 if(!res.ok)throw Error('Account unavailable');
 const data=await res.json(),user=data.users?.[0];
 if(!user||user.localId!==uid||user.disabled===true||!Number.isFinite(Number(user.validSince))||
  payload.auth_time<Number(user.validSince))throw Error('Account revoked');
 return {uid};
}
