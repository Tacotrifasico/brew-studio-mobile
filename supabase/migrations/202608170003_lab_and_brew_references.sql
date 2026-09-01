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
