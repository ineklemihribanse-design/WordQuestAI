begin;

create schema if not exists arena_private;
revoke all on schema arena_private from public, anon, authenticated;

create table arena_private.players (
  user_id uuid primary key references auth.users(id) on delete cascade,
  name text not null check (char_length(name) between 1 and 32),
  secret_slot text not null unique check (secret_slot in ('A', 'B')),
  provider text not null default 'none',
  model text not null default '',
  referee_provider text not null default 'none',
  referee_model text not null default '',
  fallback boolean not null default false
);

-- These two tables contain the answer keys. Clients cannot query this schema.
create table arena_private.items (
  id text primary key, mode text not null, level text not null, topic text not null,
  kind text not null, prompt text not null, answer text not null,
  options jsonb not null default '[]', explanation text not null default ''
);
create index arena_items_filter on arena_private.items(mode, level, topic);
create index arena_items_answer on arena_private.items(mode,level,answer,id);
create table arena_private.boards (
  id text primary key, level text not null, topic text not null,
  size int not null, height int not null, width int not null, words jsonb not null
);
create index arena_boards_filter on arena_private.boards(level, topic, size);

create table arena_private.rooms (
  id uuid primary key default gen_random_uuid(),
  code text not null unique default ('WQ-' || upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 8))),
  host uuid not null references arena_private.players,
  mode text not null check (mode in ('fast','duel','team','yds','oral','daily')),
  level text not null, topic text not null, size int not null check(size between 3 and 20),
  seconds int not null check(seconds between 60 and 900),
  training boolean not null default false,
  referee boolean not null default false,
  phase text not null default 'lobby' check(phase in ('lobby','playing','grading','finished')),
  created_at timestamptz not null default clock_timestamp(),
  starts_at timestamptz, ends_at timestamptz, finished_at timestamptz,
  content jsonb not null, height int, width int,
  winner smallint, referee_at timestamptz, grading_skipped boolean not null default false,
  rematch_id uuid references arena_private.rooms
);
create table arena_private.members (
  room_id uuid not null references arena_private.rooms on delete cascade,
  user_id uuid not null references arena_private.players,
  seat smallint not null check (seat in (1,2)),
  ready boolean not null default false,
  seen timestamptz not null default clock_timestamp(),
  primary key(room_id, user_id), unique(room_id, seat)
);
create index arena_member_history on arena_private.members(user_id, room_id);
create table arena_private.attempts (
  room_id uuid not null,
  user_id uuid not null,
  ordinal int not null,
  answer text not null,
  correct boolean,
  points int not null,
  elapsed_ms int not null,
  submitted_at timestamptz not null,
  rubric jsonb,
  primary key(room_id, user_id, ordinal),
  foreign key(room_id, user_id) references arena_private.members on delete cascade
);
create table arena_private.hints (
  room_id uuid not null, user_id uuid not null, ordinal int not null,
  primary key(room_id, user_id, ordinal),
  foreign key(room_id, user_id) references arena_private.members on delete cascade
);
create table arena_private.chat (
  id bigint generated always as identity primary key,
  room_id uuid not null references arena_private.rooms on delete cascade,
  seat smallint not null check(seat in (0,1,2)),
  body text not null check(char_length(body) between 1 and 2200),
  sent_at timestamptz not null default clock_timestamp()
);
create index arena_chat_room on arena_private.chat(room_id,id desc);
create table arena_private.tutor (
  id bigint generated always as identity primary key,
  user_id uuid not null references arena_private.players,
  prompt text not null, reply text not null,
  created_at timestamptz not null default clock_timestamp()
);
create index arena_tutor_player on arena_private.tutor(user_id,id desc);
create table arena_private.ai_usage (
  user_id uuid not null references arena_private.players,
  day date not null,
  provider text not null,
  calls int not null default 0,
  primary key(user_id, day, provider)
);
create table arena_private.ai_jobs (
  room_id uuid not null references arena_private.rooms on delete cascade,
  purpose text not null check(purpose in ('grade','summary','question')),
  started_at timestamptz not null default clock_timestamp(),
  primary key(room_id,purpose)
);

