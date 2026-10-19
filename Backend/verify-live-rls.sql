-- Local-first contract. Disposable fixtures are ALL rolled back; no email is sent.
begin;
do $$
declare a uuid:=gen_random_uuid(); b uuid:=gen_random_uuid(); c uuid:=gen_random_uuid();
 publication uuid:=gen_random_uuid(); direct uuid:=gen_random_uuid(); replica uuid:=gen_random_uuid();
 payload jsonb:='{"name":"Prueba","recipeKind":"BLACK_COFFEE","ingredients":[{"name":"Café","amount":15,"unit":"g"}],"steps":[{"stepNumber":1,"instruction":"Preparar"}]}';
 t text; rejected boolean;
begin
 insert into auth.users(id,email,raw_user_meta_data) values
 (a,a::text||'@example.invalid',jsonb_build_object('display_name','Test A','handle','test_'||replace(a::text,'-',''))),
 (b,b::text||'@example.invalid',jsonb_build_object('display_name','Test B','handle','test_'||replace(b::text,'-',''))),
 (c,c::text||'@example.invalid',jsonb_build_object('display_name','Test C','handle','test_'||replace(c::text,'-','')));
 perform set_config('request.jwt.claim.sub',a::text,true); set local role authenticated;
 foreach t in array array['beans','coffee_beans','grinders','equipment','recipes','recipe_ingredients','recipe_steps','techniques','technique_steps','brew_sessions','tastings','tasting_observations','cup_sessions','lab_experiments'] loop
   rejected:=false;
   begin execute format('select 1 from public.%I limit 1',t);
   exception when insufficient_privilege then rejected:=true; end;
   if not rejected then raise exception 'Library table % still exposed',t; end if;
 end loop;
 update public.profiles set is_private=false where id=a;
 insert into public.shares(id,entity_type,entity_id,from_name,from_handle,name,payload_snapshot_json)
 values(publication,'recipe',gen_random_uuid(),'Fake','fake','Prueba',payload);
 if not exists(select 1 from public.shares where id=publication and from_name='Test A' and original_author_user_id=a) then raise exception 'Creator not derived from account'; end if;
 insert into public.shares(id,entity_type,entity_id,from_name,from_handle,name,payload_snapshot_json,visibility,target_user_id)
 values(direct,'recipe',gen_random_uuid(),'Fake','fake','Directo',payload,'direct',b);
 perform set_config('request.jwt.claim.sub',b::text,true);
 insert into public.share_likes(share_id) values(publication) on conflict do nothing;
 insert into public.share_likes(share_id) values(publication) on conflict do nothing;
 if not exists(select 1 from public.shares where id=publication and like_count=1) then raise exception 'Duplicate likes changed count'; end if;
 insert into public.shares(id,entity_type,entity_id,from_name,from_handle,name,payload_snapshot_json,visibility,target_user_id,source_share_id)
 values(replica,'recipe',gen_random_uuid(),'Fake','fake','Variante',payload,'direct',a,publication);
 if not exists(select 1 from public.shares where id=replica and original_author_user_id=a and original_author_name='Test A' and from_user_id=b) then raise exception 'Copy lost original author'; end if;
 if (select count(*) from public.inbox_items where share_id=direct)<>1 then raise exception 'Direct inbox missing'; end if;
 update public.inbox_items set read_at=now() where share_id=direct;
 rejected:=false;
 begin update public.shares set like_count=500 where id=publication;
 exception when insufficient_privilege then rejected:=true; end;
 if not rejected then raise exception 'Like counter write allowed'; end if;
 perform set_config('request.jwt.claim.sub',c::text,true);
 if exists(select 1 from public.shares where id in(direct,replica)) then raise exception 'Third party sees direct share'; end if;
 perform set_config('request.jwt.claim.sub',b::text,true);
 insert into public.blocked_users(blocked_user_id) values(a);
 if exists(select 1 from public.shares where id in(direct,publication)) then raise exception 'Block bypassed'; end if;
 perform set_config('request.jwt.claim.sub',a::text,true);
 if exists(select 1 from public.resolve_profile_alias('test_'||replace(b::text,'-',''))) then raise exception 'Reverse block bypassed'; end if;
 update public.profiles set is_private=true where id=a;
 perform set_config('request.jwt.claim.sub',b::text,true);
 delete from public.share_likes where share_id=publication;
 reset role;
 if (select like_count from public.shares where id=publication)<>0 then raise exception 'Unlike hidden share failed'; end if;
 if exists(select 1 from public.recipes where owner_id in(a,b,c)) then raise exception 'Private recipe copied into cloud'; end if;
 if (select count(*) from public.community_creations where creator_id=a)<>2 then raise exception 'Creator registry mismatch'; end if;
end $$;
select 'PASS: local-only library, account-derived creator, public/direct snapshots, original attribution, inbox, idempotent likes, protected counts, symmetric blocks; all fixtures rolled back' as verification;
rollback;
