-- Fresh-project baseline. Historical upgrades remain in supabase/migrations.
-- Generated from reviewed client contracts; no AI tables or functions.
begin;

-- Source: 202608160000_android_schema_preflight.sql
-- Prepara el esquema oficial Android antes de aplicar las extensiones nativas de iOS.
-- Sólo agrega columnas; no renombra, elimina ni sobrescribe datos productivos.

alter table if exists public.profiles
  add column if not exists alias text not null default '',
  add column if not exists biography text not null default '',
  add column if not exists favorite_methods text not null default '',
  add column if not exists preferences jsonb not null default '{}'::jsonb,
  add column if not exists is_private boolean not null default true,
  add column if not exists version bigint not null default 1,
  add column if not exists deleted_at timestamptz;

alter table if exists public.grinders
  add column if not exists owner_id uuid references auth.users(id) on delete cascade,
  add column if not exists name text not null default '',
  add column if not exists grinder_type text not null default 'MANUAL',
  add column if not exists scale_unit text not null default 'CLICKS',
  add column if not exists minimum_setting integer not null default 0,
  add column if not exists maximum_setting integer not null default 100,
  add column if not exists notes text not null default '',
  add column if not exists version bigint not null default 1,
  add column if not exists deleted_at timestamptz;

alter table if exists public.equipment
  add column if not exists owner_id uuid references auth.users(id) on delete cascade,
  add column if not exists equipment_type text not null default 'OTHER',
  add column if not exists brand text not null default '',
  add column if not exists model text not null default '',
  add column if not exists capacity_ml integer,
  add column if not exists configuration text not null default '',
  add column if not exists is_favorite boolean not null default false,
  add column if not exists is_active boolean not null default true,
  add column if not exists version bigint not null default 1,
  add column if not exists deleted_at timestamptz;

alter table if exists public.recipes
  add column if not exists owner_id uuid references auth.users(id) on delete cascade,
  add column if not exists recipe_kind text not null default 'BLACK_COFFEE',
  add column if not exists intention text not null default '',
  add column if not exists suggested_method_id uuid,
  add column if not exists suggested_method_name text not null default '',
  add column if not exists is_favorite boolean not null default false,
  add column if not exists tags text not null default '',
  add column if not exists root_entity_id uuid,
  add column if not exists version bigint not null default 1,
  add column if not exists deleted_at timestamptz;

alter table if exists public.techniques
  add column if not exists owner_id uuid references auth.users(id) on delete cascade,
  add column if not exists method_id uuid,
  add column if not exists method_name text not null default '',
  add column if not exists recipe_id uuid,
  add column if not exists dose_grams numeric not null default 15,
  add column if not exists temperature_c integer not null default 93,
  add column if not exists execution_mode text not null default 'GUIDED',
  add column if not exists grind_value numeric not null default 0,
  add column if not exists grind_description text not null default '',
  add column if not exists grind_unit text not null default 'CLICKS',
  add column if not exists description text not null default '',
  add column if not exists total_time_seconds integer not null default 0,
  add column if not exists root_entity_id uuid,
  add column if not exists version bigint not null default 1,
  add column if not exists deleted_at timestamptz;

alter table if exists public.technique_steps
  add column if not exists owner_id uuid references auth.users(id) on delete cascade,
  add column if not exists step_number integer,
  add column if not exists duration_seconds integer not null default 0,
  add column if not exists water_added_ml integer not null default 0,
  add column if not exists water_accumulated_ml integer not null default 0,
  add column if not exists step_note text not null default '',
  add column if not exists coverage numeric,
  add column if not exists flow numeric,
  add column if not exists secondary_action text,
  add column if not exists version bigint not null default 1,
  add column if not exists deleted_at timestamptz;

