// Fresh-project tests use the REAL grants. Never grant all privileges to test roles.
import { PGlite } from '@electric-sql/pglite';
import { readFile, readdir } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import assert from 'node:assert/strict';
const root=path.dirname(path.dirname(fileURLToPath(import.meta.url)));
const db=new PGlite(); await db.waitReady;
const A='11111111-1111-4111-8111-111111111111', B='22222222-2222-4222-8222-222222222222', C='33333333-3333-4333-8333-333333333333';
await db.exec(`create role authenticated; create role anon; create schema auth;
  create table auth.users(id uuid primary key,email text,raw_user_meta_data jsonb default '{}');
  create function auth.uid() returns uuid language sql stable as $$select nullif(current_setting('request.jwt.claim.sub',true),'')::uuid$$;
  grant usage on schema auth to authenticated,anon; grant execute on function auth.uid() to authenticated,anon;`);
for(const f of (await readdir(path.join(root,'Backend/supabase/migrations'))).filter(f=>f.endsWith('.sql')&&!f.includes('local_first_community')&&!f.includes('snapshot_type_contract')).sort()) {
  const sql=await readFile(path.join(root,'Backend/supabase/migrations',f),'utf8');
  assert(!/ai_request|consume_ai|gemini/i.test(sql),'No AI artifacts in the production schema');
  await db.exec(sql.replace(/create extension if not exists pgcrypto;/gi,''));
  console.log('PASS migration',f);
}
let checks=0;
async function ok(sql,expected,label){const r=await db.query(sql); assert.deepEqual(r.rows,expected,label); checks++;console.log('PASS',label);}
async function deny(sql,label){await db.exec('savepoint rejection');let failed=false;try{await db.exec(sql);}catch{failed=true;}await db.exec('rollback to rejection; release rejection');assert(failed,label);checks++;console.log('PASS rejected:',label);}
async function as(user,role='authenticated'){await db.exec(`reset role; select set_config('request.jwt.claim.sub','${user??''}',false); set role ${role}`);}
await db.exec(`begin;
  insert into auth.users(id,email,raw_user_meta_data) values
  ('${A}','a@example.invalid','{"display_name":"Ana","handle":"ana","is_private":false}'),
  ('${B}','b@example.invalid','{"display_name":"Beto","handle":"beto"}'),
  ('${C}','c@example.invalid','{"display_name":"Caro","handle":"ana"}');`);
await ok('select count(*)::int as n from public.profiles',[{n:3}],'Signup trigger creates profiles, duplicate alias gets fallback');
await ok('select bool_and(is_private) as safe from public.profiles',[{safe:true}],'User metadata cannot make profiles public');
await as(A);
await db.exec(`insert into public.beans(id,name,stock_grams,brew_profiles) values('10000000-0000-4000-8000-000000000001','Ronpotrero',250,'{"v60":{"temperatureC":90.5555555556,"clicks":22}}');
  insert into public.recipes(id,name) values('20000000-0000-4000-8000-000000000001','Receta de Ana');
  insert into public.recipe_steps(recipe_id,instruction,step_number) values('20000000-0000-4000-8000-000000000001','Verter',1);
  insert into public.techniques(id,name,coffee_grams,water_ml,ratio,temperature) values('30000000-0000-4000-8000-000000000001','V60',15,240,16,92);
  insert into public.technique_steps(technique_id,step_order,title,duration_sec,water_add_ml,target_water_ml) values('30000000-0000-4000-8000-000000000001',1,'Verter',180,240,240);
  insert into public.cup_sessions(precise_temperature_c) values(90.5555555556);`);
await ok('select count(*)::int as n from public.cup_sessions where tasting_id is null',[{n:1}],'Cup can be saved without a tasting');
await ok(`select (brew_profiles->'v60'->>'temperatureC')::numeric > 90.55 as precise from public.beans`,[{precise:true}],'Per-method bean settings preserve Fahrenheit precision');
await deny(`insert into public.beans(user_id,name) values('${B}','Forged')`,'Cannot forge row ownership');
await deny(`insert into public.grinders(owner_id,user_id,name) values('${A}','${B}','Forged')`,'Legacy and canonical owner IDs cannot disagree');
await deny(`insert into public.coffee_beans(name) values('Bypass')`,'Client cannot write internal bean mirror');
await deny(`insert into public.beans(name,stock_grams) values('Negative',-1)`,'Negative inventory rejected');
await deny(`insert into public.shares(entity_type,entity_id,from_name,from_handle,name,payload_snapshot_json) values('recipe','20000000-0000-4000-8000-000000000001','Ana','ana','Privado','{}')`,'Private profile cannot publish public content');
await db.exec(`update public.profiles set is_private=false where id='${A}';
  insert into public.shares(id,entity_type,entity_id,from_name,from_handle,name,payload_snapshot_json) values('40000000-0000-4000-8000-000000000001','recipe','20000000-0000-4000-8000-000000000001','Ana','ana','Publicación','{}');
  insert into public.shares(id,entity_type,entity_id,from_name,from_handle,name,payload_snapshot_json,visibility,target_user_id) values('40000000-0000-4000-8000-000000000002','recipe','20000000-0000-4000-8000-000000000001','Ana','ana','Directo','{}','direct','${B}');`);
