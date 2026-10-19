import {readFile,readdir} from 'node:fs/promises';
import assert from 'node:assert/strict';
export async function verifyLocalFirst(db,root){
 for(const f of (await readdir(root+'/Backend/supabase/migrations')).filter(f=>f.includes('local_first_community')||f.includes('snapshot_type_contract')).sort())
   await db.exec(await readFile(root+'/Backend/supabase/migrations/'+f,'utf8'));
 const A='11111111-1111-4111-8111-111111111111',B='22222222-2222-4222-8222-222222222222';
 const share='40000000-0000-4000-8000-000000000001',copy='40000000-0000-4000-8000-000000000002';
 const payload=JSON.stringify({name:'Receta',recipeKind:'BLACK_COFFEE',ingredients:[{name:'Café',amount:15,unit:'g'}],steps:[{stepNumber:1,instruction:'Preparar'}]});
 let count=0;
 async function ok(sql,value,label){assert.deepEqual((await db.query(sql)).rows,value,label);count++;console.log('PASS local-first:',label);}
 async function deny(sql,label){await db.exec('savepoint no');let caught=false;try{await db.exec(sql)}catch{caught=true}await db.exec('rollback to no;release no');assert(caught,label);count++;console.log('PASS local-first rejected:',label);}
 async function as(id){await db.exec(`reset role;select set_config('request.jwt.claim.sub','${id}',false);set role authenticated`)}
 await db.exec(`begin;insert into auth.users(id,email,raw_user_meta_data) values('${A}','a@example.invalid','{"display_name":"Ana","handle":"ana"}'),('${B}','b@example.invalid','{"display_name":"Beto","handle":"beto"}');`);
 await as(A);
 for(const table of ['beans','coffee_beans','grinders','equipment','recipes','recipe_ingredients','recipe_steps','techniques','technique_steps','brew_sessions','tastings','tasting_observations','cup_sessions','lab_experiments'])
   await deny('select * from public.'+table,'Library table inaccessible: '+table);
 await db.exec(`update public.profiles set is_private=false;
 insert into public.shares(id,entity_type,entity_id,from_name,from_handle,name,payload_snapshot_json) values('${share}','recipe',gen_random_uuid(),'Spoof','spoof','Receta','${payload}');`);
 await ok(`select from_name,from_handle,original_author_user_id from public.shares where id='${share}'`,[{from_name:'Ana',from_handle:'ana',original_author_user_id:A}],'Server derives publisher and creator from account');
 await ok(`select count(*)::int as n from public.community_creations`,[{n:1}],'Only explicit publication registers a creation');
 await deny(`insert into public.shares(entity_type,entity_id,from_name,from_handle,name,payload_snapshot_json,original_author_user_id) values('recipe',gen_random_uuid(),'Ana','ana','Forged','${payload}','${B}')`,'Cannot forge another original creator');
 await deny(`insert into public.shares(entity_type,entity_id,from_name,from_handle,name,payload_snapshot_json) values('technique',gen_random_uuid(),'Ana','ana','Empty','{}')`,'Incomplete snapshot cannot be published');
 await as(B);
 await db.exec(`insert into public.share_likes(share_id) values('${share}') on conflict do nothing;insert into public.share_likes(share_id) values('${share}') on conflict do nothing;`);
 await ok(`select like_count::int as n from public.shares where id='${share}'`,[{n:1}],'Idempotent likes with visible total');
 await deny(`update public.shares set like_count=900 where id='${share}'`,'Client cannot forge like count');
 await db.exec(`insert into public.shares(id,entity_type,entity_id,from_name,from_handle,name,payload_snapshot_json,visibility,target_user_id,source_share_id) values('${copy}','recipe',gen_random_uuid(),'Beto','beto','Variante','${payload}','direct','${A}','${share}')`);
 await ok(`select original_author_user_id,original_author_name,from_user_id from public.shares where id='${copy}'`,[{original_author_user_id:A,original_author_name:'Ana',from_user_id:B}],'Republished copy preserves registered creator and new publisher');
 await ok('select count(*)::int as n from public.community_creations',[{n:1}],'Copy shares creator registration, not private library storage');
 await as(A);
 await ok(`select count(*)::int as n from public.share_likes`,[{n:0}],'Like counts do not expose liker identities');
 await db.exec('update public.profiles set is_private=true');
 await as(B); await db.exec(`delete from public.share_likes where share_id='${share}'`);
 await db.exec('reset role');
 await ok(`select like_count::int as n from public.shares where id='${share}'`,[{n:0}],'Unlike still works after author turns private');
 await ok(`select count(*)::int as n from public.recipes`,[{n:0}],'Publishing creates no private recipe mirror');
 await ok(`select count(*)::int as n from pg_tables where schemaname='public' and not rowsecurity`,[{n:0}],'All community tables protected by RLS');
 await as(A); await db.exec('update public.profiles set is_private=false');
 const tech=JSON.stringify({name:'V60',methodName:'V60',doseG:15,waterMl:240,ratio:16,temperatureC:92,totalTimeSeconds:180,steps:[{step_order:1,title:'Verter',duration_sec:180,water_add_ml:240,target_water_ml:240}]});
 await db.exec(`insert into public.shares(entity_type,entity_id,from_name,from_handle,name,payload_snapshot_json) values('technique',gen_random_uuid(),'Ana','ana','V60','${tech}')`);
 await ok("select count(*)::int as n from public.shares where entity_type='technique'",[{n:1}],'Technique snapshot accepted without private technique row');
 const broken=tech.replace('"target_water_ml":240','"target_water_ml":230');
 await deny(`insert into public.shares(entity_type,entity_id,from_name,from_handle,name,payload_snapshot_json) values('technique',gen_random_uuid(),'Ana','ana','V60','${broken}')`,'Contradictory cumulative water rejected');
 const conflict=tech.replace('"duration_sec":180','"duration_sec":180,"durationSeconds":200');
 await deny(`insert into public.shares(entity_type,entity_id,from_name,from_handle,name,payload_snapshot_json) values('technique',gen_random_uuid(),'Ana','ana','Conflict','${conflict}')`,'Aliases cannot produce different iOS and Android quantities');
 const badType=payload.replace('"amount":15','"amount":"NaN"');
 await deny(`insert into public.shares(entity_type,entity_id,from_name,from_handle,name,payload_snapshot_json) values('recipe',gen_random_uuid(),'Ana','ana','Type','${badType}')`,'Non-numeric ingredient rejected before native decoding');
 const huge=JSON.stringify({name:'Huge',notes:'x'.repeat(66000),steps:[{instruction:'Preparar'}],ingredients:[{name:'Café',amount:15,unit:'g'}]});
 await deny(`insert into public.shares(entity_type,entity_id,from_name,from_handle,name,payload_snapshot_json) values('recipe',gen_random_uuid(),'Ana','ana','Huge','${huge}')`,'64 KiB snapshot ceiling');
 for(let i=0;i<28;i++) await db.exec(`insert into public.shares(entity_type,entity_id,from_name,from_handle,name,payload_snapshot_json) values('recipe',gen_random_uuid(),'Ana','ana','Receta','${payload}')`);
 await deny(`insert into public.shares(entity_type,entity_id,from_name,from_handle,name,payload_snapshot_json) values('recipe',gen_random_uuid(),'Ana','ana','Too many','${payload}')`,'Thirty publications per hour enforced');
 await db.exec('reset role');
 await ok("select count(*)::int as n from public.techniques",[{n:0}],'Sharing a technique never backs up a library');
 await db.exec('rollback');console.log(`PASS ${count} local-first checks`);
}
