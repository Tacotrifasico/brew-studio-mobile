begin;

alter table public.ai_request_log
  add column if not exists outcome_code text not null default 'started';

alter table public.ai_request_log
  drop constraint if exists ai_request_log_outcome_code_check;
alter table public.ai_request_log
  add constraint ai_request_log_outcome_code_check check (
    outcome_code in ('started','success','invalid_output','quota_exhausted','upstream_error','timeout','unavailable')
  );

create or replace function public.consume_ai_request_quota(prompt_version_input text)
returns uuid
language plpgsql
security invoker
set search_path = public
as $$
declare
  actor uuid := auth.uid();
  recent_count integer;
  request_id uuid;
begin
  if actor is null then raise insufficient_privilege using message = 'authentication_required'; end if;
  if prompt_version_input is null or length(prompt_version_input) not between 1 and 80 then
    raise invalid_parameter_value using message = 'invalid_prompt_version';
  end if;

  perform pg_advisory_xact_lock(hashtext(actor::text));
  select count(*) into recent_count
  from public.ai_request_log
  where owner_id = actor and created_at >= now() - interval '60 seconds';
  if recent_count >= 5 then return null; end if;

  insert into public.ai_request_log(owner_id, prompt_version, outcome_code)
  values (actor, prompt_version_input, 'started')
  returning id into request_id;
  return request_id;
end;
$$;

revoke all on function public.consume_ai_request_quota(text) from public;
revoke all on function public.consume_ai_request_quota(text) from anon;
grant execute on function public.consume_ai_request_quota(text) to authenticated;

commit;
