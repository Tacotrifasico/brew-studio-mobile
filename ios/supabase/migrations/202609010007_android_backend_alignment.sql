-- Alinea el contrato remoto iOS con las tablas y columnas oficiales de Android.
-- Es idempotente y conserva las columnas legadas para que ambas aplicaciones convivan.
begin;

create table if not exists public.beans (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  roaster text, name text not null, origin text, altitude text, process text,
  roast_date text, first_use_date text, notes text,
  status text not null default 'cerrado', stock_grams numeric not null default 250,
  created_at timestamptz not null default now(), updated_at timestamptz not null default now()
);

alter table public.beans
  add column if not exists producer text not null default '',
  add column if not exists variety text not null default '',
  add column if not exists altitude_meters integer,
  add column if not exists roast_level text not null default 'Medio',
  add column if not exists initial_quantity_grams numeric,
  add column if not exists version bigint not null default 1,
  add column if not exists deleted_at timestamptz;

update public.beans
set altitude_meters = case when coalesce(altitude, '') ~ '^[0-9]+$' then altitude::integer else altitude_meters end,
    initial_quantity_grams = coalesce(initial_quantity_grams, stock_grams),
    first_use_date = nullif(first_use_date, '')
where altitude_meters is null or initial_quantity_grams is null or first_use_date = '';

-- Recupera inventario creado por versiones iOS anteriores; si el UUID ya existe, Android conserva precedencia.
insert into public.beans (
  id, user_id, roaster, name, origin, altitude, process, roast_date, first_use_date, notes,
  status, stock_grams, created_at, updated_at, producer, variety, altitude_meters,
  roast_level, initial_quantity_grams, version, deleted_at
)
select
  id, owner_id, brand, name, origin, coalesce(altitude_meters::text, ''), process,
  coalesce(to_char(roast_date, 'YYYY-MM-DD'), ''), coalesce(to_char(opened_date, 'YYYY-MM-DD'), ''), notes,
  case when remaining_quantity_grams <= 0 then 'terminado' when opened_date is null then 'cerrado' else 'abierto' end,
  remaining_quantity_grams, created_at, updated_at, producer, variety, altitude_meters,
  roast_level, initial_quantity_grams, version, deleted_at
from public.coffee_beans
on conflict (id) do nothing;

alter table public.profiles add column if not exists handle text;
alter table public.grinders
  add column if not exists user_id uuid references auth.users(id) on delete cascade,
  add column if not exists click_range text,
  add column if not exists favorite_clicks_by_method text;
alter table public.equipment
  add column if not exists user_id uuid references auth.users(id) on delete cascade,
  add column if not exists type text;
alter table public.recipes
  add column if not exists user_id uuid references auth.users(id) on delete cascade,
  add column if not exists owner_user_id uuid,
  add column if not exists owner_display_name text,
  add column if not exists method text,
  add column if not exists notes text,
  add column if not exists is_shared boolean not null default false,
  add column if not exists original_author_user_id uuid,
  add column if not exists original_author_name text,
  add column if not exists imported_from_share_id uuid;
alter table public.techniques
  add column if not exists user_id uuid references auth.users(id) on delete cascade,
  add column if not exists owner_user_id uuid,
  add column if not exists owner_display_name text,
  add column if not exists method text,
  add column if not exists coffee_grams numeric,
  add column if not exists temperature integer,
  add column if not exists grind_clicks text,
  add column if not exists is_shared boolean not null default false,
  add column if not exists original_author_user_id uuid,
  add column if not exists original_author_name text,
  add column if not exists imported_from_share_id uuid;
alter table public.technique_steps
  add column if not exists user_id uuid references auth.users(id) on delete cascade,
  add column if not exists step_order integer,
  add column if not exists duration_sec integer,
  add column if not exists water_add_ml integer,
  add column if not exists target_water_ml integer,
  add column if not exists note text;