alter table if exists public.lab_experiments
  add column if not exists owner_id uuid references auth.users(id) on delete cascade,
  add column if not exists method_id uuid,
  add column if not exists recipe_id uuid,
  add column if not exists technique_id uuid,
  add column if not exists bean_id uuid,
  add column if not exists grinder_id uuid,
  add column if not exists method text not null default '',
  add column if not exists temperature_c integer not null default 93,
  add column if not exists grind_clicks integer not null default 0,
  add column if not exists freshness text not null default '',
  add column if not exists time_seconds integer not null default 0,
  add column if not exists altitude_meters integer not null default 0,
  add column if not exists city_name text not null default '',
  add column if not exists notes text not null default '',
  add column if not exists extraction_index numeric not null default 0,
  add column if not exists summary text not null default '',
  add column if not exists version bigint not null default 1,
  add column if not exists deleted_at timestamptz;

do $$
declare table_name text;
begin
  foreach table_name in array array['grinders','equipment','recipes','techniques','technique_steps','lab_experiments']
  loop
    if to_regclass('public.' || table_name) is not null then
      execute format('update public.%I set owner_id = user_id where owner_id is null and user_id is not null', table_name);
    end if;
  end loop;
end $$;



-- Source: 202608170001_ios_core_schema.sql

create extension if not exists pgcrypto;

create or replace function public.set_updated_at()
returns trigger language plpgsql security invoker set search_path = public as $$
begin
  new.updated_at = now();
  new.version = greatest(coalesce(old.version, 0) + 1, coalesce(new.version, 1));
  return new;
end;
$$;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  display_name text not null default '', alias text not null default '', avatar_url text,
  biography text not null default '', avatar_color text not null default '#3F7A63', favorite_methods text not null default '',
  preferences jsonb not null default '{}'::jsonb,
  is_private boolean not null default true, created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(), version bigint not null default 1, deleted_at timestamptz
);

create table if not exists public.coffee_beans (
  id uuid primary key default gen_random_uuid(), owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  name text not null, brand text not null default '', origin text not null default '', producer text not null default '',
  variety text not null default '', process text not null default '', altitude_meters integer,
  roast_level text not null default 'Medio', roast_date date, opened_date date,
  initial_quantity_grams numeric not null default 0 check (initial_quantity_grams >= 0),
  remaining_quantity_grams numeric not null default 0 check (remaining_quantity_grams >= 0),
  notes text not null default '', photo_path text, created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(), version bigint not null default 1, deleted_at timestamptz
);

create table if not exists public.grinders (
  id uuid primary key default gen_random_uuid(), owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  name text not null, brand text not null default '', model text not null default '', grinder_type text not null default 'MANUAL',
  scale_unit text not null default 'CLICKS', minimum_setting integer not null default 0, maximum_setting integer not null default 40,
  calibration_notes text not null default '', notes text not null default '', created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(), version bigint not null default 1, deleted_at timestamptz,
  check (maximum_setting >= minimum_setting)
);

create table if not exists public.equipment (
  id uuid primary key default gen_random_uuid(), owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  equipment_type text not null, name text not null, brand text not null default '', model text not null default '', capacity_ml integer,
  configuration text not null default '', notes text not null default '', is_favorite boolean not null default false,
  is_active boolean not null default true, created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
  version bigint not null default 1, deleted_at timestamptz, check (capacity_ml is null or capacity_ml >= 0)
);

create table if not exists public.recipes (
  id uuid primary key default gen_random_uuid(), owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  name text not null, recipe_kind text not null default 'BLACK_COFFEE', intention text not null default '', suggested_method_id uuid,
  suggested_method_name text not null default '', is_favorite boolean not null default false, tags text not null default '',
  visibility text not null default 'PRIVATE', original_entity_id uuid, root_entity_id uuid, copy_mode text not null default 'ORIGINAL',
  created_at timestamptz not null default now(), updated_at timestamptz not null default now(), version bigint not null default 1, deleted_at timestamptz
);

create table if not exists public.recipe_ingredients (
  id uuid primary key default gen_random_uuid(), owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  recipe_id uuid not null references public.recipes(id) on delete cascade, name text not null, amount numeric not null check (amount >= 0),
  unit text not null, order_index integer not null default 0, created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(), version bigint not null default 1, deleted_at timestamptz
);

