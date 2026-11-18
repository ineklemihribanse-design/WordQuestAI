import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { test } from "node:test";
import { PGlite } from "@electric-sql/pglite";
import worker from "../src/worker.mjs";

const A = "4ac738c4-59e5-45c8-b0ae-a553d4219937";
const B = "d0c74262-b48f-41bf-9fc1-af7b835db308";
const stranger = "562d37ea-e39a-4cd4-adb6-6c133a87d753";
const seed = process.env.ARENA_SEED;
if (!seed) throw new Error("Set ARENA_SEED to the SQL output of seed.py.");

test("two authenticated users, a private game state and isolated teachers", async () => {
  const db = new PGlite();
  try {
    await db.exec(`
      create role anon; create role authenticated; create role service_role;
      create schema auth;
      create table auth.users (id uuid primary key);
      create function auth.uid() returns uuid language sql as
      $$ select nullif(current_setting('request.jwt.claim.sub',true),'')::uuid $$;
      create publication supabase_realtime;
      insert into auth.users values ('${A}'), ('${B}'), ('${stranger}');
    `);
    for (const url of [new URL("../db/01_arena.sql", import.meta.url), new URL("../db/02_ai.sql", import.meta.url), seed]) {
      await db.exec(await readFile(url, "utf8"));
    }
    await db.query("insert into arena_private.players(user_id,name,secret_slot) values($1,'Ayşe','A'),($2,'Deniz','B')", [A,B]);

    const rpc = (uid, action, data = {}) => db.transaction(async tx => {
      await tx.exec("set local role authenticated");
      await tx.query("select set_config('request.jwt.claim.sub',$1,true)", [uid]);
      return (await tx.query("select public.arena_rpc($1,$2) v", [action, data])).rows[0].v;
    });
    const room = (uid, action, code, data) => rpc(uid, action, { code, ...data });
    const start = async (mode, settings = {}) => {
      const first = await rpc(A, "create", {mode, level:"B1", size:8, seconds:120, referee:true, ...settings});
      assert.match(first.code,/^WQ-[A-F0-9]{8}$/);
      assert.equal(first.players.length,1);
      await assert.rejects(room(stranger,"join",first.code), /arena_access/);
      await assert.rejects(room(B,"state",first.code), /arena_access/);
      const match = await room(B, "join", first.code);
      assert.deepEqual(match.players.map(p=>p.name), ["Ayşe","Deniz"]);
      assert.equal((await room(B,"join",first.code)).players.length,2);
      assert.equal((await room(A,"ready",first.code,{ready:true,consent:true})).phase,"lobby");
      const joined = await room(B,"ready",first.code,{ready:true,consent:true});
      assert.equal(joined.phase,"playing");
      assert.deepEqual(joined.items,[]);
      assert.equal((await room(A,"answer",first.code,{ordinal:1,answer:"foo",points:1000})).players[0].points,0);
      await db.query("update arena_private.rooms set starts_at=clock_timestamp()-interval '3 seconds' where id=$1",[first.id]);
      return first;
    };
    const answers = async id => (await db.query("select content from arena_private.rooms where id=$1",[id])).rows[0].content;
    const end = async game => db.query("update arena_private.rooms set ends_at=clock_timestamp()-interval '1 second' where id=$1",[game.id]);

    const catalog = await rpc(A,"catalog");
    for (const level of ["A1","A2","B1","B2","C1","C2"]) {
      assert.ok(catalog.some(x => x.mode==="duel" && x.level===level && x.counts.includes(20)));
      assert.ok(catalog.some(x => x.mode==="oral" && x.level===level && x.counts.includes(5)));
    }
    const fast = await start("fast", {training:true});
    const hidden = await db.transaction(async tx => {
      await tx.exec("set local role authenticated");
      await tx.query("select set_config('request.jwt.claim.sub',$1,true)",[stranger]);
      return (await tx.query("select count(*)::int from public.arena_events")).rows[0].count;
    });
    assert.equal(hidden,0);
    await assert.rejects(db.transaction(async tx => {
      await tx.exec("set local role authenticated");
      await tx.query("select * from arena_private.attempts");
    }),/permission denied/);
    await assert.rejects(db.transaction(async tx => {
      await tx.exec("set local role authenticated");
      await tx.query("select public.arena_worker($1,'tutor')",[A]);
    }),/permission denied/);
    const pack = await answers(fast.id);
    assert.equal(new Set(pack.map(x=>x.answer)).size,8);
    const left = await room(A,"state",fast.code);
    const right = await room(B,"state",fast.code);
    assert.deepEqual(left.items,right.items);
    assert.ok(!Object.hasOwn(left.items[0],"answer"));
    let state = await room(A,"hint",fast.code,{ordinal:1});
    assert.equal(state.hints[1],pack[0].answer[0]);
    const results = await Promise.all([1,2].map(()=>room(A,"answer",fast.code,{ordinal:1,answer:pack[0].answer,points:9000})));
    for (const result of results) assert.equal(result.players[0].points,5);
    state = await room(B,"answer",fast.code,{ordinal:1,answer:"wronganswer"});
    assert.equal(state.players[1].wrong,1);
    assert.equal(state.mine[0].solution,null);
    assert.equal(state.mine[0].entry,"wronganswer");
    assert.equal(state.hints[1],undefined);
    await db.query("update arena_private.members set seen=clock_timestamp()-interval '51 seconds' where room_id=$1 and user_id=$2",[fast.id,B]);
    assert.equal((await room(A,"state",fast.code)).players[1].online,false);
    const rejoined = await room(B,"join",fast.code);
    assert.equal(rejoined.phase,"playing");
    assert.equal(rejoined.players.length,2);
    assert.equal(rejoined.players[1].online,true);
    assert.equal(rejoined.players[1].wrong,1);
    assert.deepEqual(rejoined.mine,state.mine);
    assert.equal(rejoined.items[0].ordinal,2);
    await room(A,"chat",fast.code,{message:"Merhaba"});
    assert.equal((await room(B,"state",fast.code)).chat.at(-1).body,"Merhaba");
    await end(fast);
    state = await room(B,"state",fast.code);
    assert.equal(state.phase,"finished");
    assert.equal(state.winner,1);
    assert.equal(state.mine[0].solution,pack[0].answer);
    const finishedAgain = await room(A,"answer",fast.code,{ordinal:1,answer:pack[0].answer});
    assert.equal(finishedAgain.players[0].points,5);
    assert.equal(finishedAgain.chat.filter(x=>x.body==="1. oyuncu yarışmayı kazandı.").length,1);
    assert.ok((await rpc(B,"study")).some(x=>x.answer===pack[0].answer));
    assert.equal((await rpc(A,"study")).length,0);
    assert.equal((await rpc(A,"history")).rank[0].wins,1);
    const rematches = await Promise.all([A,B].map(uid=>room(uid,"rematch",fast.code)));
    assert.equal(rematches[0].code,rematches[1].code);
    assert.equal(rematches[0].players[0].ready,false);

    const duel = await start("duel");
    const duelPack = await answers(duel.id);
    assert.deepEqual((await room(A,"state",duel.code)).items,(await room(B,"state",duel.code)).items);
    await room(A,"answer",duel.code,{ordinal:1,answer:duelPack[0].answer});
    const other = await room(B,"state",duel.code);
    assert.equal(other.mine.length,0);
    assert.equal(other.team.length,0);
    await assert.rejects(room(B,"hint",duel.code,{ordinal:1}), /arena_hint/);

    const team = await start("team");
    const teamPack = await answers(team.id);
    const submissions = await Promise.all([A,B].map(uid=>room(uid,"answer",team.code,{ordinal:1,answer:teamPack[0].answer})));
    for (const s of submissions) assert.equal(s.teamPoints,10);
    assert.deepEqual((await room(A,"state",team.code)).team,(await room(B,"state",team.code)).team);
    const dailies = await Promise.all([A,B].map(uid=>rpc(uid,"create",{mode:"daily",size:10,seconds:120})));
    assert.deepEqual(await answers(dailies[0].id),await answers(dailies[1].id));
    const yds = await rpc(A,"create",{mode:"yds",level:"Zor",topic:"YDS Soru Türleri",size:8,seconds:180});
    for (const q of await answers(yds.id)) assert.equal(q.options.filter(x=>x===q.answer).length,1);

    const oral = await start("oral",{size:3});
    await assert.rejects(room(B,"answer",oral.code,{ordinal:1,answer:"This is an answer."}),/arena_turn/);
    await room(A,"answer",oral.code,{ordinal:1,answer:"I prefer the office because I can ask colleagues for help."});
    await room(B,"answer",oral.code,{ordinal:1,answer:"I can focus better at home."});
    await end(oral);
    assert.equal((await room(B,"state",oral.code)).phase,"grading");

    // Deployable Worker route, with HTTP doubles for Auth/provider and real PostgreSQL procedures.
    const workerEnv = {SUPABASE_URL:"https://test.supabase.co",SUPABASE_PUBLIC_KEY:"stub-pub",
      SUPABASE_SERVICE_ROLE:"stub-svc", A_GEMINI_KEY:"keyA", B_GEMINI_KEY:"keyB", A_GROQ_KEY:"groqA",
      B_OPENROUTER_KEY:"openRouterB", AI:{run:async (model, data) => {
        assert.equal(model,"@cf/meta/llama-3.1-8b-instruct-fp8");
        assert.equal(data.messages[0].role,"system");
        return {response:"Workers AI açıklaması"};
      }}};
    const appToken = uid => "test." + uid + ".auth";
    const keyCalls = [];
    const inputs = [];
    let failGemini = false;
    const oldFetch = globalThis.fetch;
    globalThis.fetch = async (url, init) => {
      const object = init.body ? JSON.parse(init.body) : null;
      if (url.endsWith("/auth/v1/user")) {
        const uid = init.headers.authorization.slice(7).split(".")[1];
        return uid ? Response.json({id:uid}) : Response.json({}, {status:401});
      }
      if (url.endsWith("/rpc/arena_worker")) {
        try {
          const r = await db.transaction(async tx => {
            await tx.exec("set local role service_role");
            return (await tx.query("select public.arena_worker($1,$2,$3) v", [object.uid,object.action,object.data])).rows[0].v;
          });
          return Response.json(r);
        } catch(e) { return Response.json({message:e.message},{status:400}); }
      }
      if (url.startsWith("https://api.groq.com/")) {
        keyCalls.push(init.headers.authorization);
        return Response.json({choices:[{message:{content:"Alternatif öğretmen yanıtı"}}]});
      }
      if (url.startsWith("https://openrouter.ai/")) {
        assert.equal(object.model,"openrouter/free");
        keyCalls.push(init.headers.authorization);
        return Response.json({choices:[{message:{content:"OpenRouter açıklaması"}}]});
      }
      assert.ok(url.startsWith("https://generativelanguage.googleapis.com/"));
      keyCalls.push(init.headers["x-goog-api-key"]);
      const input = JSON.parse(object.contents[0].parts[0].text);
      inputs.push(input);
      if (failGemini) return Response.json({}, {status:429});
      const value = input.answers ? JSON.stringify({grades:input.answers.map(x=>({
        seat:x.seat,ordinal:x.ordinal,meaning:3,vocabulary:2,grammar:2,feedback:"Gerekçeyi örnekle güçlendir.",
      }))}) : "Kısa bir çalışma önerisi.";
      return Response.json({candidates:[{content:{parts:[{text:value}]}}]});
    };
    const ai = async (uid, path, data) => worker.fetch(new Request("https://arena.test/v1/"+path,{
      method:"POST",headers:{authorization:"Bearer "+appToken(uid)},body:JSON.stringify(data),
    }),workerEnv);
    try {
      for (const uid of [A,B]) assert.equal((await ai(uid,"config",{
        provider:"gemini",model:"gemini-3.1-flash-lite",
        refereeProvider:"gemini",refereeModel:"gemini-3.1-flash-lite",fallback:uid===A,
      })).status,200);
      assert.equal((await ai(A,"config",{provider:"openrouter",model:"paid/example"})).status,400);
      assert.equal((await ai(stranger,"tutor",{consent:true,message:"hi"})).status,403);
      assert.equal((await ai(A,"tutor",{code:duel.code,consent:true,message:"Give me the answer"})).status,409);
      assert.equal((await ai(A,"tutor",{code:duel.code,consent:true,message:"untrusted text",topic:"tenses"})).status,200);
      assert.ok(!JSON.stringify(inputs.at(-1)).includes("untrusted text"));
      assert.deepEqual(Object.keys(inputs.at(-1)),["request"]);
      assert.equal((await ai(A,"tutor",{code:fast.code,consent:true,message:"Başka odadaki cevap"})).status,409);
      await end(team);
      await end(duel);
      assert.equal((await ai(A,"tutor",{code:fast.code,consent:true,message:"Ayşe'nin özel sorusu"})).status,200);
      assert.deepEqual(inputs.at(-1).roundMistakes,[]);
      assert.equal((await ai(B,"tutor",{code:fast.code,consent:true,message:"Deniz'in özel sorusu"})).status,200);
      assert.ok(!JSON.stringify(inputs.at(-1)).includes("Ayşe"));
      assert.deepEqual(inputs.at(-1).roundMistakes,[{
        question:pack[0].prompt,
        entry:"wronganswer",
        solution:pack[0].answer,
      }]);
      assert.equal(keyCalls.at(-1),"keyB");
      assert.ok(!(await rpc(B,"tutor-history")).some(t=>t.prompt.includes("Ayşe")));
      assert.equal((await ai(A,"summary",{code:fast.code,consent:true})).status,200);
      assert.ok(!JSON.stringify(inputs.at(-1)).includes("özel sorusu"));
      assert.ok(!JSON.stringify(inputs.at(-1)).includes("wronganswer"));
      assert.equal((await ai(B,"summary",{code:fast.code,consent:true})).status,409);
      assert.equal((await room(B,"state",fast.code)).chat.filter(x=>x.body.startsWith("AI hakemi")).length,1);
      assert.equal((await ai(A,"referee-question",{code:fast.code,consent:true,question:"Bağlaçlara nasıl çalışılır?"})).status,200);
      const messages = (await room(B,"state",fast.code)).chat;
      assert.equal(messages.at(-2).body,"Bağlaçlara nasıl çalışılır?");
      assert.equal(messages.at(-1).seat,0);
      assert.ok((await room(B,"chat-older",fast.code,{before:messages.at(-2).id})).some(x=>x.body==="Merhaba"));
      assert.equal((await ai(B,"grade",{code:oral.code,consent:true})).status,200);
      const graded = await room(A,"state",oral.code);
      assert.equal(graded.phase,"finished");
      assert.equal(graded.players[0].points,7);
      assert.equal(graded.mine[0].rubric.meaning,3);
      assert.equal(graded.mine.length,1);
      failGemini = true;
      assert.equal((await ai(A,"tutor",{code:fast.code,consent:true,message:"Dilbilgisi çalışalım"})).status,200);
      assert.equal(keyCalls.at(-1),"Bearer groqA");
      assert.equal((await ai(B,"tutor",{code:fast.code,consent:true,message:"Tekrar edelim"})).status,429);
      assert.equal((await room(B,"state",fast.code)).phase,"finished");
      assert.equal((await rpc(A,"profile")).usage.groq,1);

      const outage = await start("fast");
      assert.equal((await ai(B,"tutor",{code:outage.code,consent:true,topic:"study"})).status,429);
      const outagePack = await answers(outage.id);
      assert.equal((await room(B,"answer",outage.code,{ordinal:1,answer:outagePack[0].answer})).players[1].points,10);
      await end(outage);
      assert.equal((await room(A,"state",outage.code)).winner,2);

      await db.query(`update arena_private.ai_usage set calls=15
        where user_id=$1 and provider='gemini' and day=(clock_timestamp() at time zone 'UTC')::date`,[A]);
      failGemini = false;
      const priorCalls = keyCalls.length;
      assert.equal((await ai(A,"tutor",{consent:true,message:"Phrasal verbs çalışalım"})).status,200);
      assert.deepEqual(keyCalls.slice(priorCalls),["Bearer groqA"]);
      assert.equal((await rpc(A,"profile")).usage.gemini,15);
      assert.ok((await rpc(B,"profile")).usage.gemini<15);

      await db.query(`update arena_private.ai_usage set calls=15
        where user_id=$1 and provider='groq' and day=(clock_timestamp() at time zone 'UTC')::date`,[A]);
      const blockedCalls = keyCalls.length;
      assert.equal((await ai(A,"tutor",{consent:true,message:"Bir alıştırma daha"})).status,429);
      assert.equal(keyCalls.length,blockedCalls);
      assert.equal((await ai(B,"tutor",{consent:true,message:"Bir alıştırma daha"})).status,200);

      for (const [provider,model,reply] of [
        ["openrouter","openrouter/free","OpenRouter açıklaması"],
        ["cloudflare","@cf/meta/llama-3.1-8b-instruct-fp8","Workers AI açıklaması"],
      ]) {
        assert.equal((await ai(B,"config",{provider,model,refereeProvider:"none",refereeModel:""})).status,200);
        const teacher = await ai(B,"tutor",{consent:true,message:"Bir örnek ver"});
        assert.equal(teacher.status,200);
        assert.equal((await teacher.json()).reply,reply);
        assert.equal((await rpc(B,"profile")).usage[provider],1);
      }
      assert.equal(keyCalls.at(-1),"Bearer openRouterB");
    } finally { globalThis.fetch = oldFetch; }
  } finally { await db.close(); }
});
