begin;

create unique index if not exists profiles_alias_unique_active
on public.profiles (lower(alias))
where deleted_at is null and btrim(alias) <> '';

create or replace function public.resolve_profile_alias(alias_input text)
returns table(user_id uuid, alias text)
language sql
stable
security definer
set search_path = ''
as $$
  select profile.id, profile.alias
  from public.profiles as profile
  where auth.uid() is not null
    and profile.id <> auth.uid()
    and profile.deleted_at is null
    and lower(profile.alias) = lower(trim(both '@' from btrim(alias_input)))
  limit 1
$$;

revoke all on function public.resolve_profile_alias(text) from public;
revoke all on function public.resolve_profile_alias(text) from anon;
grant execute on function public.resolve_profile_alias(text) to authenticated;

commit;
