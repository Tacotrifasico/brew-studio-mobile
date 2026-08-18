begin;

create or replace function public.contains_objectionable_content(value text)
returns boolean
language sql
immutable
set search_path = ''
as $$
  select lower(translate(coalesce(value, ''),
    'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN')) ~
    '(pornografia|pornography|violacion|rape|nazi|terrorista|terrorist|matarte|kill yourself|suicidate|suicide|odio racial|racial hate)';
$$;

create or replace function public.enforce_brew_share_moderation()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if btrim(new.from_name) = '' or btrim(new.from_handle) = '' or btrim(new.name) = '' then
    raise exception using errcode = '23514', message = 'social content requires profile identity and a title';
  end if;

  if char_length(new.from_name) > 80 or char_length(new.from_handle) > 40 or
     char_length(new.name) > 160 or char_length(new.subtitle) > 300 or char_length(new.message) > 1000 then
    raise exception using errcode = '22001', message = 'social content exceeds allowed length';
  end if;

  if public.contains_objectionable_content(
    concat_ws(' ', new.from_name, new.from_handle, new.name, new.subtitle, new.message, new.payload_snapshot::text)
  ) then
    raise exception using errcode = '23514', message = 'social content violates community policy';
  end if;

  return new;
end;
$$;

drop trigger if exists brew_shares_moderate_content on public.brew_shares;
create trigger brew_shares_moderate_content
before insert or update of from_name, from_handle, name, subtitle, message, payload_snapshot
on public.brew_shares
for each row execute function public.enforce_brew_share_moderation();

commit;
