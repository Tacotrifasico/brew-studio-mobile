-- Current product scope: local library; cloud accounts and explicit social snapshots only.
begin;
revoke all on public.beans,public.coffee_beans,public.grinders,public.equipment,
 public.recipes,public.recipe_ingredients,public.recipe_steps,public.techniques,public.technique_steps,
 public.brew_sessions,public.tastings,public.tasting_observations,public.cup_sessions,public.lab_experiments
 from authenticated,anon,public;

create table public.community_creations (
 id uuid primary key default gen_random_uuid(),
 creator_id uuid references auth.users(id) on delete set null,
 creator_name text not null, entity_type text not null check(entity_type in('recipe','technique')),
 local_entity_id uuid not null, created_at timestamptz not null default now(),
 unique(creator_id,entity_type,local_entity_id)
);
alter table public.community_creations enable row level security;
alter table public.shares add column creation_id uuid references public.community_creations(id),
 add column source_share_id uuid references public.shares(id) on delete set null,
 add column like_count bigint not null default 0 check(like_count>=0);
create index shares_creation_idx on public.shares(creation_id);
create index shares_source_idx on public.shares(source_share_id);
create policy creation_read on public.community_creations for select to authenticated using (
 creator_id=(select auth.uid()) or exists(select 1 from public.shares s where s.creation_id=community_creations.id and private.share_visible(s.id)));
revoke all on public.community_creations from public,anon,authenticated;
grant select on public.community_creations to authenticated;

create function private.validate_social_snapshot(kind text,p jsonb) returns void
language plpgsql set search_path='' as $$
declare step jsonb; ingredient jsonb; dose numeric; water integer; temp numeric;
 ratio numeric; total_water integer:=0; total_seconds integer:=0; idx integer:=0; sec integer; added integer;
begin
 if jsonb_typeof(p)<>'object' or octet_length(p::text)>65536 or length(btrim(coalesce(p->>'name','')))=0
   or jsonb_typeof(p->'steps') is distinct from 'array' then
   raise exception using errcode='23514',message='Complete snapshot required (maximum 64 KiB)'; end if;
 if jsonb_array_length(p->'steps') not between 1 and 80 then raise exception using errcode='23514',message='Between 1 and 80 steps required'; end if;
 if kind='recipe' then
   if jsonb_typeof(p->'ingredients') is distinct from 'array' then raise exception using errcode='23514',message='Ingredients required'; end if;
   if jsonb_array_length(p->'ingredients') not between 1 and 80 then raise exception using errcode='23514',message='Invalid ingredients'; end if;
   for ingredient in select value from jsonb_array_elements(p->'ingredients') loop
     if btrim(coalesce(ingredient->>'name',''))='' or btrim(coalesce(ingredient->>'unit',''))=''
       or coalesce((ingredient->>'amount')::numeric,0)<=0 then raise exception using errcode='23514',message='Invalid ingredient'; end if;
   end loop;
   for step in select value from jsonb_array_elements(p->'steps') loop
     idx:=idx+1;
     if btrim(coalesce(step->>'instruction',''))='' or coalesce((step->>'stepNumber')::integer,idx)<>idx
       or (step->>'durationSeconds' is not null and (step->>'durationSeconds')::integer<=0) then
       raise exception using errcode='23514',message='Invalid recipe step'; end if;
   end loop;
 elsif kind='technique' then
   dose:=coalesce(p->>'doseG',p->>'coffeeGrams',p->>'doseGrams')::numeric;
   water:=(p->>'waterMl')::integer; ratio:=(p->>'ratio')::numeric;
   temp:=coalesce(p->>'temperatureC',p->>'temperature')::numeric;
   if dose is null or water is null or ratio is null or temp is null or dose<=0 or water<=0
     or temp not between 1 and 100 or abs(water/dose-ratio)>0.05
     or btrim(coalesce(p->>'methodName',p->>'method',''))='' then
     raise exception using errcode='23514',message='Invalid technique quantities'; end if;
   for step in select value from jsonb_array_elements(p->'steps') loop
     idx:=idx+1; sec:=coalesce(step->>'duration_sec',step->>'durationSeconds')::integer;
     added:=coalesce(step->>'water_add_ml',step->>'waterAddedMl')::integer;
     if sec is null or sec<=0 or added is null or added<0 or btrim(coalesce(step->>'title',''))=''
       or coalesce(step->>'step_order',step->>'stepNumber')::integer is distinct from idx then
       raise exception using errcode='23514',message='Invalid technique step'; end if;
     total_water:=total_water+added; total_seconds:=total_seconds+sec;
     if coalesce(step->>'target_water_ml',step->>'waterAccumulatedMl')::integer is distinct from total_water then
       raise exception using errcode='23514',message='Invalid cumulative water'; end if;
   end loop;
   if total_water<>water or total_seconds>86400 or
     (p->>'totalTimeSeconds' is not null and (p->>'totalTimeSeconds')::integer<>total_seconds) then
     raise exception using errcode='23514',message='Technique totals disagree with steps'; end if;
 else raise exception using errcode='23514',message='Unsupported content type'; end if;
