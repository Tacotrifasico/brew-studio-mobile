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
