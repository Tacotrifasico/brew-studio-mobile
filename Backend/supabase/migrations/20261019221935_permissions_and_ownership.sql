-- Production security contract shared by Android and iOS. No service key in clients.
begin;
revoke create on schema public from public, anon, authenticated;
grant usage on schema public to authenticated;
create schema if not exists private;
revoke all on schema private from public, anon;
grant usage on schema private to authenticated;

alter table public.beans add column brew_profiles jsonb not null default '{}'::jsonb
  check (jsonb_typeof(brew_profiles) = 'object' and octet_length(brew_profiles::text) <= 65536);
alter table public.beans add constraint beans_stock_nonnegative check (stock_grams >= 0);
alter table public.shares add constraint shares_visibility_valid check (
  (visibility = 'public' and target_user_id is null) or
  (visibility = 'direct' and target_user_id is not null and target_user_id <> from_user_id));
alter table public.shares add constraint shares_status_valid check (status in ('active','removed'));
alter table public.shares add constraint shares_type_valid check (entity_type in ('recipe','technique'));
alter table public.shares add constraint shares_snapshot_object check (
  jsonb_typeof(payload_snapshot_json) = 'object' and octet_length(payload_snapshot_json::text) <= 524288);
-- Deleting a recipient must not turn a private direct share into an orphan.
alter table public.shares drop constraint shares_target_user_id_fkey;
alter table public.shares add constraint shares_target_user_id_fkey foreign key(target_user_id)
  references auth.users(id) on delete cascade;

-- A client may not assign different identities to legacy and canonical columns.
do $$ declare t text; begin
  foreach t in array array['grinders','equipment','recipes','techniques','technique_steps','lab_experiments'] loop
    execute format('alter table public.%I add constraint %I check (user_id = owner_id)',t,t||'_same_owner');
    execute format('alter table public.%I alter column user_id set not null',t);
  end loop;
end $$;

-- Runs as the caller. Parent rows hidden by RLS are not usable foreign references.
-- Only internal FKs to owned entities are checked; author/provenance IDs are not ownership.
create function private.assert_owned_links() returns trigger language plpgsql
set search_path = '' as $$
declare link record; ref uuid; parent_owner uuid; row_owner uuid;
begin
  row_owner := (to_jsonb(new)->>'owner_id')::uuid;
  for link in
    select a.attname as col, c.confrelid::regclass as parent
    from pg_catalog.pg_constraint c
    join pg_catalog.pg_attribute a on a.attrelid=c.conrelid and a.attnum=c.conkey[1]
    join pg_catalog.pg_attribute p on p.attrelid=c.confrelid and p.attname='owner_id' and not p.attisdropped
    where c.conrelid=tg_relid and c.contype='f' and cardinality(c.conkey)=1
  loop
    ref := (to_jsonb(new)->>link.col)::uuid;
    if ref is not null then
      parent_owner := null;
      execute format('select owner_id from %s where id=$1',link.parent) into parent_owner using ref;
      if parent_owner is distinct from row_owner then
        raise exception using errcode='23503',message='Related item must belong to the same account';
      end if;
    end if;
  end loop;
  return new;
end $$;
do $$ declare t text; begin
  foreach t in array array['recipes','recipe_ingredients','recipe_steps','techniques','technique_steps','brew_sessions','tastings','tasting_observations','cup_sessions','lab_experiments'] loop
    execute format('create trigger z_assert_owned_links before insert or update on public.%I for each row execute function private.assert_owned_links()',t);
  end loop;
end $$;

-- Defense against a bean UUID collision hijacking another user's iOS FK mirror.
create function private.guard_bean_mirror() returns trigger language plpgsql security definer
set search_path = '' as $$
begin
  if exists(select 1 from public.coffee_beans b where b.id=new.id and b.owner_id<>new.user_id) then
    raise exception using errcode='23505',message='Bean identifier already belongs to another account';
  end if;
  return new;
end $$;
create trigger beans_guard_mirror before insert or update on public.beans
for each row execute function private.guard_bean_mirror();