create table if not exists public.recipe_steps (
  id uuid primary key default gen_random_uuid(), owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  recipe_id uuid not null references public.recipes(id) on delete cascade, instruction text not null, step_number integer not null,
  duration_seconds integer, created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
  version bigint not null default 1, deleted_at timestamptz, check (step_number > 0), check (duration_seconds is null or duration_seconds >= 0)
);

create table if not exists public.techniques (
  id uuid primary key default gen_random_uuid(), owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  name text not null, method_id uuid, method_name text not null default '', recipe_id uuid references public.recipes(id) on delete set null,
  bean_id uuid references public.coffee_beans(id) on delete set null, grinder_id uuid references public.grinders(id) on delete set null,
  dose_grams numeric not null, water_ml integer not null, ratio numeric not null, temperature_c integer not null,
  execution_mode text not null default 'GUIDED', grind_value numeric not null default 0, grind_description text not null default '',
  grind_unit text not null default 'CLICKS', notes text not null default '', description text not null default '',
  total_time_seconds integer not null default 0, visibility text not null default 'PRIVATE', original_entity_id uuid, root_entity_id uuid,
  copy_mode text not null default 'ORIGINAL', created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
  version bigint not null default 1, deleted_at timestamptz,
  check (dose_grams > 0 and water_ml > 0 and ratio > 0 and total_time_seconds >= 0)
);

create table if not exists public.technique_steps (
  id uuid primary key default gen_random_uuid(), owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  technique_id uuid not null references public.techniques(id) on delete cascade, step_number integer not null, title text not null,
  duration_seconds integer not null default 0, water_added_ml integer not null default 0, water_accumulated_ml integer not null default 0,
  intensity text not null default 'MEDIUM', gesture text not null default 'CIRCULAR_POUR', step_note text not null default '',
  coverage numeric, flow numeric, secondary_action text, created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
  version bigint not null default 1, deleted_at timestamptz,
  check (step_number > 0 and duration_seconds >= 0 and water_added_ml >= 0 and water_accumulated_ml >= 0)
);

create table if not exists public.brew_sessions (
  id uuid primary key default gen_random_uuid(), owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  technique_id uuid references public.techniques(id) on delete set null, recipe_id uuid references public.recipes(id) on delete set null,
  bean_id uuid references public.coffee_beans(id) on delete set null, grinder_id uuid references public.grinders(id) on delete set null,
  technique_name_snapshot text not null default '', method_name_snapshot text not null default '', bean_name_snapshot text not null default '',
  grinder_name_snapshot text not null default '', dose_grams numeric not null, water_ml integer not null, ratio numeric not null,
  temperature_c integer not null, grind_description text not null default '', elapsed_seconds integer not null default 0,
  completed_at timestamptz not null default now(), steps_snapshot jsonb not null default '[]'::jsonb,
  created_at timestamptz not null default now(), updated_at timestamptz not null default now(), version bigint not null default 1, deleted_at timestamptz
);

create table if not exists public.tastings (
  id uuid primary key default gen_random_uuid(), owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  brew_session_id uuid references public.brew_sessions(id) on delete set null, recipe_id uuid references public.recipes(id) on delete set null,
  technique_id uuid references public.techniques(id) on delete set null, bean_id uuid references public.coffee_beans(id) on delete set null,
  active_flavor_family text not null default 'FRUITY', selected_flavor_notes jsonb not null default '[]'::jsonb,
  expected_notes text not null default '', texture text not null default '', cleanliness text not null default '', persistence text not null default '',
  aroma numeric not null default 3, acidity numeric not null default 3, sweetness numeric not null default 3,
  body numeric not null default 3, bitterness numeric not null default 3, finish numeric not null default 3,
  rating numeric not null default 4, nps integer not null default 8, evaluator_notes text not null default '',
  cooling_elapsed_seconds integer not null default 0, cup_life_state text not null default 'FRESH', evaluated_at timestamptz not null default now(),
  created_at timestamptz not null default now(), updated_at timestamptz not null default now(), version bigint not null default 1, deleted_at timestamptz,
  check (rating between 1 and 5 and nps between 0 and 10)
);