-- Realtime receives a revision only. All useful data is scoped in arena_rpc().
create table public.arena_events (
  room_id uuid primary key references arena_private.rooms on delete cascade,
  revision bigint not null default 0
);
alter table public.arena_events enable row level security;
revoke all on public.arena_events from public, anon, authenticated;
grant select on public.arena_events to authenticated;
create function arena_private.member_of(rid uuid) returns boolean
language sql stable security definer set search_path = '' as $$
  select exists(select 1 from arena_private.members where room_id=rid and user_id=auth.uid())
$$;
grant usage on schema arena_private to authenticated;
revoke all on function arena_private.member_of(uuid) from public, anon;
grant execute on function arena_private.member_of(uuid) to authenticated;
create policy arena_events_member on public.arena_events for select to authenticated
  using (arena_private.member_of(room_id));
alter publication supabase_realtime add table public.arena_events;

create function arena_private.touch(rid uuid) returns void language sql set search_path = '' as $$
  insert into public.arena_events(room_id) values(rid)
  on conflict(room_id) do update set revision=arena_events.revision+1
$$;

create function arena_private.letters(a text) returns text language sql immutable set search_path = '' as $$
  select regexp_replace(upper(translate(a,'ıİ','II') collate "C"), '[^A-Z]', '', 'g')
$$;

create function arena_private.score(rid uuid, uid uuid) returns jsonb
language sql stable set search_path = '' as $$
  select jsonb_build_object(
    'answered',count(*), 'correct',count(*) filter(where correct is true), 'wrong',count(*) filter(where correct is false),
    'points',coalesce(sum(points),0), 'averageMs',coalesce(round(avg(elapsed_ms)),0),
    'hintCount',(select count(*) from arena_private.hints where room_id=rid and user_id=uid),
    'lastAt',max(submitted_at)
  )
  from arena_private.attempts where room_id=rid and user_id=uid
$$;

create function arena_private.finish(r arena_private.rooms, skip_grade boolean default false) returns void
language plpgsql set search_path = '' as $$
declare first jsonb; second jsonb; first_points int; second_points int; victor int;
begin
  if r.phase not in ('playing','grading') then return; end if;
  if r.mode <> 'team' and not skip_grade then
    select arena_private.score(r.id,user_id) into first from arena_private.members where room_id=r.id and seat=1;
    select arena_private.score(r.id,user_id) into second from arena_private.members where room_id=r.id and seat=2;
    first_points := (first->>'points')::int; second_points := (second->>'points')::int;
    if first_points<>second_points then victor := case when first_points>second_points then 1 else 2 end;
    elsif r.mode='duel' and (first->>'correct')::int=r.size and (second->>'correct')::int=r.size
        and abs(extract(epoch from (first->>'lastAt')::timestamptz-(second->>'lastAt')::timestamptz))>=1 then
      victor := case when (first->>'lastAt')::timestamptz<(second->>'lastAt')::timestamptz then 1 else 2 end;
    end if;
  end if;
  update arena_private.rooms set phase='finished', finished_at=clock_timestamp(), winner=victor, grading_skipped=skip_grade
    where id=r.id and phase in ('playing','grading');
  if found then
    insert into arena_private.chat(room_id,seat,body) values
      (r.id,0,case when skip_grade then 'Sözlü sınav değerlendirme olmadan kapatıldı.'
                   when victor is null then 'Yarışma tamamlandı.'
                   else victor || '. oyuncu yarışmayı kazandı.' end);
    perform arena_private.touch(r.id);
  end if;
end $$;

create function arena_private.settle(r arena_private.rooms) returns arena_private.rooms
language plpgsql set search_path = '' as $$
declare n int; solved int;
begin
  if r.phase <> 'playing' then return r; end if;
  select count(*),count(distinct ordinal) filter(where correct is true) into n,solved
    from arena_private.attempts where room_id=r.id;
  if clock_timestamp()<r.ends_at and n<2*r.size and (r.mode<>'team' or solved<r.size) then return r; end if;
  if r.mode='oral' and n>0 then
    update arena_private.rooms set phase='grading' where id=r.id;
    perform arena_private.touch(r.id);
  else
    perform arena_private.finish(r, r.mode='oral');
  end if;
  select * into r from arena_private.rooms where id=r.id;
  return r;
