-- Prepara el esquema oficial Android antes de aplicar las extensiones nativas de iOS.
-- Sólo agrega columnas; no renombra, elimina ni sobrescribe datos productivos.
begin;

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

commit;