end $$;

create function private.register_social_author() returns trigger
language plpgsql security definer set search_path='' as $$
declare actor uuid:=auth.uid(); profile public.profiles; source public.shares; creation public.community_creations;
begin
 if actor is null or actor<>new.from_user_id then raise exception using errcode='42501',message='Authenticated author required'; end if;
 select * into profile from public.profiles where id=actor and deleted_at is null;
 if not found then raise exception using errcode='23514',message='Profile required'; end if;
 if tg_op='UPDATE' then
   if new.from_user_id<>old.from_user_id or new.entity_id<>old.entity_id or new.entity_type<>old.entity_type
     or new.source_share_id is distinct from old.source_share_id then
     raise exception using errcode='23514',message='Publication identity is immutable'; end if;
   new.creation_id:=old.creation_id;
 else
   -- Serializes a per-account limit without storing requests/activity on every local save.
   perform pg_advisory_xact_lock(hashtextextended(actor::text,1));
   if (select count(*) from public.shares where from_user_id=actor and created_at>now()-interval '1 hour')>=30 then
     raise exception using errcode='54000',message='Publication limit reached; try later'; end if;
   new.created_at:=now(); new.updated_at:=now(); new.like_count:=0;
   if new.source_share_id is not null then
     select * into source from public.shares where id=new.source_share_id and private.share_visible(id);
     if not found or source.entity_type<>new.entity_type or source.creation_id is null then
       raise exception using errcode='23514',message='Accessible source publication required to preserve authorship'; end if;
     new.creation_id:=source.creation_id;
   else
     if new.original_author_user_id is not null and new.original_author_user_id<>actor then
       raise exception using errcode='23514',message='Cannot claim another creator without a source publication'; end if;
     insert into public.community_creations(creator_id,creator_name,entity_type,local_entity_id)
       values(actor,profile.display_name,new.entity_type,new.entity_id)
       on conflict(creator_id,entity_type,local_entity_id) do nothing;
     select id into new.creation_id from public.community_creations where creator_id=actor and entity_type=new.entity_type and local_entity_id=new.entity_id;
   end if;
 end if;
 select * into creation from public.community_creations where id=new.creation_id;
 if not found then raise exception using errcode='23514',message='Creator registration missing'; end if;
 new.from_name:=profile.display_name; new.from_handle:=profile.alias;
 new.original_author_user_id:=creation.creator_id; new.original_author_name:=creation.creator_name;
 new.original_entity_id:=creation.local_entity_id;
 perform private.validate_social_snapshot(new.entity_type,new.payload_snapshot_json);
 return new;
end $$;
create trigger a_register_author before insert or update of from_user_id,entity_id,entity_type,source_share_id,name,subtitle,message,payload_snapshot_json,visibility,target_user_id,status,deleted_at
 on public.shares for each row execute function private.register_social_author();
-- Counter-only changes must not re-run profile-privacy checks after an author goes private.
drop trigger shares_moderate_content on public.shares;
create trigger shares_moderate_content before insert or update of from_name,from_handle,name,subtitle,message,payload_snapshot_json,visibility
 on public.shares for each row execute function public.enforce_share_moderation();

create function private.update_like_count() returns trigger language plpgsql security definer set search_path='' as $$
begin
 if tg_op='INSERT' then update public.shares set like_count=like_count+1 where id=new.share_id;
 else update public.shares set like_count=greatest(0,like_count-1) where id=old.share_id; end if;
 return null;
end $$;
create trigger share_like_counter after insert or delete on public.share_likes
 for each row execute function private.update_like_count();
-- An owner can remove their like/save even after a publication becomes hidden.
drop policy like_read on public.share_likes;
create policy like_read on public.share_likes for select to authenticated using(user_id=(select auth.uid()));
drop policy save_read on public.share_saves;
create policy save_read on public.share_saves for select to authenticated using(user_id=(select auth.uid()));
-- Counts are public to readers of the share; the identities of likers stay private.
revoke insert,update on public.shares from authenticated;
grant insert(id,entity_type,entity_id,from_user_id,from_name,from_handle,target_user_id,visibility,name,subtitle,message,
 payload_snapshot_json,original_author_user_id,original_author_name,original_entity_id,source_share_id,created_at,updated_at)
 on public.shares to authenticated;
grant update(name,subtitle,message,payload_snapshot_json,visibility,target_user_id,status,deleted_at) on public.shares to authenticated;
revoke all on function private.validate_social_snapshot(text,jsonb),private.register_social_author(),private.update_like_count() from public,anon,authenticated;
notify pgrst,'reload schema';
commit;