end $$;

create function arena_private.snapshot(r arena_private.rooms, uid uuid) returns jsonb
language plpgsql set search_path = '' as $$
declare seat_no int; completed int; my_count int; turn_seat int;
  active boolean := r.phase='playing' and clock_timestamp()>=r.starts_at;
  item_list jsonb := '[]'; members jsonb; mine jsonb; shared jsonb; messages jsonb; breakdown jsonb;
begin
  select seat into strict seat_no from arena_private.members where room_id=r.id and user_id=uid;
  select count(*) into my_count from arena_private.attempts where room_id=r.id and user_id=uid;
  select count(*) into completed from arena_private.attempts where room_id=r.id;
  if active and r.mode='oral' then turn_seat:=completed%2+1; end if;
  if active or r.phase in ('grading','finished') then
    select coalesce(jsonb_agg((item-'answer'-'explanation') || jsonb_build_object('ordinal',ord) order by ord),'[]') into item_list
    from jsonb_array_elements(r.content) with ordinality as x(item,ord)
    where r.mode in ('duel','team') or
      (active and r.mode='oral' and ord=completed/2+1) or
      (active and r.mode not in ('duel','team','oral') and ord=my_count+1);
  end if;
  select coalesce(jsonb_agg(jsonb_build_object(
    'seat',m.seat,'name',p.name,'ready',m.ready,'online',m.seen>clock_timestamp()-interval '50 seconds'
  ) || arena_private.score(r.id,m.user_id) order by m.seat),'[]') into members
    from arena_private.members m join arena_private.players p using(user_id) where m.room_id=r.id;
  select coalesce(jsonb_agg(jsonb_build_object('ordinal',a.ordinal,'correct',a.correct,'points',a.points,
    'entry',case when r.phase<>'finished' and r.mode in ('duel','team') and not a.correct then null else a.answer end,
    'solution',case when r.phase='finished' or (r.mode in ('duel','team') and a.correct)
      then (r.content->(a.ordinal-1))->>'answer' end,
    'prompt',case when r.phase='finished' then (r.content->(a.ordinal-1))->>'prompt' end,
    'explanation',case when r.phase='finished' then (r.content->(a.ordinal-1))->>'explanation' end,
    'rubric',a.rubric) order by a.ordinal),'[]') into mine
    from arena_private.attempts a where a.room_id=r.id and a.user_id=uid;
  select coalesce(jsonb_agg(jsonb_build_object('ordinal',a.ordinal,'answer',r.content->(a.ordinal-1)->>'answer') order by a.ordinal),'[]') into shared
    from arena_private.attempts a where r.mode='team' and a.room_id=r.id and a.correct;
  select coalesce(jsonb_agg(to_jsonb(c)-'room_id' order by c.id),'[]') into messages from
    (select * from arena_private.chat where room_id=r.id order by id desc limit 60) c;
  if r.phase='finished' then
    select coalesce(jsonb_agg(to_jsonb(b) order by b.seat,b.topic,b.kind),'[]') into breakdown from (
      select m.seat, (r.content->(a.ordinal-1))->>'topic' as topic, (r.content->(a.ordinal-1))->>'kind' as kind,
        count(*) as attempts, count(*) filter(where a.correct) as correct, sum(a.points) as points
      from arena_private.attempts a join arena_private.members m using(room_id,user_id)
      where a.room_id=r.id group by 1,2,3
    ) b;
  end if;
  return jsonb_build_object(
    'id',r.id,'code',r.code,'mode',r.mode,'level',r.level,'topic',r.topic,'size',r.size,'seconds',r.seconds,
    'training',r.training,'referee',r.referee,'phase',r.phase,'seat',seat_no,'turnSeat',turn_seat,
    'height',r.height,'width',r.width,'serverMs',floor(extract(epoch from clock_timestamp())*1000),
    'startMs',floor(extract(epoch from r.starts_at)*1000),'endMs',floor(extract(epoch from r.ends_at)*1000),
    'players',members,'items',item_list,'mine',mine,'team',shared,'chat',messages,'breakdown',coalesce(breakdown,'[]'),
    'hints',coalesce((select jsonb_object_agg(h.ordinal,left(arena_private.letters(r.content->(h.ordinal-1)->>'answer'),1))
      from arena_private.hints h where h.room_id=r.id and h.user_id=uid),'{}'),
    'refereeAt',r.referee_at,'teamPoints',coalesce((select sum(points) from arena_private.attempts where room_id=r.id and r.mode='team'),0),
    'winner',r.winner,'gradingSkipped',r.grading_skipped,
    'rematchCode',(select code from arena_private.rooms where id=r.rematch_id));
