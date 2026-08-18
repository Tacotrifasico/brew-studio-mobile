begin;

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
    insert into public.inbox_items (share_id, target_user_id, created_at)
    values (new.id, new.target_user_id, new.created_at)
    on conflict (share_id) do update
      set target_user_id = excluded.target_user_id;
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

insert into public.inbox_items (share_id, target_user_id, created_at)
select id, target_user_id, created_at
from public.brew_shares
where visibility = 'DIRECT' and target_user_id is not null and status = 'ACTIVE' and deleted_at is null
on conflict (share_id) do update set target_user_id = excluded.target_user_id;

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

commit;