alter table public.lab_experiments
  add column if not exists user_id uuid references auth.users(id) on delete cascade,
  add column if not exists method_name text,
  add column if not exists temperature integer,
  add column if not exists clicks integer,
  add column if not exists bean_freshness text,
  add column if not exists estimated_time_seconds integer,
  add column if not exists experiment_notes text,
  add column if not exists timestamp bigint;

create or replace function public.align_android_bean()
returns trigger language plpgsql set search_path = public, pg_temp as $$
begin
  new.user_id := coalesce(new.user_id, auth.uid());
  new.roaster := coalesce(new.roaster, '');
  new.stock_grams := coalesce(new.stock_grams, new.initial_quantity_grams, 0);
  new.initial_quantity_grams := greatest(coalesce(new.initial_quantity_grams, new.stock_grams), new.stock_grams);
  new.altitude := coalesce(nullif(new.altitude, ''), new.altitude_meters::text, '');
  new.altitude_meters := coalesce(new.altitude_meters,
    case when coalesce(new.altitude, '') ~ '^[0-9]+$' then new.altitude::integer else null end);
  new.status := case
    when new.stock_grams <= 0 then 'terminado'
    when nullif(new.first_use_date, '') is null then 'cerrado'
    else 'abierto'
  end;
  new.updated_at := coalesce(new.updated_at, now());
  return new;
end;
$$;
drop trigger if exists beans_align_clients on public.beans;
create trigger beans_align_clients before insert or update on public.beans
for each row execute function public.align_android_bean();
drop trigger if exists beans_set_updated_at on public.beans;
create trigger beans_set_updated_at before update on public.beans
for each row execute function public.set_updated_at();

create or replace function public.mirror_bean_for_ios_relations()
returns trigger language plpgsql security definer set search_path = public, pg_temp as $$
begin
  insert into public.coffee_beans (
    id, owner_id, name, brand, origin, producer, variety, process, altitude_meters,
    roast_level, roast_date, opened_date, initial_quantity_grams, remaining_quantity_grams,
    notes, created_at, updated_at, version, deleted_at
  ) values (
    new.id, new.user_id, new.name, coalesce(new.roaster, ''), coalesce(new.origin, ''),
    new.producer, new.variety, coalesce(new.process, ''), new.altitude_meters,
    new.roast_level,
    case when coalesce(new.roast_date, '') ~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}$' then new.roast_date::date else null end,
    case when coalesce(new.first_use_date, '') ~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}$' then new.first_use_date::date else null end,
    coalesce(new.initial_quantity_grams, new.stock_grams), new.stock_grams, coalesce(new.notes, ''),
    new.created_at, new.updated_at, new.version, new.deleted_at
  )
  on conflict (id) do update set
    owner_id = excluded.owner_id, name = excluded.name, brand = excluded.brand,
    origin = excluded.origin, producer = excluded.producer, variety = excluded.variety,
    process = excluded.process, altitude_meters = excluded.altitude_meters,
    roast_level = excluded.roast_level, roast_date = excluded.roast_date,
    opened_date = excluded.opened_date, initial_quantity_grams = excluded.initial_quantity_grams,
    remaining_quantity_grams = excluded.remaining_quantity_grams, notes = excluded.notes,
    updated_at = excluded.updated_at, version = excluded.version, deleted_at = excluded.deleted_at;
  return new;
end;
$$;
drop trigger if exists beans_mirror_ios_relations on public.beans;
create trigger beans_mirror_ios_relations after insert or update on public.beans
for each row execute function public.mirror_bean_for_ios_relations();
update public.beans set id = id;

alter table public.grinders alter column owner_id set default auth.uid();
alter table public.equipment alter column owner_id set default auth.uid();
alter table public.recipes alter column owner_id set default auth.uid();
alter table public.techniques alter column owner_id set default auth.uid();
alter table public.technique_steps alter column owner_id set default auth.uid();
alter table public.lab_experiments alter column owner_id set default auth.uid();

