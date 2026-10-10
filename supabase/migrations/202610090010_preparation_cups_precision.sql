-- AXCIS-ONLINE: apply in staging BEFORE enabling this version's sync clients.
-- A completed preparation is a cup even when no sensory tasting was performed.
alter table public.cup_sessions alter column tasting_id drop not null;
alter table public.cup_sessions add column if not exists precise_temperature_c double precision;
alter table public.brew_sessions add column if not exists precise_temperature_c double precision;
alter table public.lab_experiments add column if not exists precise_temperature_c double precision;
-- Android currently maps its local cups to public.cups; retain that legacy path.
alter table public.cups add column if not exists precise_temperature_c double precision;
-- Legacy integer columns stay intact. Null means use the legacy Celsius value.