create table if not exists public.tasting_observations (
  id uuid primary key default gen_random_uuid(), owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  tasting_id uuid not null references public.tastings(id) on delete cascade, elapsed_seconds integer not null,
  stage text not null, notes text not null default '', aroma numeric not null, acidity numeric not null, sweetness numeric not null,
  body numeric not null, bitterness numeric not null, finish numeric not null, created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(), version bigint not null default 1, deleted_at timestamptz
);

create table if not exists public.cup_sessions (
  id uuid primary key default gen_random_uuid(), owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  brew_session_id uuid references public.brew_sessions(id) on delete set null,
  tasting_id uuid not null unique references public.tastings(id) on delete cascade,
  technique_name_snapshot text not null default '', bean_name_snapshot text not null default '', rating numeric not null default 0,
  created_at timestamptz not null default now(), updated_at timestamptz not null default now(), version bigint not null default 1, deleted_at timestamptz
);

create table if not exists public.lab_experiments (
  id uuid primary key default gen_random_uuid(), owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  method text not null, coffee_grams numeric not null, water_ml integer not null, ratio numeric not null, temperature_c integer not null,
  grind_clicks integer not null, freshness text not null, time_seconds integer not null, altitude_meters integer not null default 0,
  city_name text not null default '', notes text not null default '', extraction_index numeric not null, summary text not null default '',
  created_at timestamptz not null default now(), updated_at timestamptz not null default now(), version bigint not null default 1, deleted_at timestamptz
);



create table if not exists public.blocked_users (
  blocker_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  blocked_user_id uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(), primary key (blocker_id, blocked_user_id), check (blocker_id <> blocked_user_id)
);

create table if not exists public.brew_shares (
  id uuid primary key default gen_random_uuid(), owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  entity_type text not null check (entity_type in ('recipe','technique')), entity_id uuid not null,
  from_name text not null, from_handle text not null, target_user_id uuid references auth.users(id) on delete cascade,
  visibility text not null default 'PUBLIC' check (visibility in ('PUBLIC','DIRECT')),
  name text not null, subtitle text not null default '', message text not null default '', payload_snapshot jsonb not null,
  original_entity_id uuid, status text not null default 'ACTIVE' check (status in ('ACTIVE','REMOVED')),
  created_at timestamptz not null default now(), updated_at timestamptz not null default now(), version bigint not null default 1, deleted_at timestamptz
);

create table if not exists public.share_likes (
  share_id uuid not null references public.brew_shares(id) on delete cascade, user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(), primary key (share_id, user_id)
);
create table if not exists public.share_saves (
  share_id uuid not null references public.brew_shares(id) on delete cascade, user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(), primary key (share_id, user_id)
);
create table if not exists public.content_reports (
  id uuid primary key default gen_random_uuid(), reporter_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  share_id uuid not null references public.brew_shares(id) on delete cascade, reason text not null,
  status text not null default 'OPEN' check (status in ('OPEN','REVIEWED','DISMISSED','ACTIONED')), created_at timestamptz not null default now(), unique (reporter_id, share_id)
);