create or replace function public.align_grinder_clients()
returns trigger language plpgsql set search_path = public, pg_temp as $$
declare parsed_range text[];
begin
  new.user_id := coalesce(new.user_id, new.owner_id, auth.uid());
  new.owner_id := coalesce(new.owner_id, new.user_id);
  if tg_op = 'UPDATE' and (new.brand is distinct from old.brand or new.model is distinct from old.model) and new.name is not distinct from old.name then
    new.name := btrim(concat_ws(' ', new.brand, new.model));
  else
    new.name := coalesce(nullif(new.name, ''), btrim(concat_ws(' ', new.brand, new.model)));
  end if;
  if tg_op = 'INSERT' and nullif(new.click_range, '') is not null then
    parsed_range := regexp_match(new.click_range, '^\s*(-?[0-9]+)\s*-\s*(-?[0-9]+)\s*$');
    if parsed_range is not null then
      new.minimum_setting := parsed_range[1]::integer; new.maximum_setting := parsed_range[2]::integer;
    end if;
  elsif tg_op = 'UPDATE' and new.click_range is distinct from old.click_range
     and new.minimum_setting is not distinct from old.minimum_setting and new.maximum_setting is not distinct from old.maximum_setting then
    parsed_range := regexp_match(coalesce(new.click_range, ''), '^\s*(-?[0-9]+)\s*-\s*(-?[0-9]+)\s*$');
    if parsed_range is not null then
      new.minimum_setting := parsed_range[1]::integer; new.maximum_setting := parsed_range[2]::integer;
    end if;
  else
    new.click_range := new.minimum_setting::text || ' - ' || new.maximum_setting::text;
  end if;
  return new;
end;
$$;
drop trigger if exists grinders_align_clients on public.grinders;
create trigger grinders_align_clients before insert or update on public.grinders for each row execute function public.align_grinder_clients();
update public.grinders set id = id;

create or replace function public.align_equipment_clients()
returns trigger language plpgsql set search_path = public, pg_temp as $$
begin
  new.user_id := coalesce(new.user_id, new.owner_id, auth.uid());
  new.owner_id := coalesce(new.owner_id, new.user_id);
  if (tg_op = 'INSERT' and nullif(new.type, '') is not null) or
     (tg_op = 'UPDATE' and new.type is distinct from old.type and new.equipment_type is not distinct from old.equipment_type) then
    new.equipment_type := case lower(coalesce(new.type, 'otro'))
      when 'molino' then 'GRINDER' when 'método' then 'BREWER_METHOD' when 'báscula' then 'SCALE'
      when 'tetera' then 'KETTLE' when 'filtros' then 'FILTERS' when 'servidor' then 'SERVER'
      when 'prensa' then 'PRESS' when 'accesorios' then 'ACCESSORY' else 'OTHER' end;
  else
    new.equipment_type := coalesce(nullif(new.equipment_type, ''), 'OTHER');
  end if;
  new.type := case upper(new.equipment_type)
    when 'GRINDER' then 'molino' when 'BREWER_METHOD' then 'método' when 'SCALE' then 'báscula'
    when 'KETTLE' then 'tetera' when 'FILTERS' then 'filtros' when 'SERVER' then 'servidor'
    when 'PRESS' then 'prensa' when 'ACCESSORY' then 'accesorios' else 'otro' end;
  return new;
end;
$$;
drop trigger if exists equipment_align_clients on public.equipment;
create trigger equipment_align_clients before insert or update on public.equipment for each row execute function public.align_equipment_clients();
update public.equipment set equipment_type = case lower(coalesce(type, 'otro'))
  when 'molino' then 'GRINDER' when 'método' then 'BREWER_METHOD' when 'báscula' then 'SCALE'
  when 'tetera' then 'KETTLE' when 'filtros' then 'FILTERS' when 'servidor' then 'SERVER'
  when 'prensa' then 'PRESS' when 'accesorios' then 'ACCESSORY' else equipment_type end;