end $$;

create function arena_private.catalog() returns jsonb language sql stable set search_path = '' as $$
  with groups as (
    select mode,level,topic,count(*)::int as n from arena_private.items group by 1,2,3
    union all select mode,level,'',count(*)::int from arena_private.items group by 1,2
  ), layouts as (
    select level,topic,array_agg(distinct size order by size) as counts from arena_private.boards group by 1,2
  )
  select coalesce(jsonb_agg(row_to_json(x) order by mode,level,topic),'[]') from (
    select mode,level,topic,case when mode='oral' then array(select i from unnest(array[3,5]) i where i<=n)
      else array(select i from unnest(array[8,10,15,20]) i where i<=n) end as counts from groups
    union all select 'duel',level,topic,counts from layouts
    union all select 'team',level,topic,counts from layouts
    union all select 'daily','B1','',array[10]
  ) x where cardinality(counts)>0
$$;

create function arena_private.study(uid uuid) returns jsonb language sql stable set search_path = '' as $$
  select coalesce(jsonb_agg(to_jsonb(w) order by misses desc,last_seen desc),'[]') from (
    select r.content->(a.ordinal-1)->>'answer' as answer, max(r.content->(a.ordinal-1)->>'prompt') as clue,
      max(r.content->(a.ordinal-1)->>'kind') as kind, count(*) filter(where not a.correct) as misses,
      max(r.finished_at) as last_seen
    from arena_private.attempts a join arena_private.rooms r on r.id=a.room_id
    where a.user_id=uid and r.phase='finished' and r.mode<>'oral'
    group by 1 having count(*) filter(where not a.correct)>0
    order by misses desc,last_seen desc limit 50
  ) w
$$;

create function public.arena_rpc(action text, data jsonb default '{}') returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
  uid uuid:=auth.uid(); player arena_private.players; r arena_private.rooms; old arena_private.rooms;
  mode_name text; lvl text; subject text; question_count int; duration int; allow_hint boolean; include_referee boolean;
  pack jsonb; grid arena_private.boards; other_seat int; my_seat int; index_no int;
  text_value text; expected text; matching boolean; award int; time_open timestamptz;
  t timestamptz:=statement_timestamp(); seen_before timestamptz; history jsonb; rank jsonb;