create index if not exists coffee_beans_owner_updated_idx on public.coffee_beans(owner_id, updated_at);
create index if not exists grinders_owner_updated_idx on public.grinders(owner_id, updated_at);
create index if not exists equipment_owner_updated_idx on public.equipment(owner_id, updated_at);
create index if not exists recipes_owner_updated_idx on public.recipes(owner_id, updated_at);
create index if not exists recipe_ingredients_parent_idx on public.recipe_ingredients(recipe_id, order_index);
create index if not exists recipe_steps_parent_idx on public.recipe_steps(recipe_id, step_number);
create index if not exists techniques_owner_updated_idx on public.techniques(owner_id, updated_at);
create index if not exists technique_steps_parent_idx on public.technique_steps(technique_id, step_number);
create index if not exists brew_sessions_owner_updated_idx on public.brew_sessions(owner_id, updated_at);
create index if not exists tastings_owner_updated_idx on public.tastings(owner_id, updated_at);
create index if not exists tasting_observations_parent_idx on public.tasting_observations(tasting_id, elapsed_seconds);
create index if not exists cup_sessions_owner_updated_idx on public.cup_sessions(owner_id, updated_at);
create index if not exists lab_experiments_owner_updated_idx on public.lab_experiments(owner_id, updated_at);
create index if not exists brew_shares_feed_idx on public.brew_shares(visibility, status, created_at desc);
create index if not exists brew_shares_target_idx on public.brew_shares(target_user_id, created_at desc);
create index if not exists content_reports_status_idx on public.content_reports(status, created_at);

do $$
declare table_name text;
begin
  foreach table_name in array array['profiles','coffee_beans','grinders','equipment','recipes','recipe_ingredients','recipe_steps','techniques','technique_steps','brew_sessions','tastings','tasting_observations','cup_sessions','lab_experiments']
  loop
    execute format('alter table public.%I enable row level security', table_name);
  end loop;
  alter table public.blocked_users enable row level security;
  alter table public.brew_shares enable row level security;
  alter table public.share_likes enable row level security;
  alter table public.share_saves enable row level security;
  alter table public.content_reports enable row level security;
end $$;

do $$
declare table_name text; owner_column text;
begin
  foreach table_name in array array['coffee_beans','grinders','equipment','recipes','recipe_ingredients','recipe_steps','techniques','technique_steps','brew_sessions','tastings','tasting_observations','cup_sessions','lab_experiments']
  loop
    begin
      execute format('create policy %I on public.%I for all to authenticated using (owner_id = auth.uid()) with check (owner_id = auth.uid())', table_name || '_owner_all', table_name);
    exception when duplicate_object then null;
    end;
  end loop;
  begin
    create policy profiles_owner_all on public.profiles for all to authenticated using (id = auth.uid()) with check (id = auth.uid());
  exception when duplicate_object then null;
  end;
  begin
  exception when duplicate_object then null;
  end;
  begin create policy blocked_users_owner_all on public.blocked_users for all to authenticated using (blocker_id = auth.uid()) with check (blocker_id = auth.uid()); exception when duplicate_object then null; end;
  begin create policy brew_shares_read_allowed on public.brew_shares for select to authenticated using (
    deleted_at is null and status = 'ACTIVE' and (owner_id = auth.uid() or target_user_id = auth.uid() or visibility = 'PUBLIC') and
    not exists (select 1 from public.blocked_users b where (b.blocker_id = auth.uid() and b.blocked_user_id = owner_id) or (b.blocker_id = owner_id and b.blocked_user_id = auth.uid()))
  ); exception when duplicate_object then null; end;
  begin create policy brew_shares_owner_write on public.brew_shares for all to authenticated using (owner_id = auth.uid()) with check (owner_id = auth.uid()); exception when duplicate_object then null; end;
  begin create policy share_likes_owner_write on public.share_likes for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid()); exception when duplicate_object then null; end;
  begin create policy share_saves_owner_all on public.share_saves for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid()); exception when duplicate_object then null; end;
  begin create policy content_reports_owner_insert on public.content_reports for insert to authenticated with check (reporter_id = auth.uid()); exception when duplicate_object then null; end;
  begin create policy content_reports_owner_read on public.content_reports for select to authenticated using (reporter_id = auth.uid()); exception when duplicate_object then null; end;
end $$;

