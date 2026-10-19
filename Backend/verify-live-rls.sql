-- SQL role simulation against the deployed project. No email is sent and ALL
-- fixtures are rolled back, including auth.users. Not a substitute for JWT/device tests.
begin;
do $$
declare a uuid:=gen_random_uuid(); b uuid:=gen_random_uuid(); c uuid:=gen_random_uuid();
  bean uuid:=gen_random_uuid(); recipe uuid:=gen_random_uuid(); shared uuid:=gen_random_uuid();
  direct uuid:=gen_random_uuid(); rejected boolean;
begin
  insert into auth.users(id,email,raw_user_meta_data) values
    (a,a::text||'@example.invalid',jsonb_build_object('display_name','Test A','handle','test_'||replace(a::text,'-',''))),
    (b,b::text||'@example.invalid',jsonb_build_object('display_name','Test B','handle','test_'||replace(b::text,'-',''))),
    (c,c::text||'@example.invalid',jsonb_build_object('display_name','Test C','handle','test_'||replace(c::text,'-','')));
  if (select count(*) from public.profiles where id in(a,b,c) and is_private)<>3 then raise exception 'Profile provisioning failed'; end if;
  perform set_config('request.jwt.claim.sub',a::text,true);
  set local role authenticated;
  insert into public.beans(id,name,stock_grams,brew_profiles) values(bean,'Test grain',250,'{"v60":{"clicks":22,"temperatureC":90.5555555556}}');
  insert into public.recipes(id,name) values(recipe,'Test recipe');
  insert into public.recipe_steps(recipe_id,instruction,step_number) values(recipe,'Test step',1);
  insert into public.cup_sessions(precise_temperature_c) values(90.5555555556);
  update public.profiles set is_private=false where id=a;
  insert into public.shares(id,entity_type,entity_id,from_name,from_handle,name,payload_snapshot_json)
    values(shared,'recipe',recipe,'Test A','test_'||replace(a::text,'-',''),'Test public','{}');
  insert into public.shares(id,entity_type,entity_id,from_name,from_handle,name,payload_snapshot_json,visibility,target_user_id)
    values(direct,'recipe',recipe,'Test A','test_'||replace(a::text,'-',''),'Test direct','{}','direct',b);
  perform set_config('request.jwt.claim.sub',b::text,true);
  if exists(select 1 from public.beans where id=bean) then raise exception 'Private bean leaked'; end if;
  if exists(select 1 from public.recipe_steps where recipe_id=recipe) then raise exception 'Private steps leaked'; end if;
  update public.beans set name='Intrusion' where id=bean;
  if found then raise exception 'Cross-account update allowed'; end if;
  rejected:=false;
  begin insert into public.recipe_steps(recipe_id,instruction,step_number) values(recipe,'Intrusion',2);
  exception when foreign_key_violation then rejected:=true; end;
  if not rejected then raise exception 'Cross-account parent accepted'; end if;
  if (select count(*) from public.shares where id in(shared,direct))<>2 then raise exception 'Share visibility failed'; end if;
  if (select count(*) from public.inbox_items where share_id=direct and target_user_id=b)<>1 then raise exception 'Inbox trigger failed'; end if;
  update public.inbox_items set read_at=now() where share_id=direct;
  rejected:=false;
  begin update public.inbox_items set target_user_id=c where share_id=direct;
  exception when insufficient_privilege then rejected:=true; end;
  if not rejected then raise exception 'Inbox retargeting allowed'; end if;
  insert into public.share_saves(share_id) values(shared);
  perform set_config('request.jwt.claim.sub',c::text,true);
  if exists(select 1 from public.shares where id=direct) then raise exception 'Direct share leaked to third party'; end if;
  if exists(select 1 from public.share_saves where share_id=shared) then raise exception 'Private save leaked'; end if;
  perform set_config('request.jwt.claim.sub',b::text,true);
  insert into public.blocked_users(blocked_user_id) values(a);
  if exists(select 1 from public.shares where id in(shared,direct)) then raise exception 'Block did not hide shares'; end if;
  perform set_config('request.jwt.claim.sub',a::text,true);
  if exists(select 1 from public.resolve_profile_alias('test_'||replace(b::text,'-',''))) then raise exception 'Reverse block bypassed in alias lookup'; end if;
  update public.profiles set is_private=true where id=a;
  perform set_config('request.jwt.claim.sub',c::text,true);
  if exists(select 1 from public.shares where id=shared) then raise exception 'Private profile public share leaked'; end if;
  reset role;
  -- Account deletion must preserve FK integrity (still inside rollback-only fixtures).
  delete from auth.users where id=b;
  if exists(select 1 from public.shares where id=direct) then raise exception 'Deleted recipient direct share orphaned'; end if;
  perform set_config('request.jwt.claim.sub','',true);
  set local role anon;
  rejected:=false;
  begin perform 1 from public.beans limit 1;
  exception when insufficient_privilege then rejected:=true; end;
  if not rejected then raise exception 'Anonymous inventory access allowed'; end if;
  reset role;
end $$;
select 'PASS: deployed RLS, profile trigger, cups, precision, child ownership, public/direct shares, inbox, private saves, symmetric blocks, profile privacy, deletion, anonymous denial' as verification;
rollback;