-- Deliberately private helpers: RLS must see blocks from EITHER party and public
-- author privacy without exposing the other user's profile or block list.
create function private.users_blocked(a uuid,b uuid) returns boolean language sql stable security definer
set search_path = '' as $$
  select exists(select 1 from public.blocked_users where
    (blocker_id=a and blocked_user_id=b) or (blocker_id=b and blocked_user_id=a))
$$;
create function private.share_visible(share uuid) returns boolean language sql stable security definer
set search_path = '' as $$
  select exists(select 1 from public.shares s where s.id=share and auth.uid() is not null and
    (s.from_user_id=auth.uid() or (
      s.deleted_at is null and s.status='active' and not private.users_blocked(auth.uid(),s.from_user_id) and
      ((s.visibility='direct' and s.target_user_id=auth.uid()) or
       (s.visibility='public' and exists(select 1 from public.profiles p where p.id=s.from_user_id and not p.is_private and p.deleted_at is null))))))
$$;

-- Rebuild the fresh-project policies, removing permissive legacy OR policies.
do $$ declare p record; t text; begin
  for p in select tablename,policyname from pg_policies where schemaname='public' loop
    execute format('drop policy %I on public.%I',p.policyname,p.tablename);
  end loop;
  foreach t in array array['coffee_beans','grinders','equipment','recipes','recipe_ingredients','recipe_steps','techniques','technique_steps','brew_sessions','tastings','tasting_observations','cup_sessions','lab_experiments'] loop
    execute format('create policy owner_all on public.%I to authenticated using (owner_id=(select auth.uid())) with check (owner_id=(select auth.uid()))',t);
  end loop;
end $$;
create policy owner_all on public.beans to authenticated using (user_id=(select auth.uid())) with check (user_id=(select auth.uid()));
create policy owner_all on public.profiles to authenticated using (id=(select auth.uid())) with check (id=(select auth.uid()));
create policy owner_all on public.blocked_users to authenticated using (blocker_id=(select auth.uid())) with check (blocker_id=(select auth.uid()));
create policy share_read on public.shares for select to authenticated using (private.share_visible(id));
create policy share_insert on public.shares for insert to authenticated with check (
  from_user_id=(select auth.uid()) and (target_user_id is null or not private.users_blocked(from_user_id,target_user_id)));
create policy share_update on public.shares for update to authenticated using (from_user_id=(select auth.uid())) with check (
  from_user_id=(select auth.uid()) and (target_user_id is null or not private.users_blocked(from_user_id,target_user_id)));
create policy share_delete on public.shares for delete to authenticated using (from_user_id=(select auth.uid()));
create policy like_read on public.share_likes for select to authenticated using (user_id=(select auth.uid()) and private.share_visible(share_id));
create policy like_insert on public.share_likes for insert to authenticated with check (user_id=(select auth.uid()) and private.share_visible(share_id));
create policy like_delete on public.share_likes for delete to authenticated using (user_id=(select auth.uid()));
create policy save_read on public.share_saves for select to authenticated using (user_id=(select auth.uid()) and private.share_visible(share_id));
create policy save_insert on public.share_saves for insert to authenticated with check (user_id=(select auth.uid()) and private.share_visible(share_id));
create policy save_delete on public.share_saves for delete to authenticated using (user_id=(select auth.uid()));
create policy report_read on public.content_reports for select to authenticated using (reporter_id=(select auth.uid()));
create policy report_insert on public.content_reports for insert to authenticated with check (reporter_id=(select auth.uid()) and status='OPEN' and private.share_visible(share_id));
create policy inbox_read on public.inbox_items for select to authenticated using (target_user_id=(select auth.uid()) and private.share_visible(share_id));
create policy inbox_update on public.inbox_items for update to authenticated using (target_user_id=(select auth.uid()) and private.share_visible(share_id)) with check (target_user_id=(select auth.uid()) and private.share_visible(share_id));
create policy activity_read on public.activity_log for select to authenticated using (user_id=(select auth.uid()));
create policy activity_insert on public.activity_log for insert to authenticated with check (user_id=(select auth.uid()) and (share_id is null or private.share_visible(share_id)));