create or replace function public.align_recipe_clients()
returns trigger language plpgsql set search_path = public, pg_temp as $$
begin
  new.user_id := coalesce(new.user_id, new.owner_id, auth.uid());
  new.owner_id := coalesce(new.owner_id, new.user_id);
  new.owner_user_id := coalesce(new.owner_user_id, new.user_id);
  if tg_op = 'INSERT' then
    new.suggested_method_name := coalesce(nullif(new.suggested_method_name, ''), new.method, '');
    new.method := coalesce(new.method, nullif(new.suggested_method_name, ''));
  elsif new.method is distinct from old.method and new.suggested_method_name is not distinct from old.suggested_method_name then
    new.suggested_method_name := coalesce(new.method, '');
  else
    new.method := nullif(new.suggested_method_name, '');
  end if;
  if tg_op = 'INSERT' then
    new.intention := coalesce(nullif(new.intention, ''), new.notes, ''); new.notes := coalesce(new.notes, nullif(new.intention, ''));
  elsif new.notes is distinct from old.notes and new.intention is not distinct from old.intention then
    new.intention := coalesce(new.notes, '');
  else
    new.notes := nullif(new.intention, '');
  end if;
  new.visibility := lower(coalesce(new.visibility, 'private'));
  new.copy_mode := lower(coalesce(new.copy_mode, 'original'));
  return new;
end;
$$;
drop trigger if exists recipes_align_clients on public.recipes;
create trigger recipes_align_clients before insert or update on public.recipes for each row execute function public.align_recipe_clients();
update public.recipes set id = id;

create or replace function public.align_technique_clients()
returns trigger language plpgsql set search_path = public, pg_temp as $$
begin
  new.user_id := coalesce(new.user_id, new.owner_id, auth.uid());
  new.owner_id := coalesce(new.owner_id, new.user_id);
  new.owner_user_id := coalesce(new.owner_user_id, new.user_id);
  if tg_op = 'INSERT' then new.method_name := coalesce(nullif(new.method_name, ''), new.method, ''); new.method := coalesce(new.method, nullif(new.method_name, ''));
  elsif new.method is distinct from old.method and new.method_name is not distinct from old.method_name then new.method_name := coalesce(new.method, '');
  else new.method := nullif(new.method_name, ''); end if;
  if tg_op = 'INSERT' then new.dose_grams := coalesce(new.coffee_grams, new.dose_grams, 15); new.coffee_grams := new.dose_grams;
  elsif new.coffee_grams is distinct from old.coffee_grams and new.dose_grams is not distinct from old.dose_grams then new.dose_grams := coalesce(new.coffee_grams, 15);
  else new.coffee_grams := new.dose_grams; end if;
  if tg_op = 'INSERT' then new.temperature_c := coalesce(new.temperature, new.temperature_c, 93); new.temperature := new.temperature_c;
  elsif new.temperature is distinct from old.temperature and new.temperature_c is not distinct from old.temperature_c then new.temperature_c := coalesce(new.temperature, 93);
  else new.temperature := new.temperature_c; end if;
  if tg_op = 'INSERT' then new.grind_description := coalesce(nullif(new.grind_description, ''), new.grind_clicks, ''); new.grind_clicks := nullif(new.grind_description, '');
  elsif new.grind_clicks is distinct from old.grind_clicks and new.grind_description is not distinct from old.grind_description then new.grind_description := coalesce(new.grind_clicks, '');
  else new.grind_clicks := nullif(new.grind_description, ''); end if;
  new.visibility := lower(coalesce(new.visibility, 'private'));
  new.copy_mode := lower(coalesce(new.copy_mode, 'original'));
  return new;
