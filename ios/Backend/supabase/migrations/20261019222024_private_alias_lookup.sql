begin;
-- A narrow lookup needs to bypass profile RLS, but that implementation must not
-- live in the schema exposed by PostgREST. The public API is only an invoker wrapper.
create function private.resolve_profile_alias(alias_input text)
returns table(user_id uuid,alias text) language sql stable security definer set search_path='' as $$
  select p.id,p.alias from public.profiles p where auth.uid() is not null and p.id<>auth.uid()
    and p.deleted_at is null and not private.users_blocked(auth.uid(),p.id)
    and lower(p.alias)=lower(trim(both '@' from btrim(alias_input))) limit 1
$$;
revoke all on function private.resolve_profile_alias(text) from public,anon;
grant execute on function private.resolve_profile_alias(text) to authenticated;
create or replace function public.resolve_profile_alias(alias_input text)
returns table(user_id uuid,alias text) language sql stable security invoker set search_path='' as $$
  select * from private.resolve_profile_alias(alias_input)
$$;
revoke all on function public.resolve_profile_alias(text) from public,anon;
grant execute on function public.resolve_profile_alias(text) to authenticated;
-- Legacy staging table is deliberately sealed; canonical clients use shares.
create policy archived_no_access on public.brew_shares to authenticated using(false) with check(false);
notify pgrst,'reload schema';
commit;
