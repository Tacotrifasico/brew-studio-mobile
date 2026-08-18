begin;

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
  biography text not null default '', preferences jsonb not null default '{}'::jsonb,
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

create table if not exists public.ai_request_log (
  id uuid primary key default gen_random_uuid(), owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  prompt_version text not null, created_at timestamptz not null default now()
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
create index if not exists ai_request_log_owner_created_idx on public.ai_request_log(owner_id, created_at desc);
create index if not exists brew_shares_feed_idx on public.brew_shares(visibility, status, created_at desc);
create index if not exists brew_shares_target_idx on public.brew_shares(target_user_id, created_at desc);
create index if not exists content_reports_status_idx on public.content_reports(status, created_at);

do $$
declare table_name text;
begin
  foreach table_name in array array['profiles','coffee_beans','grinders','equipment','recipes','recipe_ingredients','recipe_steps','techniques','technique_steps','brew_sessions','tastings','tasting_observations','cup_sessions']
  loop
    execute format('alter table public.%I enable row level security', table_name);
  end loop;
  alter table public.ai_request_log enable row level security;
  alter table public.blocked_users enable row level security;
  alter table public.brew_shares enable row level security;
  alter table public.share_likes enable row level security;
  alter table public.share_saves enable row level security;
  alter table public.content_reports enable row level security;
end $$;

do $$
declare table_name text; owner_column text;
begin
  foreach table_name in array array['coffee_beans','grinders','equipment','recipes','recipe_ingredients','recipe_steps','techniques','technique_steps','brew_sessions','tastings','tasting_observations','cup_sessions']
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
    create policy ai_request_log_owner_all on public.ai_request_log for all to authenticated using (owner_id = auth.uid()) with check (owner_id = auth.uid());
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
  foreach table_name in array array['profiles','coffee_beans','grinders','equipment','recipes','recipe_ingredients','recipe_steps','techniques','technique_steps','brew_sessions','tastings','tasting_observations','cup_sessions']
  loop
    execute format('drop trigger if exists %I on public.%I', table_name || '_set_updated_at', table_name);
    execute format('create trigger %I before update on public.%I for each row execute function public.set_updated_at()', table_name || '_set_updated_at', table_name);
  end loop;
  drop trigger if exists brew_shares_set_updated_at on public.brew_shares;
  create trigger brew_shares_set_updated_at before update on public.brew_shares for each row execute function public.set_updated_at();
end $$;

commit;