end;
$$;
drop trigger if exists techniques_align_clients on public.techniques;
create trigger techniques_align_clients before insert or update on public.techniques for each row execute function public.align_technique_clients();
update public.techniques set
  method_name = coalesce(nullif(method_name, ''), method, ''),
  dose_grams = coalesce(coffee_grams, dose_grams),
  temperature_c = coalesce(temperature, temperature_c),
  grind_description = coalesce(nullif(grind_description, ''), grind_clicks, '');

create or replace function public.align_technique_step_clients()
returns trigger language plpgsql set search_path = public, pg_temp as $$
begin
  new.user_id := coalesce(new.user_id, new.owner_id, auth.uid());
  new.owner_id := coalesce(new.owner_id, new.user_id);
  if tg_op = 'INSERT' then new.step_number := coalesce(new.step_number, new.step_order); new.step_order := new.step_number;
  elsif new.step_order is distinct from old.step_order and new.step_number is not distinct from old.step_number then new.step_number := new.step_order;
  else new.step_order := new.step_number; end if;
  if tg_op = 'INSERT' then new.duration_seconds := coalesce(new.duration_sec, new.duration_seconds, 0); new.duration_sec := new.duration_seconds;
  elsif new.duration_sec is distinct from old.duration_sec and new.duration_seconds is not distinct from old.duration_seconds then new.duration_seconds := coalesce(new.duration_sec, 0);
  else new.duration_sec := new.duration_seconds; end if;
  if tg_op = 'INSERT' then new.water_added_ml := coalesce(new.water_add_ml, new.water_added_ml, 0); new.water_add_ml := new.water_added_ml;
  elsif new.water_add_ml is distinct from old.water_add_ml and new.water_added_ml is not distinct from old.water_added_ml then new.water_added_ml := coalesce(new.water_add_ml, 0);
  else new.water_add_ml := new.water_added_ml; end if;
  if tg_op = 'INSERT' then new.water_accumulated_ml := coalesce(new.target_water_ml, new.water_accumulated_ml, 0); new.target_water_ml := new.water_accumulated_ml;
  elsif new.target_water_ml is distinct from old.target_water_ml and new.water_accumulated_ml is not distinct from old.water_accumulated_ml then new.water_accumulated_ml := coalesce(new.target_water_ml, 0);
  else new.target_water_ml := new.water_accumulated_ml; end if;
  if tg_op = 'INSERT' then new.step_note := coalesce(nullif(new.step_note, ''), new.note, ''); new.note := nullif(new.step_note, '');
  elsif new.note is distinct from old.note and new.step_note is not distinct from old.step_note then new.step_note := coalesce(new.note, '');
  else new.note := nullif(new.step_note, ''); end if;
  return new;
end;
$$;
drop trigger if exists technique_steps_align_clients on public.technique_steps;
create trigger technique_steps_align_clients before insert or update on public.technique_steps for each row execute function public.align_technique_step_clients();
update public.technique_steps set
  step_number = coalesce(step_order, step_number), duration_seconds = coalesce(duration_sec, duration_seconds),
  water_added_ml = coalesce(water_add_ml, water_added_ml), water_accumulated_ml = coalesce(target_water_ml, water_accumulated_ml),
  step_note = coalesce(note, step_note);

