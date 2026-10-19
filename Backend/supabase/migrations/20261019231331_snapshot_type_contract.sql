-- Reject malformed JSON types and conflicting aliases before either native decoder reads them.
begin;
create function private.validate_social_quantities(kind text,p jsonb) returns void
language plpgsql set search_path='' as $$
declare step jsonb; ingredient jsonb; dose numeric; water integer; temp numeric;
 ratio numeric; total_water integer:=0; total_seconds integer:=0; idx integer:=0; sec integer; added integer;
begin
 if jsonb_typeof(p)<>'object' or octet_length(p::text)>65536 or length(btrim(coalesce(p->>'name','')))=0
   or jsonb_typeof(p->'steps') is distinct from 'array' then
   raise exception using errcode='23514',message='Complete snapshot required (maximum 64 KiB)'; end if;
 if jsonb_array_length(p->'steps') not between 1 and 80 then raise exception using errcode='23514',message='Between 1 and 80 steps required'; end if;
 if kind='recipe' then
   if jsonb_typeof(p->'ingredients') is distinct from 'array' then raise exception using errcode='23514',message='Ingredients required'; end if;
   if jsonb_array_length(p->'ingredients') not between 1 and 80 then raise exception using errcode='23514',message='Invalid ingredients'; end if;
   for ingredient in select value from jsonb_array_elements(p->'ingredients') loop
     if btrim(coalesce(ingredient->>'name',''))='' or btrim(coalesce(ingredient->>'unit',''))=''
       or coalesce((ingredient->>'amount')::numeric,0)<=0 then raise exception using errcode='23514',message='Invalid ingredient'; end if;
   end loop;
   for step in select value from jsonb_array_elements(p->'steps') loop
     idx:=idx+1;
     if btrim(coalesce(step->>'instruction',''))='' or coalesce((step->>'stepNumber')::integer,idx)<>idx
       or (step->>'durationSeconds' is not null and (step->>'durationSeconds')::integer<=0) then
       raise exception using errcode='23514',message='Invalid recipe step'; end if;
   end loop;
 elsif kind='technique' then
   dose:=coalesce(p->>'doseG',p->>'coffeeGrams',p->>'doseGrams')::numeric;
   water:=(p->>'waterMl')::integer; ratio:=(p->>'ratio')::numeric;
   temp:=coalesce(p->>'temperatureC',p->>'temperature')::numeric;
   if dose is null or water is null or ratio is null or temp is null or dose<=0 or water<=0
     or temp not between 1 and 100 or abs(water/dose-ratio)>0.05
     or btrim(coalesce(p->>'methodName',p->>'method',''))='' then
     raise exception using errcode='23514',message='Invalid technique quantities'; end if;
   for step in select value from jsonb_array_elements(p->'steps') loop
     idx:=idx+1; sec:=coalesce(step->>'duration_sec',step->>'durationSeconds')::integer;
     added:=coalesce(step->>'water_add_ml',step->>'waterAddedMl')::integer;
     if sec is null or sec<=0 or added is null or added<0 or btrim(coalesce(step->>'title',''))=''
       or coalesce(step->>'step_order',step->>'stepNumber')::integer is distinct from idx then
       raise exception using errcode='23514',message='Invalid technique step'; end if;
     total_water:=total_water+added; total_seconds:=total_seconds+sec;
     if coalesce(step->>'target_water_ml',step->>'waterAccumulatedMl')::integer is distinct from total_water then
       raise exception using errcode='23514',message='Invalid cumulative water'; end if;
   end loop;
   if total_water<>water or total_seconds>86400 or
     (p->>'totalTimeSeconds' is not null and (p->>'totalTimeSeconds')::integer<>total_seconds) then
     raise exception using errcode='23514',message='Technique totals disagree with steps'; end if;
 else raise exception using errcode='23514',message='Unsupported content type'; end if;
end $$;
revoke all on function private.validate_social_quantities(text,jsonb) from public,anon,authenticated;
create or replace function private.validate_social_snapshot(kind text,p jsonb) returns void
language plpgsql set search_path='' as $$
declare k text; s jsonb; ingredient jsonb; pair text[];
begin
 if jsonb_typeof(p->'name') is distinct from 'string' then raise exception using errcode='23514',message='Snapshot name must be text'; end if;
 foreach k in array array['recipeKind','intention','suggestedMethodName','methodName','method','tags','executionMode','grindDescription','grindUnit','notes','description','techniqueDescription','ingredientsSummary','stepsSummary'] loop
   if p?k and p->k<>'null'::jsonb and jsonb_typeof(p->k)<>'string' then
     raise exception using errcode='23514',message='Snapshot text has invalid type'; end if;
 end loop;
 foreach k in array array['doseG','coffeeGrams','doseGrams','ratio','grindValue','waterMl','temperatureC','temperature','totalTimeSeconds'] loop
   if p?k and jsonb_typeof(p->k)<>'number' then raise exception using errcode='23514',message='Snapshot quantity must be a number'; end if;
 end loop;
 foreach k in array array['waterMl','temperatureC','temperature','totalTimeSeconds'] loop
   if p?k and (p->>k)::numeric<>trunc((p->>k)::numeric) then raise exception using errcode='23514',message='Water, time and Celsius must use whole numbers'; end if;
 end loop;
 foreach pair slice 1 in array array[['doseG','coffeeGrams'],['doseG','doseGrams'],['temperatureC','temperature'],['methodName','method']] loop
   if p?pair[1] and p?pair[2] and p->pair[1] is distinct from p->pair[2] then
     raise exception using errcode='23514',message='Snapshot aliases disagree'; end if;
 end loop;
 if kind='recipe' then
   if jsonb_typeof(p->'recipeKind') is distinct from 'string' then raise exception using errcode='23514',message='Recipe kind required'; end if;
   if jsonb_typeof(p->'ingredients')='array' then
     for ingredient in select value from jsonb_array_elements(p->'ingredients') loop
       if jsonb_typeof(ingredient->'amount') is distinct from 'number' or jsonb_typeof(ingredient->'name') is distinct from 'string' or jsonb_typeof(ingredient->'unit') is distinct from 'string' then
         raise exception using errcode='23514',message='Invalid ingredient JSON types'; end if;
     end loop;
   end if;
 end if;
 if jsonb_typeof(p->'steps')='array' then
   for s in select value from jsonb_array_elements(p->'steps') loop
     foreach k in array array['title','instruction','gesture','intensity','note','stepNote'] loop
       if s?k and s->k<>'null'::jsonb and jsonb_typeof(s->k)<>'string' then raise exception using errcode='23514',message='Invalid step text'; end if;
     end loop;
     foreach k in array array['step_order','stepNumber','duration_sec','durationSeconds','water_add_ml','waterAddedMl','target_water_ml','waterAccumulatedMl'] loop
       if s?k and s->k<>'null'::jsonb and (jsonb_typeof(s->k)<>'number' or (s->>k)::numeric<>trunc((s->>k)::numeric)) then
         raise exception using errcode='23514',message='Step values must be whole numbers'; end if;
     end loop;
     foreach pair slice 1 in array array[['stepNumber','step_order'],['durationSeconds','duration_sec'],['waterAddedMl','water_add_ml'],['waterAccumulatedMl','target_water_ml'],['note','stepNote']] loop
       if s?pair[1] and s?pair[2] and s->pair[1] is distinct from s->pair[2] then raise exception using errcode='23514',message='Step aliases disagree'; end if;
     end loop;
   end loop;
 end if;
 perform private.validate_social_quantities(kind,p);
end $$;
revoke all on function private.validate_social_snapshot(text,jsonb) from public,anon,authenticated;
commit;