begin
  select * into player from arena_private.players where user_id=uid;
  if not found then raise exception 'arena_access'; end if;
  if action='catalog' then return arena_private.catalog(); end if;
  if action='study' then return arena_private.study(uid); end if;
  if action='profile' then return jsonb_build_object('name',player.name,
    'provider',player.provider,'model',player.model,'refereeProvider',player.referee_provider,'refereeModel',player.referee_model,
    'fallback',player.fallback, 'usage',coalesce((select jsonb_object_agg(provider,calls) from arena_private.ai_usage
      where user_id=uid and day=(clock_timestamp() at time zone 'UTC')::date),'{}'));
  end if;
  if action='tutor-history' then return coalesce((select jsonb_agg(to_jsonb(x) order by x.id)
    from (select id,prompt,reply,created_at from arena_private.tutor where user_id=uid order by id desc limit 80) x),'[]'); end if;
  if action='history' then
    for r in select x.* from arena_private.rooms x join arena_private.members m on m.room_id=x.id
      where m.user_id=uid and x.phase='playing' and x.ends_at<=clock_timestamp() limit 25 for update of x
    loop perform arena_private.settle(r); end loop;
    select coalesce(jsonb_agg(to_jsonb(h) order by h.created_at desc),'[]') into history from (
      select x.code,x.mode,x.level,x.topic,x.phase,x.created_at,x.finished_at,x.winner,m.seat,
        arena_private.score(x.id,uid) as score
      from arena_private.rooms x join arena_private.members m on m.room_id=x.id
      where m.user_id=uid order by x.created_at desc limit 100
    ) h;
    select coalesce(jsonb_agg(to_jsonb(s) order by s.points desc,s.wins desc,s.name),'[]') into rank from (
      select p.name, p.user_id=uid as me, sum(coalesce((arena_private.score(x.id,m.user_id)->>'points')::int,0)) as points,
        count(*) filter(where x.winner=m.seat) as wins, count(*) as matches,
        sum((arena_private.score(x.id,m.user_id)->>'correct')::int) as correct,
        sum((arena_private.score(x.id,m.user_id)->>'wrong')::int) as wrong
      from arena_private.rooms x join arena_private.members m on m.room_id=x.id
      join arena_private.players p on p.user_id=m.user_id
      where x.phase='finished' and not x.grading_skipped
        and exists(select 1 from arena_private.members mine where mine.room_id=x.id and mine.user_id=uid)
      group by p.user_id,p.name
    ) s;
    return jsonb_build_object('rooms',history,'rank',rank);
  end if;
  if action='create' then
    -- The player's row also serializes simultaneous room creation requests.
    perform 1 from arena_private.players where user_id=uid for update;
    if (select count(*) from arena_private.rooms where host=uid and created_at>t-interval '1 hour')>=20 then
      raise exception 'arena_room_limit';
    end if;
    mode_name:=coalesce(data->>'mode','fast'); lvl:=coalesce(data->>'level','B1'); subject:=coalesce(data->>'topic','');
    question_count:=(data->>'size')::int; duration:=(data->>'seconds')::int;
    allow_hint:=coalesce((data->>'training')::boolean,false) and mode_name not in ('oral','daily');
    include_referee:=mode_name='oral' or coalesce((data->>'referee')::boolean,false);
    if question_count is null or duration not in (60,120,180,300,600,900) or duration is null then raise exception 'arena_settings'; end if;
    if mode_name='daily' then lvl:='B1'; subject:=''; question_count:=10; duration:=120; end if;
    if mode_name in ('duel','team') then
      select * into grid from arena_private.boards where level=lvl and topic=subject and size=question_count order by random() limit 1;
      if not found then raise exception 'arena_pack'; end if;
      pack:=grid.words;
    elsif mode_name in ('fast','daily','yds','oral') then
      if (mode_name='oral' and question_count not in (3,5)) or (mode_name<>'oral' and question_count not in (8,10,15,20))
        then raise exception 'arena_settings'; end if;
      select jsonb_agg(to_jsonb(q)-'id'-'level'-'mode' order by q.ordering) into pack from (
        select i.*, case when mode_name='daily' then md5(i.id||(t at time zone 'Europe/Istanbul')::date::text) else random()::text end as ordering
        from arena_private.items i where i.mode=(case when mode_name='daily' then 'fast' else mode_name end) and i.level=lvl
          and (subject='' or i.topic=subject)
          and (mode_name='oral' or subject<>'' or not exists(select 1 from arena_private.items earlier where earlier.mode=i.mode and earlier.level=i.level
                    and earlier.answer=i.answer and earlier.id<i.id))
        order by ordering limit question_count
      ) q;
      if coalesce(jsonb_array_length(pack),0)<>question_count then raise exception 'arena_pack'; end if;
      select jsonb_agg(v-'ordering' order by ord) into pack from jsonb_array_elements(pack) with ordinality a(v,ord);
    else raise exception 'arena_settings';
    end if;
    insert into arena_private.rooms(host,mode,level,topic,size,seconds,training,referee,content,height,width)
      values(uid,mode_name,lvl,subject,question_count,duration,allow_hint,include_referee,pack,grid.height,grid.width) returning * into r;
    insert into arena_private.members(room_id,user_id,seat) values(r.id,uid,1);
    perform arena_private.touch(r.id);
    return arena_private.snapshot(r,uid);
  end if;

  select * into r from arena_private.rooms where code=upper(trim(coalesce(data->>'code',''))) for update;
  if not found then raise exception 'arena_missing'; end if;
  select seat,seen into my_seat,seen_before from arena_private.members where room_id=r.id and user_id=uid;
  if my_seat is null then
    if action<>'join' then raise exception 'arena_access'; end if;
    if r.phase<>'lobby' or exists(select 1 from arena_private.members where room_id=r.id and seat=2) then
      raise exception 'arena_full';
    end if;
    if r.created_at<t-interval '7 days' then raise exception 'arena_expired'; end if;
    insert into arena_private.members(room_id,user_id,seat) values(r.id,uid,2);
    my_seat:=2;
    perform arena_private.touch(r.id);
  end if;
  if r.phase in ('lobby','playing') and (seen_before is null or seen_before<t-interval '19 seconds') then
    update arena_private.members set seen=clock_timestamp() where room_id=r.id and user_id=uid;
    if seen_before<t-interval '50 seconds' then
      if r.phase='lobby' then update arena_private.members set ready=false where room_id=r.id and user_id=uid; end if;
      perform arena_private.touch(r.id);
    end if;
  end if;
  -- Receive time and submission validation share the same row lock.
  if action<>'answer' or t>r.ends_at then r:=arena_private.settle(r); end if;
  if action in ('join','state') then return arena_private.snapshot(r,uid); end if;
  if action='chat-older' then
    return coalesce((select jsonb_agg(to_jsonb(c)-'room_id' order by c.id) from
      (select * from arena_private.chat where room_id=r.id and id<(data->>'before')::bigint order by id desc limit 60) c),'[]');
  end if;

  if action='ready' then
    if r.phase<>'lobby' then return arena_private.snapshot(r,uid); end if;
    if r.mode='oral' and not coalesce((data->>'consent')::boolean,false) and coalesce((data->>'ready')::boolean,false)
      then raise exception 'arena_consent'; end if;
    update arena_private.members set ready=coalesce((data->>'ready')::boolean,false),seen=clock_timestamp() where room_id=r.id and user_id=uid;
    if (select count(*) from arena_private.members where room_id=r.id and ready and seen>t-interval '50 seconds')=2 then
      update arena_private.rooms set phase='playing', starts_at=clock_timestamp()+interval '4 seconds',
        ends_at=clock_timestamp()+make_interval(secs=>r.seconds+4) where id=r.id returning * into r;
      insert into arena_private.chat(room_id,seat,body) values(r.id,0,'İki oyuncu hazır. Yarışma başlıyor.');
    end if;
  elsif action in ('answer','hint') then
    if r.phase<>'playing' or t<r.starts_at or t>r.ends_at then
      return arena_private.snapshot(arena_private.settle(r),uid);
    end if;
    index_no:=(data->>'ordinal')::int;
    if index_no is null or index_no not between 1 and r.size then raise exception 'arena_question'; end if;
    if exists(select 1 from arena_private.attempts where room_id=r.id and user_id=uid and ordinal=index_no)
        or (r.mode='team' and exists(select 1 from arena_private.attempts where room_id=r.id and ordinal=index_no and correct)) then
      return arena_private.snapshot(r,uid);
    end if;
    if r.mode not in ('duel','team','oral') and index_no<>(select count(*)+1 from arena_private.attempts where room_id=r.id and user_id=uid) then
      raise exception 'arena_question';
    end if;
    if r.mode='oral' then
      select count(*) into other_seat from arena_private.attempts where room_id=r.id;
      if index_no<>other_seat/2+1 or my_seat<>other_seat%2+1 then raise exception 'arena_turn'; end if;
    end if;
    expected:=r.content->(index_no-1)->>'answer';
    if action='hint' then
      if not r.training or r.mode='oral' then raise exception 'arena_hint'; end if;
      insert into arena_private.hints values(r.id,uid,index_no) on conflict do nothing;
    else
      text_value:=btrim(coalesce(data->>'answer',''));
      if text_value='' or char_length(text_value)>(case when r.mode='oral' then 900 else 150 end) then raise exception 'arena_answer'; end if;
      if r.mode<>'oral' and arena_private.letters(text_value)='' then raise exception 'arena_answer'; end if;
      matching:=case when r.mode<>'oral' then arena_private.letters(text_value)=arena_private.letters(expected) end;
      award:=case when matching and exists(select 1 from arena_private.hints where room_id=r.id and user_id=uid and ordinal=index_no) then 5
                  when matching then 10 else 0 end;
      if r.mode in ('duel','team') then time_open:=r.starts_at;
      elsif r.mode='oral' then select coalesce(max(submitted_at),r.starts_at) into time_open from arena_private.attempts where room_id=r.id;
      else select coalesce(max(submitted_at),r.starts_at) into time_open from arena_private.attempts where room_id=r.id and user_id=uid;
      end if;
      insert into arena_private.attempts(room_id,user_id,ordinal,answer,correct,points,elapsed_ms,submitted_at)
        values(r.id,uid,index_no,text_value,matching,award,greatest(0,round(extract(epoch from t-time_open)*1000)::int),t);
      r:=arena_private.settle(r);
    end if;
  elsif action='chat' then
    text_value:=btrim(coalesce(data->>'message',''));
    if char_length(text_value) not between 1 and 600 then raise exception 'arena_message'; end if;
    if (select count(*) from arena_private.chat where room_id=r.id and seat=my_seat and sent_at>t-interval '1 minute')>=12 then
      raise exception 'arena_chat_limit';
    end if;
    insert into arena_private.chat(room_id,seat,body) values(r.id,my_seat,text_value);
  elsif action='rematch' then
    if r.phase<>'finished' then raise exception 'arena_stage'; end if;
    if r.rematch_id is not null then
      select * into old from arena_private.rooms where id=r.rematch_id;
    else
      if (select count(*) from arena_private.rooms where host=r.host and created_at>t-interval '1 hour')>=20 then raise exception 'arena_room_limit'; end if;
      pack:=r.content;
      if r.mode in ('duel','team') then
        select * into grid from arena_private.boards where level=r.level and topic=r.topic and size=r.size
          order by (words=r.content),random() limit 1;
        pack:=coalesce(grid.words,pack);
      elsif r.mode<>'daily' then
        select jsonb_agg(v order by random()) into pack from jsonb_array_elements(r.content) x(v);
      end if;
      insert into arena_private.rooms(host,mode,level,topic,size,seconds,training,referee,content,height,width)
        values(r.host,r.mode,r.level,r.topic,r.size,r.seconds,r.training,r.referee,pack,coalesce(grid.height,r.height),coalesce(grid.width,r.width)) returning * into old;
      insert into arena_private.members(room_id,user_id,seat,seen) select old.id,user_id,seat,
        case when user_id=uid then t else 'epoch'::timestamptz end from arena_private.members where room_id=r.id;
      update arena_private.rooms set rematch_id=old.id where id=r.id;
      perform arena_private.touch(old.id);
      perform arena_private.touch(r.id);
    end if;
    return arena_private.snapshot(old,uid);
  elsif action='close-oral' then
    if r.phase<>'grading' then raise exception 'arena_stage'; end if;
    perform arena_private.finish(r,true);
    select * into r from arena_private.rooms where id=r.id;
  else raise exception 'arena_action';
  end if;
  perform arena_private.touch(r.id);
  return arena_private.snapshot(r,uid);
end $$;

revoke all on all tables in schema arena_private from public, anon, authenticated;
revoke all on all sequences in schema arena_private from public, anon, authenticated;
revoke all on all functions in schema arena_private from public, anon, authenticated;
grant execute on function arena_private.member_of(uuid) to authenticated;
revoke all on function public.arena_rpc(text,jsonb) from public, anon, authenticated;
grant execute on function public.arena_rpc(text,jsonb) to authenticated;
commit;