create or replace function public.align_lab_clients()
returns trigger language plpgsql set search_path = public, pg_temp as $$
begin
  new.user_id := coalesce(new.user_id, new.owner_id, auth.uid());
  new.owner_id := coalesce(new.owner_id, new.user_id);
  if tg_op = 'INSERT' then new.method := coalesce(nullif(new.method, ''), new.method_name, ''); new.method_name := coalesce(nullif(new.method_name, ''), new.method, 'Laboratorio');
  elsif new.method_name is distinct from old.method_name and new.method is not distinct from old.method then new.method := coalesce(new.method_name, '');
  else new.method_name := coalesce(nullif(new.method, ''), 'Laboratorio'); end if;
  if tg_op = 'INSERT' then new.temperature_c := coalesce(new.temperature, new.temperature_c, 93); new.temperature := new.temperature_c;
  elsif new.temperature is distinct from old.temperature and new.temperature_c is not distinct from old.temperature_c then new.temperature_c := coalesce(new.temperature, 93);
  else new.temperature := new.temperature_c; end if;
  if tg_op = 'INSERT' then new.grind_clicks := coalesce(new.clicks, new.grind_clicks, 0); new.clicks := new.grind_clicks;
  elsif new.clicks is distinct from old.clicks and new.grind_clicks is not distinct from old.grind_clicks then new.grind_clicks := coalesce(new.clicks, 0);
  else new.clicks := new.grind_clicks; end if;
  if tg_op = 'INSERT' then new.freshness := coalesce(nullif(new.freshness, ''), new.bean_freshness, 'sin dato'); new.bean_freshness := nullif(new.freshness, '');
  elsif new.bean_freshness is distinct from old.bean_freshness and new.freshness is not distinct from old.freshness then new.freshness := coalesce(new.bean_freshness, 'sin dato');
  else new.bean_freshness := nullif(new.freshness, ''); end if;
  if tg_op = 'INSERT' then new.time_seconds := coalesce(new.estimated_time_seconds, new.time_seconds, 0); new.estimated_time_seconds := new.time_seconds;
  elsif new.estimated_time_seconds is distinct from old.estimated_time_seconds and new.time_seconds is not distinct from old.time_seconds then new.time_seconds := coalesce(new.estimated_time_seconds, 0);
  else new.estimated_time_seconds := new.time_seconds; end if;
  if tg_op = 'INSERT' then new.notes := coalesce(nullif(new.notes, ''), new.experiment_notes, ''); new.experiment_notes := nullif(new.notes, '');
  elsif new.experiment_notes is distinct from old.experiment_notes and new.notes is not distinct from old.notes then new.notes := coalesce(new.experiment_notes, '');
  else new.experiment_notes := nullif(new.notes, ''); end if;
  new.timestamp := coalesce(new.timestamp, (extract(epoch from now()) * 1000)::bigint);
  return new;
end;
$$;
drop trigger if exists lab_experiments_align_clients on public.lab_experiments;
create trigger lab_experiments_align_clients before insert or update on public.lab_experiments for each row execute function public.align_lab_clients();
update public.lab_experiments set
  method = coalesce(nullif(method, ''), method_name, ''), temperature_c = coalesce(temperature, temperature_c),
  grind_clicks = coalesce(clicks, grind_clicks), freshness = coalesce(nullif(freshness, ''), bean_freshness, 'sin dato'),
  time_seconds = coalesce(estimated_time_seconds, time_seconds), notes = coalesce(experiment_notes, notes);

do $$ begin
  begin alter table public.recipes add constraint recipes_suggested_method_id_fkey foreign key (suggested_method_id) references public.equipment(id) on delete set null; exception when duplicate_object then null; end;
  begin alter table public.techniques add constraint techniques_method_id_fkey foreign key (method_id) references public.equipment(id) on delete set null; exception when duplicate_object then null; end;
  begin alter table public.techniques add constraint techniques_recipe_id_fkey foreign key (recipe_id) references public.recipes(id) on delete set null; exception when duplicate_object then null; end;
end $$;

update public.profiles set alias = coalesce(nullif(alias, ''), nullif(handle, ''), 'barista_' || left(id::text, 6)) where alias = '';
create or replace function public.align_profile_clients()
returns trigger language plpgsql set search_path = public, pg_temp as $$
begin
  if tg_op = 'UPDATE' and new.handle is distinct from old.handle and new.alias is not distinct from old.alias then new.alias := coalesce(nullif(new.handle, ''), 'barista_' || left(new.id::text, 6));
  else new.handle := coalesce(nullif(new.alias, ''), nullif(new.handle, '')); end if;
  new.alias := coalesce(nullif(new.alias, ''), new.handle, 'barista_' || left(new.id::text, 6));
  return new;
