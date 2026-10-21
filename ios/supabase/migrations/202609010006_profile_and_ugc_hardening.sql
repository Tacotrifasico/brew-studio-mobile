begin;

do $$
begin
  if not exists (select 1 from pg_constraint where conname = 'profiles_display_name_length' and conrelid = 'public.profiles'::regclass) then
    alter table public.profiles add constraint profiles_display_name_length check (char_length(btrim(display_name)) between 1 and 80) not valid;
  end if;
  if not exists (select 1 from pg_constraint where conname = 'profiles_alias_format' and conrelid = 'public.profiles'::regclass) then
    alter table public.profiles add constraint profiles_alias_format check (alias ~ '^[A-Za-z0-9._-]{1,40}$') not valid;
  end if;
  if not exists (select 1 from pg_constraint where conname = 'profiles_biography_length' and conrelid = 'public.profiles'::regclass) then
    alter table public.profiles add constraint profiles_biography_length check (char_length(biography) <= 300) not valid;
  end if;
  if not exists (select 1 from pg_constraint where conname = 'profiles_avatar_color_format' and conrelid = 'public.profiles'::regclass) then
    alter table public.profiles add constraint profiles_avatar_color_format check (avatar_color ~ '^#[0-9A-Fa-f]{6}$') not valid;
  end if;
  if not exists (select 1 from pg_constraint where conname = 'profiles_favorite_methods_length' and conrelid = 'public.profiles'::regclass) then
    alter table public.profiles add constraint profiles_favorite_methods_length check (char_length(favorite_methods) <= 300) not valid;
  end if;
  if not exists (select 1 from pg_constraint where conname = 'content_reports_reason_length' and conrelid = 'public.content_reports'::regclass) then
    alter table public.content_reports add constraint content_reports_reason_length check (char_length(btrim(reason)) between 1 and 500) not valid;
  end if;
end $$;

create or replace function public.enforce_private_profile_sharing()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if new.visibility = 'PUBLIC' and coalesce(
    (select profile.is_private from public.profiles profile where profile.id = new.owner_id and profile.deleted_at is null),
    true
  ) then
    raise exception using errcode = '23514', message = 'private profiles cannot publish public shares';
  end if;
  return new;
end;
$$;

drop trigger if exists brew_shares_enforce_profile_privacy on public.brew_shares;
create trigger brew_shares_enforce_profile_privacy
before insert or update of owner_id, visibility
on public.brew_shares
for each row execute function public.enforce_private_profile_sharing();

commit;