await as(B);
await ok('select count(*)::int as n from public.beans',[{n:0}],'Account B cannot read A beans');
await ok('select count(*)::int as n from public.recipe_steps',[{n:0}],'Account B cannot read A recipe steps');
await ok(`update public.beans set name='Intruder' returning id`,[],'Account B cannot update A beans');
await deny(`insert into public.recipe_steps(recipe_id,instruction,step_number) values('20000000-0000-4000-8000-000000000001','Intruder',2)`,'Cannot attach steps to another account recipe');
await deny(`insert into public.cup_sessions(bean_id) values('10000000-0000-4000-8000-000000000001')`,'Cannot reference another account bean');
await ok('select count(*)::int as n from public.shares',[{n:2}],'Recipient sees public and own direct share');
await ok('select count(*)::int as n from public.inbox_items',[{n:1}],'Direct share creates recipient inbox');
await db.exec('update public.inbox_items set read_at=now()');
await deny(`update public.inbox_items set target_user_id='${C}'`,'Recipient cannot retarget inbox');
await db.exec(`insert into public.share_saves(share_id) values('40000000-0000-4000-8000-000000000001');
  insert into public.content_reports(share_id,reason) values('40000000-0000-4000-8000-000000000001','Revisar');`);
await deny(`update public.content_reports set status='DISMISSED'`,'Reporter cannot moderate reports');
await ok(`select user_id from public.resolve_profile_alias('@ANA')`,[{user_id:A}],'Exact case-insensitive alias lookup');
await as(C);
await ok('select count(*)::int as n from public.shares',[{n:1}],'Third party cannot see a direct share');
await ok('select count(*)::int as n from public.share_saves',[{n:0}],'Saves remain private');
await deny(`insert into public.share_likes(share_id) values('40000000-0000-4000-8000-000000000002')`,'Cannot like inaccessible direct share');
await as(B);
await db.exec(`insert into public.blocked_users(blocked_user_id) values('${A}')`);
await ok('select count(*)::int as n from public.shares',[{n:0}],'Block hides author in both public and direct feeds');
await as(A);
await ok(`select user_id from public.resolve_profile_alias('beto')`,[],'Block respects the other account private block list');
await deny(`insert into public.shares(entity_type,entity_id,from_name,from_handle,name,payload_snapshot_json,visibility,target_user_id) values('recipe','20000000-0000-4000-8000-000000000001','Ana','ana','Blocked','{}','direct','${B}')`,'Cannot send to an account that blocked sender');
await db.exec(`update public.profiles set is_private=true where id='${A}'`);
await as(C);
await ok('select count(*)::int as n from public.shares',[{n:0}],'Turning profile private hides earlier public shares');
await as(null,'anon');
await deny('select * from public.beans','Anonymous inventory denied');
await deny('select * from public.profiles','Anonymous profile directory denied');
await deny(`select * from public.resolve_profile_alias('ana')`,'Anonymous alias lookup denied');
await db.exec('reset role');
await ok(`select count(*)::int as n from pg_tables where schemaname='public' and not rowsecurity`,[{n:0}],'Every public table has RLS');
await ok(`select count(*)::int as n from pg_proc p join pg_namespace n on n.oid=p.pronamespace where n.nspname in ('public','private') and p.prosecdef and has_function_privilege('anon',p.oid,'execute')`,[{n:0}],'No anonymous SECURITY DEFINER function');
await db.exec('rollback');
await import('./LocalFirstBackendChecks.mjs').then(m=>m.verifyLocalFirst(db,root));
await db.close();
console.log(`PASS ${checks} production-contract checks (fresh PostgreSQL, actual grants; no remote writes)`);