do $$
declare table_name text;
begin
  foreach table_name in array array['profiles','coffee_beans','grinders','equipment','recipes','recipe_ingredients','recipe_steps','techniques','technique_steps','brew_sessions','tastings','tasting_observations','cup_sessions','lab_experiments']
  loop
    execute format('drop trigger if exists %I on public.%I', table_name || '_set_updated_at', table_name);
    execute format('create trigger %I before update on public.%I for each row execute function public.set_updated_at()', table_name || '_set_updated_at', table_name);
  end loop;
  drop trigger if exists brew_shares_set_updated_at on public.brew_shares;
  create trigger brew_shares_set_updated_at before update on public.brew_shares for each row execute function public.set_updated_at();
end $$;



-- Source: 202608170002_social_content_moderation.sql

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



-- Source: 202608170003_lab_and_brew_references.sql
-- Preserva referencias estables y snapshots usados por Laboratorio y Preparación.
-- En instalaciones Android, crea primero el espejo de granos para que bean_id conserve su referencia.
do $$
begin
  if to_regclass('public.beans') is not null then
    insert into public.coffee_beans (
      id, owner_id, name, brand, origin, producer, variety, process, altitude_meters,
      roast_level, roast_date, opened_date, initial_quantity_grams, remaining_quantity_grams,
      notes, created_at, updated_at, version, deleted_at
    )
    select
      id, user_id, name, coalesce(roaster, ''), coalesce(origin, ''), '', '', coalesce(process, ''),
      case when coalesce(altitude, '') ~ '^[0-9]+$' then altitude::integer else null end, 'Medio',
      case when coalesce(roast_date, '') ~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}$' then roast_date::date else null end,
      case when coalesce(first_use_date, '') ~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}$' then first_use_date::date else null end,
      stock_grams, stock_grams, coalesce(notes, ''), created_at, updated_at, 1, null
    from public.beans
    on conflict (id) do update set
      owner_id = excluded.owner_id, name = excluded.name, brand = excluded.brand,
      origin = excluded.origin, process = excluded.process, altitude_meters = excluded.altitude_meters,
      roast_date = excluded.roast_date, opened_date = excluded.opened_date,
      remaining_quantity_grams = excluded.remaining_quantity_grams, notes = excluded.notes,
      updated_at = excluded.updated_at;
  end if;
end $$;

alter table public.brew_sessions
  add column if not exists method_id uuid,
  add column if not exists recipe_name_snapshot text not null default '';

alter table public.lab_experiments
  add column if not exists method_id uuid,
  add column if not exists recipe_id uuid,
  add column if not exists technique_id uuid,
  add column if not exists bean_id uuid,
  add column if not exists grinder_id uuid;

do $$
begin
  if not exists (select 1 from pg_constraint where conname = 'brew_sessions_method_id_fkey') then
    alter table public.brew_sessions add constraint brew_sessions_method_id_fkey foreign key (method_id) references public.equipment(id) on delete set null;
  end if;
  if not exists (select 1 from pg_constraint where conname = 'lab_experiments_method_id_fkey') then
    alter table public.lab_experiments add constraint lab_experiments_method_id_fkey foreign key (method_id) references public.equipment(id) on delete set null;
  end if;
  if not exists (select 1 from pg_constraint where conname = 'lab_experiments_recipe_id_fkey') then
    alter table public.lab_experiments add constraint lab_experiments_recipe_id_fkey foreign key (recipe_id) references public.recipes(id) on delete set null;
  end if;
  if not exists (select 1 from pg_constraint where conname = 'lab_experiments_technique_id_fkey') then
    alter table public.lab_experiments add constraint lab_experiments_technique_id_fkey foreign key (technique_id) references public.techniques(id) on delete set null;
  end if;
  if not exists (select 1 from pg_constraint where conname = 'lab_experiments_bean_id_fkey') then
    alter table public.lab_experiments add constraint lab_experiments_bean_id_fkey foreign key (bean_id) references public.coffee_beans(id) on delete set null;
  end if;
  if not exists (select 1 from pg_constraint where conname = 'lab_experiments_grinder_id_fkey') then
    alter table public.lab_experiments add constraint lab_experiments_grinder_id_fkey foreign key (grinder_id) references public.grinders(id) on delete set null;
  end if;
