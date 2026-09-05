import { PGlite } from '@electric-sql/pglite';
import { readFile, readdir } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const toolsDirectory = path.dirname(fileURLToPath(import.meta.url));
const repo = path.dirname(toolsDirectory);
const android = process.argv[2];
if (!android) throw new Error('Uso: npm run verify-backend -- /ruta/001_brew_studio_schema.sql');
const migrationsDirectory = path.join(repo, 'supabase/migrations');
const migrationNames = (await readdir(migrationsDirectory)).filter(name => name.endsWith('.sql')).sort();
const userId = '11111111-1111-4111-8111-111111111111';
const otherUserId = '22222222-2222-4222-8222-222222222222';

async function bootstrap(db) {
  await db.exec(`
    create role authenticated;
    create role anon;
    create schema auth;
    create table auth.users (id uuid primary key, email text not null);
    create function auth.uid() returns uuid language sql stable as $$
      select coalesce(nullif(current_setting('request.jwt.claim.sub', true), ''), '${userId}')::uuid
    $$;
    create function public.uuid_generate_v4() returns uuid language sql volatile as $$ select gen_random_uuid() $$;
  `);
}

async function apply(db, file) {
  try {
    const sql = (await readFile(file, 'utf8'))
      .replace(/create extension if not exists "uuid-ossp";/gi, '')
      .replace(/create extension if not exists pgcrypto;/gi, '');
    await db.exec(sql);
    process.stdout.write(`OK ${path.basename(file)}\n`);
  } catch (error) {
    throw new Error(`${path.basename(file)}: ${error.message}`, { cause: error });
  }
}

async function applyIOSMigrations(db, excludedSuffix = '') {
  for (const name of migrationNames.filter(name => !excludedSuffix || !name.endsWith(excludedSuffix))) {
    await apply(db, path.join(migrationsDirectory, name));
  }
}

async function verifyRLS(db) {
  await db.exec(`
    insert into auth.users(id,email) values ('${otherUserId}','otra@example.com');
    update public.profiles set display_name='Ana',handle='ana',alias='ana',is_private=false where id='${userId}';
    update public.profiles set display_name='Otra',handle='otra.cafe',alias='otra.cafe',is_private=true where id='${otherUserId}';
    insert into public.beans(user_id,name,stock_grams) values ('${otherUserId}','Privado B',100);
    grant usage on schema public,auth to authenticated;
    grant select,insert,update,delete on all tables in schema public to authenticated;
    grant execute on all functions in schema public,auth to authenticated;
    select set_config('request.jwt.claim.sub','${userId}',false);
    set role authenticated;
  `);
  const visible = await db.query(`select user_id,name from public.beans order by name`);
  if (visible.rows.length !== 1 || visible.rows[0].user_id !== userId) throw new Error('RLS permitió leer café privado de otra cuenta');
  const modified = await db.query(`update public.beans set name='Intruso' where user_id='${otherUserId}' returning id`);
  if (modified.rows.length !== 0) throw new Error('RLS permitió modificar café privado de otra cuenta');
  const recipient = await db.query(`select * from public.resolve_profile_alias('@OTRA.CAFE')`);
  if (recipient.rows.length !== 1 || recipient.rows[0].user_id !== otherUserId || recipient.rows[0].alias !== 'otra.cafe') throw new Error('La resolución exacta por alias no encontró al destinatario');
  const self = await db.query(`select * from public.resolve_profile_alias('ana')`);
  if (self.rows.length !== 0) throw new Error('La resolución por alias permitió enviarse a la misma cuenta');
  await db.exec('reset role');
}

