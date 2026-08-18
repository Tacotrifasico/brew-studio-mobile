-- Preserva referencias estables y snapshots usados por Laboratorio y Preparación.
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
