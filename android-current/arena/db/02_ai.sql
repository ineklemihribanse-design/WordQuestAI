begin;

-- The Worker authenticates the user through Auth and then calls only this service-role RPC.
-- Clients never receive a secret slot, unfinished answers, or another account's tutoring records.
create or replace function public.arena_worker(uid uuid, action text, data jsonb default '{}') returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
  p arena_private.players; r arena_private.rooms; seat_no int; n int; row_value jsonb; payload jsonb;
  provider_name text; model_name text; purpose_name text; chosen jsonb; today date:=(clock_timestamp() at time zone 'UTC')::date;
  now_at timestamptz:=clock_timestamp(); submitted text; result text; expected_count int;
  round_mistakes jsonb:='[]';
begin
  select * into p from arena_private.players where user_id=uid;
  if not found then raise exception 'arena_access'; end if;

  if action='config' then
    update arena_private.players set provider=data->>'provider',model=data->>'model',
      referee_provider=data->>'refereeProvider',referee_model=data->>'refereeModel',
      fallback=coalesce((data->>'fallback')::boolean,false)
      where user_id=uid;
    return jsonb_build_object('saved',true);
  elsif action='reserve' then
    provider_name:=data->>'provider';
    if provider_name not in ('gemini','groq','openrouter','cloudflare') then raise exception 'arena_provider'; end if;
    perform 1 from arena_private.players where user_id=uid for update;
    if coalesce((select sum(calls) from arena_private.ai_usage where user_id=uid and day=today),0)>=30 then
      raise exception 'arena_quota';
    end if;
    insert into arena_private.ai_usage(user_id,day,provider,calls) values(uid,today,provider_name,1)
      on conflict(user_id,day,provider) do update set calls=ai_usage.calls+1 where ai_usage.calls<15;
    if not found then raise exception 'arena_quota'; end if;
    return jsonb_build_object('reserved',true);
  end if;

  if action in ('summary','question','grade','grade-save','summary-save','question-save','release') or
    (action='tutor' and coalesce(data->>'code','')<>'') then
    select rooms.* into r from arena_private.rooms join arena_private.members m on m.room_id=rooms.id and m.user_id=uid
      where rooms.code=upper(trim(data->>'code')) for update of rooms;
    if not found then raise exception 'arena_access'; end if;
    r:=arena_private.settle(r);
  end if;

  if action='tutor' then
    -- The teacher receives only the caller's tutoring context. During play even
    -- that context is omitted and available prompts are selected from safe topics.
    if coalesce(r.phase,'') not in ('playing','grading') and exists(
      select 1 from arena_private.rooms x join arena_private.members m on m.room_id=x.id
      where m.user_id=uid and x.phase='playing' and x.ends_at>now_at
    ) then raise exception 'arena_active'; end if;
    if r.phase in ('playing','grading') then
      if data->>'topic' not in ('word-order','tenses','connectors','study') or data->>'topic' is null
        then raise exception 'arena_active'; end if;
      payload:=jsonb_build_object('active',true,'topic',data->>'topic','context','[]'::jsonb,'review','[]'::jsonb);
    else
      select coalesce(jsonb_agg(jsonb_build_object('prompt',prompt,'reply',left(reply,800)) order by id),'[]') into chosen
        from (select id,prompt,reply from arena_private.tutor where user_id=uid and left(prompt,5)<>'@wq: '
              order by id desc limit 5) x;
      if r.phase='finished' and not r.grading_skipped then
        select coalesce(jsonb_agg(jsonb_strip_nulls(jsonb_build_object(
          'question',r.content->(a.ordinal-1)->>'prompt',
          'entry',a.answer,
          'solution',nullif(r.content->(a.ordinal-1)->>'answer',''),
          'explanation',nullif(r.content->(a.ordinal-1)->>'explanation',''),
          'rubric',a.rubric
        )) order by a.ordinal),'[]') into round_mistakes
        from arena_private.attempts a
        where a.room_id=r.id and a.user_id=uid and a.correct is false;
      end if;
      payload:=jsonb_build_object('active',false,'context',chosen,'roundMistakes',round_mistakes,'review',
        coalesce((select jsonb_agg(i) from (select value as i from jsonb_array_elements(arena_private.study(uid)) limit 8) t),'[]'));
    end if;
    return payload || jsonb_build_object('slot',p.secret_slot,'provider',p.provider,'model',p.model,'fallback',p.fallback);
  elsif action='tutor-save' then
    submitted:=data->>'prompt'; result:=data->>'reply';
    if char_length(submitted) not between 1 and 900 or char_length(result) not between 1 and 2200 then raise exception 'arena_message'; end if;
    insert into arena_private.tutor(user_id,prompt,reply) values(uid,submitted,result) returning id into n;
    return jsonb_build_object('id',n,'prompt',submitted,'reply',result);
  end if;

  if action in ('grade','summary','question') then
    if (action='grade' and r.phase<>'grading') or
      (action in ('summary','question') and (r.phase<>'finished' or r.grading_skipped)) or not r.referee then raise exception 'arena_stage'; end if;
    if action='summary' and r.referee_at is not null
      then raise exception 'arena_already'; end if;
    insert into arena_private.ai_jobs(room_id,purpose) values(r.id,action)
      on conflict(room_id,purpose) do update set started_at=now_at where ai_jobs.started_at<now_at-interval '75 seconds';
    if not found then raise exception 'arena_busy'; end if;
    if action='grade' then
      select coalesce(jsonb_agg(jsonb_build_object('seat',m.seat,'ordinal',a.ordinal,'answer',a.answer,
        'question',r.content->(a.ordinal-1)->>'prompt') order by a.ordinal,m.seat),'[]') into payload
      from arena_private.attempts a join arena_private.members m using(room_id,user_id) where a.room_id=r.id;
    else
      select coalesce(jsonb_agg(jsonb_build_object('seat',m.seat,
        'score',arena_private.score(r.id,m.user_id)-'lastAt',
        'incorrect',coalesce((select jsonb_agg(v) from (
          select jsonb_build_object('prompt',r.content->(a.ordinal-1)->>'prompt','kind',r.content->(a.ordinal-1)->>'kind') as v
          from arena_private.attempts a where a.room_id=r.id and a.user_id=m.user_id and not a.correct limit 8
        ) misses),'[]')) order by m.seat),'[]') into payload
      from arena_private.members m where m.room_id=r.id;
    end if;
    return jsonb_build_object('slot',p.secret_slot,'provider',p.referee_provider,'model',p.referee_model,
      'fallback',p.fallback,'job',(select extract(epoch from started_at)::text from arena_private.ai_jobs where room_id=r.id and purpose=action),
      'mode',r.mode,'level',r.level,'topic',r.topic,'input',payload,'winner',r.winner);
  end if;

  purpose_name:=case when action='release' then data->>'purpose' when action='summary-save' then 'summary'
    when action='question-save' then 'question' else 'grade' end;
  if not exists(select 1 from arena_private.ai_jobs where room_id=r.id and purpose=purpose_name
    and extract(epoch from started_at)::text=data->>'job') then raise exception 'arena_busy'; end if;
  if action='release' then
    delete from arena_private.ai_jobs where room_id=r.id and purpose=purpose_name;
    return jsonb_build_object('released',true);
  elsif action in ('summary-save','question-save') then
    if r.phase<>'finished' then raise exception 'arena_stage'; end if;
    result:=btrim(coalesce(data->>'reply',''));
    if char_length(result) not between 1 and 2160 then raise exception 'arena_message'; end if;
    if action='question-save' then
      submitted:=data->>'question';
      select seat into seat_no from arena_private.members where room_id=r.id and user_id=uid;
      if char_length(submitted) not between 1 and 600 then raise exception 'arena_message'; end if;
      insert into arena_private.chat(room_id,seat,body) values(r.id,seat_no,submitted);
    else
      update arena_private.rooms set referee_at=clock_timestamp() where id=r.id;
    end if;
    insert into arena_private.chat(room_id,seat,body) values(r.id,0,'AI hakemi · '||result);
  elsif action='grade-save' then
    if r.phase<>'grading' then raise exception 'arena_stage'; end if;
    select count(*) into expected_count from arena_private.attempts a where room_id=r.id;
    chosen:=data->'grades';
    if jsonb_typeof(chosen)<>'array' or jsonb_array_length(chosen)<>expected_count then raise exception 'arena_grade'; end if;
    for row_value in select * from jsonb_array_elements(chosen)
    loop
      seat_no:=(row_value->>'seat')::int;
      if (row_value->>'meaning')::int not between 0 and 4 or
         (row_value->>'vocabulary')::int not between 0 and 3 or
         (row_value->>'grammar')::int not between 0 and 3 or
         char_length(row_value->>'feedback') not between 1 and 400 then raise exception 'arena_grade'; end if;
      n:=(row_value->>'meaning')::int+(row_value->>'vocabulary')::int+(row_value->>'grammar')::int;
      update arena_private.attempts a set points=n,correct=n>=6,rubric=row_value-'seat'-'ordinal'
        from arena_private.members m where a.room_id=r.id and m.room_id=r.id and m.user_id=a.user_id
        and m.seat=seat_no and a.ordinal=(row_value->>'ordinal')::int and a.rubric is null;
      if not found then raise exception 'arena_grade'; end if;
    end loop;
    if exists(select 1 from arena_private.attempts where room_id=r.id and rubric is null) then raise exception 'arena_grade'; end if;
    perform arena_private.finish(r);
  else raise exception 'arena_action';
  end if;
  delete from arena_private.ai_jobs where room_id=r.id and purpose=purpose_name;
  perform arena_private.touch(r.id);
  return jsonb_build_object('saved',true);
end $$;

revoke all on function public.arena_worker(uuid,text,jsonb) from public, anon, authenticated;
grant execute on function public.arena_worker(uuid,text,jsonb) to service_role;
commit;