async function androidUpgradeScenario() {
  const db = new PGlite(); await db.waitReady; await bootstrap(db); await apply(db, android);
  await db.exec(`
    insert into auth.users(id,email) values ('${userId}','ana@example.com');
    insert into public.beans(id,user_id,roaster,name,origin,altitude,process,roast_date,first_use_date,notes,status,stock_grams)
      values ('20000000-0000-4000-8000-000000000001','${userId}','Casa','Chiapas','México','1500','Lavado','2026-08-01','','Cacao','cerrado',200);
    insert into public.grinders(id,user_id,brand,model,click_range) values ('30000000-0000-4000-8000-000000000001','${userId}','Comandante','C40','10 - 30');
    insert into public.equipment(id,user_id,name,type) values ('40000000-0000-4000-8000-000000000001','${userId}','V60','método');
    insert into public.recipes(id,user_id,owner_user_id,name,method,notes) values ('50000000-0000-4000-8000-000000000001','${userId}','${userId}','Dulce','V60','Balance');
    insert into public.techniques(id,user_id,owner_user_id,name,method,coffee_grams,water_ml,ratio,temperature,grind_clicks)
      values ('60000000-0000-4000-8000-000000000001','${userId}','${userId}','Pulsos','V60',15,240,16,92,'22 clicks');
    insert into public.technique_steps(id,technique_id,user_id,step_order,title,duration_sec,water_add_ml,target_water_ml)
      values ('70000000-0000-4000-8000-000000000001','60000000-0000-4000-8000-000000000001','${userId}',1,'Bloom',45,50,50);
    insert into public.lab_experiments(id,user_id,method_name,coffee_grams,water_ml,ratio,temperature,clicks,bean_freshness,estimated_time_seconds,experiment_notes,timestamp)
      values ('80000000-0000-4000-8000-000000000001','${userId}','V60',15,240,16,92,22,'fresco',180,'Prueba',1);
  `);
  await applyIOSMigrations(db);
  await verifyRLS(db);

  const bean = (await db.query(`select name, remaining_quantity_grams from public.coffee_beans where id='20000000-0000-4000-8000-000000000001'`)).rows[0];
  if (bean?.name !== 'Chiapas' || Number(bean?.remaining_quantity_grams) !== 200) throw new Error('El espejo del café Android no coincide');
  await db.exec(`update public.grinders set click_range='12 - 28' where id='30000000-0000-4000-8000-000000000001'`);
  let pair = (await db.query(`select minimum_setting,maximum_setting from public.grinders where id='30000000-0000-4000-8000-000000000001'`)).rows[0];
  if (pair.minimum_setting !== 12 || pair.maximum_setting !== 28) throw new Error('Molino Android→iOS no propagó');
  await db.exec(`update public.grinders set minimum_setting=14,maximum_setting=26 where id='30000000-0000-4000-8000-000000000001'`);
  pair = (await db.query(`select click_range from public.grinders where id='30000000-0000-4000-8000-000000000001'`)).rows[0];
  if (pair.click_range !== '14 - 26') throw new Error('Molino iOS→Android no propagó');
  await db.exec(`update public.recipes set method='AeroPress' where id='50000000-0000-4000-8000-000000000001'`);
  pair = (await db.query(`select suggested_method_name from public.recipes where id='50000000-0000-4000-8000-000000000001'`)).rows[0];
  if (pair.suggested_method_name !== 'AeroPress') throw new Error('Receta Android→iOS no propagó');
  await db.exec(`update public.recipes set is_shared=true,original_author_user_id='${otherUserId}',original_author_name='Autora',original_entity_id='50000000-0000-4000-8000-000000000099',root_entity_id='50000000-0000-4000-8000-000000000099',copy_mode='fork' where id='50000000-0000-4000-8000-000000000001'`);
  pair = (await db.query(`select is_shared,original_author_user_id,original_author_name,original_entity_id,root_entity_id,copy_mode from public.recipes where id='50000000-0000-4000-8000-000000000001'`)).rows[0];
  if (!pair.is_shared || pair.original_author_user_id !== otherUserId || pair.original_author_name !== 'Autora' || pair.original_entity_id !== '50000000-0000-4000-8000-000000000099' || pair.root_entity_id !== '50000000-0000-4000-8000-000000000099' || pair.copy_mode !== 'fork') throw new Error('La atribución de receta no se conservó');
  await db.exec(`update public.techniques set dose_grams=18,temperature_c=94 where id='60000000-0000-4000-8000-000000000001'`);
  pair = (await db.query(`select coffee_grams,temperature from public.techniques where id='60000000-0000-4000-8000-000000000001'`)).rows[0];
  if (Number(pair.coffee_grams) !== 18 || pair.temperature !== 94) throw new Error('Técnica iOS→Android no propagó');
  await db.exec(`update public.technique_steps set duration_sec=60,water_add_ml=70 where id='70000000-0000-4000-8000-000000000001'`);
  pair = (await db.query(`select duration_seconds,water_added_ml from public.technique_steps where id='70000000-0000-4000-8000-000000000001'`)).rows[0];
  if (pair.duration_seconds !== 60 || pair.water_added_ml !== 70) throw new Error('Pasos Android→iOS no propagaron');
  await db.close();
}

async function iosUpgradeScenario() {
  const db = new PGlite(); await db.waitReady; await bootstrap(db);
  await applyIOSMigrations(db, '007_android_backend_alignment.sql');
  await db.exec(`
    insert into auth.users(id,email) values ('${userId}','ana@example.com');
    insert into public.coffee_beans(id,owner_id,name,brand,remaining_quantity_grams,initial_quantity_grams)
      values ('90000000-0000-4000-8000-000000000001','${userId}','Oaxaca','Casa',180,250);
  `);
  await apply(db, path.join(migrationsDirectory, '202609010007_android_backend_alignment.sql'));
  const bean = (await db.query(`select user_id,name,stock_grams from public.beans where id='90000000-0000-4000-8000-000000000001'`)).rows[0];
  if (bean?.name !== 'Oaxaca' || Number(bean?.stock_grams) !== 180 || bean?.user_id !== userId) throw new Error('El café iOS anterior no se conservó');
  await db.close();
}

await androidUpgradeScenario();
await iosUpgradeScenario();
process.stdout.write('PostgreSQL efímero: Android→iOS, iOS→Android, conservación previa y RLS A/B aprobados\n');