end;
$$;
drop trigger if exists profiles_align_clients on public.profiles;
create trigger profiles_align_clients before insert or update on public.profiles for each row execute function public.align_profile_clients();

create table if not exists public.shares (
  id uuid primary key default gen_random_uuid(), entity_type text not null, entity_id uuid not null,
  from_user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  from_name text not null, from_handle text, target_user_id uuid references auth.users(id) on delete set null,
  visibility text not null default 'public', name text not null, subtitle text, message text,
  payload_snapshot_json jsonb not null, original_author_user_id uuid, original_author_name text,
  original_entity_id uuid, created_at timestamptz not null default now(), updated_at timestamptz not null default now()
);
alter table public.shares
  add column if not exists status text not null default 'active',
  add column if not exists version bigint not null default 1,
  add column if not exists deleted_at timestamptz;
update public.shares set from_handle = coalesce(from_handle, ''), subtitle = coalesce(subtitle, ''), message = coalesce(message, '');
alter table public.shares alter column from_handle set default '';
alter table public.shares alter column from_handle set not null;
alter table public.shares alter column subtitle set default '';
alter table public.shares alter column subtitle set not null;
alter table public.shares alter column message set default '';
alter table public.shares alter column message set not null;
drop trigger if exists shares_set_updated_at on public.shares;
create trigger shares_set_updated_at before update on public.shares
for each row execute function public.set_updated_at();

insert into public.shares (id, entity_type, entity_id, from_user_id, from_name, from_handle, target_user_id, visibility, name, subtitle, message, payload_snapshot_json, original_entity_id, status, created_at, updated_at, version, deleted_at)
select id, entity_type, entity_id, owner_id, from_name, from_handle, target_user_id,
       case visibility when 'DIRECT' then 'direct' else 'public' end,
       name, subtitle, message, payload_snapshot, original_entity_id,
       case status when 'REMOVED' then 'removed' else 'active' end,
       created_at, updated_at, version, deleted_at
from public.brew_shares
on conflict (id) do nothing;

do $$
declare item record;
begin
  for item in
    select conrelid::regclass as table_name, conname
    from pg_constraint
    where contype = 'f' and confrelid = 'public.brew_shares'::regclass
      and conrelid in ('public.share_likes'::regclass, 'public.share_saves'::regclass, 'public.content_reports'::regclass, 'public.inbox_items'::regclass, 'public.activity_log'::regclass)
  loop
    execute format('alter table %s drop constraint %I', item.table_name, item.conname);
  end loop;
end $$;

do $$ begin
  begin alter table public.share_likes add constraint share_likes_share_id_fkey foreign key (share_id) references public.shares(id) on delete cascade; exception when duplicate_object then null; end;
  begin alter table public.share_saves add constraint share_saves_share_id_fkey foreign key (share_id) references public.shares(id) on delete cascade; exception when duplicate_object then null; end;
  begin alter table public.content_reports add constraint content_reports_share_id_fkey foreign key (share_id) references public.shares(id) on delete cascade; exception when duplicate_object then null; end;
  begin alter table public.inbox_items add constraint inbox_items_share_id_fkey foreign key (share_id) references public.shares(id) on delete cascade; exception when duplicate_object then null; end;
  begin alter table public.activity_log add constraint activity_log_share_id_fkey foreign key (share_id) references public.shares(id) on delete set null; exception when duplicate_object then null; end;
end $$;

alter table public.share_likes alter column user_id set default auth.uid();
alter table public.share_saves alter column user_id set default auth.uid();