-- Profile provisioning uses metadata for presentation ONLY, never permissions.
-- Do not reject an Auth signup because an optional alias is invalid/already used.
create function private.create_profile() returns trigger language plpgsql security definer
set search_path = '' as $$
declare chosen_alias text; chosen_name text;
begin
  chosen_alias := lower(coalesce(new.raw_user_meta_data->>'alias',new.raw_user_meta_data->>'handle',''));
  if chosen_alias !~ '^[a-z0-9._-]{1,40}$' then chosen_alias := 'barista_'||replace(new.id::text,'-',''); end if;
  chosen_name := left(coalesce(nullif(btrim(new.raw_user_meta_data->>'display_name'),''),'Barista'),80);
  begin
    insert into public.profiles(id,display_name,alias,handle,is_private) values(new.id,chosen_name,chosen_alias,chosen_alias,true);
  exception when unique_violation then
    chosen_alias := 'barista_'||replace(new.id::text,'-','');
    insert into public.profiles(id,display_name,alias,handle,is_private) values(new.id,chosen_name,chosen_alias,chosen_alias,true);
  end;
  return new;
end $$;
create trigger cupa_create_profile after insert on auth.users for each row execute function private.create_profile();

-- Exact-match alias lookup, never a public account directory; respect blocks.
create or replace function public.resolve_profile_alias(alias_input text)
returns table(user_id uuid,alias text) language sql stable security definer set search_path='' as $$
  select p.id,p.alias from public.profiles p where auth.uid() is not null and p.id<>auth.uid()
    and p.deleted_at is null and not private.users_blocked(auth.uid(),p.id)
    and lower(p.alias)=lower(trim(both '@' from btrim(alias_input))) limit 1
$$;

-- Explicit API privileges: no anonymous reads, no client admin/moderation rights.
revoke all on all tables in schema public from public,anon,authenticated;
grant select,insert,update,delete on public.beans,public.grinders,public.equipment,
  public.recipes,public.recipe_ingredients,public.recipe_steps,public.techniques,public.technique_steps,
  public.brew_sessions,public.tastings,public.tasting_observations,public.cup_sessions,
  public.lab_experiments,public.blocked_users,public.shares to authenticated;
grant select on public.coffee_beans to authenticated;
grant select,insert,update on public.profiles to authenticated;
grant select,insert,delete on public.share_likes,public.share_saves to authenticated;
grant select on public.content_reports to authenticated;
grant insert (id,reporter_id,share_id,reason) on public.content_reports to authenticated;
grant select on public.inbox_items to authenticated;
grant update(read_at) on public.inbox_items to authenticated;
grant select,insert on public.activity_log to authenticated;
revoke all on all functions in schema public from public,anon,authenticated;
revoke all on all functions in schema private from public,anon,authenticated;
grant execute on function public.resolve_profile_alias(text) to authenticated;
-- Nested invoker trigger functions require just these deterministic validators.
grant execute on function public.contains_objectionable_content(text) to authenticated;
grant execute on function private.users_blocked(uuid,uuid),private.share_visible(uuid) to authenticated;

-- Support reverse block lookup, FK checks, sync and parent-child pulls.
create index blocked_users_reverse_idx on public.blocked_users(blocked_user_id,blocker_id);
create index shares_feed_idx on public.shares(visibility,status,created_at desc) where deleted_at is null;
create index shares_target_idx on public.shares(target_user_id,created_at desc);
create index beans_owner_updated_idx on public.beans(user_id,updated_at);
do $$ declare c record; col text; begin
  for c in select conrelid::regclass as tbl, a.attname as col, conrelid
    from pg_constraint k join pg_attribute a on a.attrelid=k.conrelid and a.attnum=k.conkey[1]
    where k.contype='f' and k.connamespace='public'::regnamespace and cardinality(k.conkey)=1
  loop
    if not exists(select 1 from pg_index i where i.indrelid=c.conrelid and i.indkey[0]=(select attnum from pg_attribute where attrelid=c.conrelid and attname=c.col)) then
      execute format('create index %I on %s (%I)', 'fk_'||c.conrelid::text||'_'||c.col,c.tbl,c.col);
    end if;
  end loop;
end $$;
notify pgrst,'reload schema';
commit;