end $$;

create index if not exists brew_sessions_method_idx on public.brew_sessions(method_id);
create index if not exists lab_experiments_method_idx on public.lab_experiments(method_id);
create index if not exists lab_experiments_recipe_idx on public.lab_experiments(recipe_id);
create index if not exists lab_experiments_technique_idx on public.lab_experiments(technique_id);
create index if not exists lab_experiments_bean_idx on public.lab_experiments(bean_id);
create index if not exists lab_experiments_grinder_idx on public.lab_experiments(grinder_id);


-- Source: 202608170004_cup_history_snapshots.sql
-- Complete the iOS cup history without rewriting an already deployed migration.
alter table public.cup_sessions
  add column if not exists recipe_id uuid references public.recipes(id) on delete set null,
  add column if not exists bean_id uuid references public.coffee_beans(id) on delete set null,
  add column if not exists technique_id uuid references public.techniques(id) on delete set null,
  add column if not exists method_id uuid,
  add column if not exists grinder_id uuid references public.grinders(id) on delete set null,
  add column if not exists executed_dose_g numeric not null default 0,
  add column if not exists executed_water_ml integer not null default 0,
  add column if not exists executed_ratio numeric not null default 0,
  add column if not exists executed_temperature_c integer not null default 0,
  add column if not exists executed_grind_setting text not null default '',
  add column if not exists executed_duration_seconds integer not null default 0,
  add column if not exists recipe_name_snapshot text not null default '',
  add column if not exists method_name_snapshot text not null default '',
  add column if not exists grinder_name_snapshot text not null default '',
  add column if not exists cup_life_seconds integer not null default 0,
  add column if not exists cup_life_state text not null default 'FRESH',
  add column if not exists nps integer not null default 0,
  add column if not exists comment text not null default '',
  add column if not exists brew_date timestamptz,
  add column if not exists recipe_snapshot jsonb not null default '{}'::jsonb,
  add column if not exists technique_snapshot jsonb not null default '{}'::jsonb,
  add column if not exists bean_snapshot jsonb not null default '{}'::jsonb,
  add column if not exists grinder_snapshot jsonb not null default '{}'::jsonb;

create index if not exists cup_sessions_owner_brew_date_idx
  on public.cup_sessions(owner_id, brew_date desc)
  where deleted_at is null;

create index if not exists cup_sessions_recipe_id_idx on public.cup_sessions(recipe_id);
create index if not exists cup_sessions_bean_id_idx on public.cup_sessions(bean_id);
create index if not exists cup_sessions_technique_id_idx on public.cup_sessions(technique_id);
create index if not exists cup_sessions_grinder_id_idx on public.cup_sessions(grinder_id);

do $$
begin
  if not exists (select 1 from pg_constraint where conname = 'cup_sessions_method_id_fkey') then
    alter table public.cup_sessions add constraint cup_sessions_method_id_fkey
      foreign key (method_id) references public.equipment(id) on delete set null;
  end if;
end $$;

create index if not exists cup_sessions_method_id_idx on public.cup_sessions(method_id);


-- Source: 202608180005_social_inbox_activity.sql

create table if not exists public.inbox_items (
  id uuid primary key default gen_random_uuid(),
  share_id uuid not null unique references public.brew_shares(id) on delete cascade,
  target_user_id uuid not null references auth.users(id) on delete cascade,
  read_at timestamptz,
  created_at timestamptz not null default now()
);

create table if not exists public.activity_log (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  action text not null check (char_length(action) between 1 and 80),
  entity_type text check (entity_type is null or entity_type in ('recipe', 'technique')),
  entity_id uuid,
  share_id uuid references public.brew_shares(id) on delete set null,
  note text check (note is null or char_length(note) <= 500),
  created_at timestamptz not null default now()
);