create or replace function public.enforce_share_moderation()
returns trigger language plpgsql set search_path = '' as $$
begin
  new.visibility := lower(new.visibility);
  new.status := lower(new.status);
  if btrim(new.from_name) = '' or btrim(new.from_handle) = '' or btrim(new.name) = '' then
    raise exception using errcode = '23514', message = 'social content requires profile identity and a title';
  end if;
  if char_length(new.from_name) > 80 or char_length(new.from_handle) > 40 or char_length(new.name) > 160 or char_length(new.subtitle) > 300 or char_length(new.message) > 280 then
    raise exception using errcode = '22001', message = 'social content exceeds allowed length';
  end if;
  if public.contains_objectionable_content(concat_ws(' ', new.from_name, new.from_handle, new.name, new.subtitle, new.message, new.payload_snapshot_json::text)) then
    raise exception using errcode = '23514', message = 'social content violates community policy';
  end if;
  if new.visibility = 'public' and coalesce((select p.is_private from public.profiles p where p.id = new.from_user_id and p.deleted_at is null), true) then
    raise exception using errcode = '23514', message = 'private profiles cannot publish public shares';
  end if;
  return new;
end;
$$;
drop trigger if exists shares_moderate_content on public.shares;
create trigger shares_moderate_content before insert or update on public.shares for each row execute function public.enforce_share_moderation();

create or replace function public.sync_share_inbox()
returns trigger language plpgsql security definer set search_path = public, pg_temp as $$
begin
  if new.visibility = 'direct' and new.target_user_id is not null and new.status = 'active' and new.deleted_at is null then
    update public.inbox_items set target_user_id = new.target_user_id where share_id = new.id;
    if not found then
      insert into public.inbox_items (share_id, target_user_id, created_at) values (new.id, new.target_user_id, new.created_at);
    end if;
  else
    delete from public.inbox_items where share_id = new.id;
  end if;
  return new;
end;
$$;
drop trigger if exists shares_sync_inbox on public.shares;
create trigger shares_sync_inbox after insert or update of visibility, target_user_id, status, deleted_at on public.shares
for each row execute function public.sync_share_inbox();

update public.inbox_items i set target_user_id = s.target_user_id
from public.shares s where i.share_id = s.id and s.visibility = 'direct' and s.target_user_id is not null and s.status = 'active' and s.deleted_at is null;
insert into public.inbox_items (share_id, target_user_id, created_at)
select s.id, s.target_user_id, s.created_at from public.shares s
where s.visibility = 'direct' and s.target_user_id is not null and s.status = 'active' and s.deleted_at is null
  and not exists (select 1 from public.inbox_items i where i.share_id = s.id);

alter table public.beans enable row level security;
alter table public.shares enable row level security;
drop policy if exists "Public profiles are readable by everyone" on public.profiles;
drop policy if exists "Public recipes are visible to everyone" on public.recipes;
drop policy if exists "Public techniques are visible to everyone" on public.techniques;
drop policy if exists "Public shares are visible to all users" on public.shares;
do $$ begin
  begin create policy profiles_self_read on public.profiles for select to authenticated using (id = auth.uid()); exception when duplicate_object then null; end;
  begin create policy beans_user_all on public.beans for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid()); exception when duplicate_object then null; end;
  begin create policy shares_read_allowed on public.shares for select to authenticated using (
    deleted_at is null and status = 'active' and
    (from_user_id = auth.uid() or target_user_id = auth.uid() or visibility = 'public') and
    not exists (select 1 from public.blocked_users b where (b.blocker_id = auth.uid() and b.blocked_user_id = from_user_id) or (b.blocker_id = from_user_id and b.blocked_user_id = auth.uid()))
  ); exception when duplicate_object then null; end;
  begin create policy shares_owner_insert on public.shares for insert to authenticated with check (from_user_id = auth.uid()); exception when duplicate_object then null; end;
  begin create policy shares_owner_update on public.shares for update to authenticated using (from_user_id = auth.uid()) with check (from_user_id = auth.uid()); exception when duplicate_object then null; end;
  begin create policy shares_owner_delete on public.shares for delete to authenticated using (from_user_id = auth.uid()); exception when duplicate_object then null; end;
end $$;

commit;