create index if not exists inbox_items_target_created_idx on public.inbox_items(target_user_id, created_at desc);
create index if not exists activity_log_user_created_idx on public.activity_log(user_id, created_at desc);

create or replace function public.sync_direct_share_inbox()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
  if new.visibility = 'DIRECT' and new.target_user_id is not null and new.status = 'ACTIVE' and new.deleted_at is null then
    update public.inbox_items set target_user_id = new.target_user_id where share_id = new.id;
    if not found then
      insert into public.inbox_items (share_id, target_user_id, created_at)
      values (new.id, new.target_user_id, new.created_at);
    end if;
  else
    delete from public.inbox_items where share_id = new.id;
  end if;
  return new;
end;
$$;

drop trigger if exists brew_shares_sync_inbox on public.brew_shares;
create trigger brew_shares_sync_inbox
after insert or update of visibility, target_user_id, status, deleted_at
on public.brew_shares
for each row execute function public.sync_direct_share_inbox();

update public.inbox_items i set target_user_id = s.target_user_id
from public.brew_shares s
where i.share_id = s.id and s.visibility = 'DIRECT' and s.target_user_id is not null and s.status = 'ACTIVE' and s.deleted_at is null;
insert into public.inbox_items (share_id, target_user_id, created_at)
select s.id, s.target_user_id, s.created_at
from public.brew_shares s
where s.visibility = 'DIRECT' and s.target_user_id is not null and s.status = 'ACTIVE' and s.deleted_at is null
  and not exists (select 1 from public.inbox_items i where i.share_id = s.id);

alter table public.inbox_items enable row level security;
alter table public.activity_log enable row level security;

do $$
begin
  begin
    create policy inbox_items_recipient_read on public.inbox_items
      for select to authenticated using (target_user_id = auth.uid());
  exception when duplicate_object then null;
  end;
  begin
    create policy inbox_items_recipient_update on public.inbox_items
      for update to authenticated using (target_user_id = auth.uid()) with check (target_user_id = auth.uid());
  exception when duplicate_object then null;
  end;
  begin
    create policy activity_log_owner_read on public.activity_log
      for select to authenticated using (user_id = auth.uid());
  exception when duplicate_object then null;
  end;
  begin
    create policy activity_log_owner_insert on public.activity_log
      for insert to authenticated with check (user_id = auth.uid());
  exception when duplicate_object then null;
  end;
end $$;

revoke insert, update, delete on public.inbox_items from authenticated;
grant select on public.inbox_items to authenticated;
grant update (read_at) on public.inbox_items to authenticated;
revoke update, delete on public.activity_log from authenticated;
grant select, insert on public.activity_log to authenticated;



-- Source: 202609010006_profile_and_ugc_hardening.sql

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



-- Source: 202609010007_android_backend_alignment.sql
-- Alinea el contrato remoto iOS con las tablas y columnas oficiales de Android.
-- Es idempotente y conserva las columnas legadas para que ambas aplicaciones convivan.

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



-- Source: 202609050008_direct_recipient_alias.sql

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



-- Source: 202610090010_preparation_cups_precision.sql
-- AXCIS-ONLINE: apply in staging BEFORE enabling this version's sync clients.
-- A completed preparation is a cup even when no sensory tasting was performed.
alter table public.cup_sessions alter column tasting_id drop not null;
alter table public.cup_sessions add column if not exists precise_temperature_c double precision;
alter table public.brew_sessions add column if not exists precise_temperature_c double precision;
alter table public.lab_experiments add column if not exists precise_temperature_c double precision;
-- Android currently maps its local cups to public.cups; retain that legacy path.
-- A fresh iOS-only database has cup_sessions but not this optional legacy table.
alter table if exists public.cups add column if not exists precise_temperature_c double precision;
-- Legacy integer columns stay intact. Null means use the legacy Celsius value.


-- Fail closed until the following privileges migration.
revoke all on all tables in schema public from public,anon,authenticated;
revoke all on all functions in schema public from public,anon,authenticated;
commit;
