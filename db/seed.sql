-- =========================================
-- DealsPulse - Day 1 Database Seed (FINAL)
-- Safe to re-run. Creates schema, RLS, policies, trigger, storage policies, realtime.
-- =========================================

-- 0) Extensions
create extension if not exists pgcrypto;

-- 1) Tables
create table if not exists public.profiles(
  user_id uuid primary key references auth.users(id) on delete cascade,
  account_type text check (account_type in ('user','vendor')) default 'user',
  business_name text,
  logo_url text,
  address text,
  lat double precision,
  lng double precision,
  phone text,
  is_verified boolean default false,
  radius_miles int default 10,
  created_at timestamptz default now()
);

create table if not exists public.deals(
  id uuid primary key default gen_random_uuid(),
  vendor_id uuid not null references auth.users(id) on delete cascade,
  title text not null,
  description text,
  category text not null,
  price text,
  image_url text,
  lat double precision not null,
  lng double precision not null,
  geohash text not null,
  status text default 'active',   -- 'pending'|'active'|'rejected'|'ended'
  created_at timestamptz default now(),
  expires_at timestamptz not null,
  is_promoted boolean default false
);

create table if not exists public.reports(
  id uuid primary key default gen_random_uuid(),
  post_id uuid not null references public.deals(id) on delete cascade,
  reporter_id uuid not null references auth.users(id) on delete cascade,
  reason text,
  created_at timestamptz default now()
);

-- 1a) Defensive constraints (idempotent via DO blocks)
do $$ begin
  alter table public.deals
    add constraint deals_expires_future check (expires_at > created_at);
exception when duplicate_object then null; end $$;

do $$ begin
  alter table public.deals
    add constraint deals_lat_range check (lat between -90 and 90);
exception when duplicate_object then null; end $$;

do $$ begin
  alter table public.deals
    add constraint deals_lng_range check (lng between -180 and 180);
exception when duplicate_object then null; end $$;

-- 2) Indexes
create index if not exists deals_vendor_idx       on public.deals(vendor_id, expires_at);
create index if not exists deals_geo_idx          on public.deals(geohash, expires_at);
create index if not exists profiles_type_name_idx on public.profiles(account_type, business_name);
create index if not exists deals_expires_idx      on public.deals(expires_at);

-- 3) Enable RLS
alter table public.profiles enable row level security;
alter table public.deals enable row level security;
alter table public.reports enable row level security;

-- 4) RLS Policies (DROP + CREATE; split INSERT/UPDATE where needed)

-- --------- PROFILES ----------
-- Read
drop policy if exists read_profiles on public.profiles;
create policy read_profiles on public.profiles
  for select using (true);

-- Insert own profile
drop policy if exists write_own_profile_insert on public.profiles;
create policy write_own_profile_insert on public.profiles
  for insert
  with check (auth.uid() = user_id);

-- Update own profile
drop policy if exists write_own_profile_update on public.profiles;
create policy write_own_profile_update on public.profiles
  for update
  using (auth.uid() = user_id);

-- --------- DEALS ----------
-- Read
drop policy if exists read_deals on public.deals;
create policy read_deals on public.deals
  for select using (true);

-- Insert own deal (vendor only)
drop policy if exists insert_deal_insert on public.deals;
create policy insert_deal_insert on public.deals
  for insert
  with check (auth.uid() = vendor_id);

-- Update own deal
drop policy if exists update_own_deal_update on public.deals;
create policy update_own_deal_update on public.deals
  for update
  using (auth.uid() = vendor_id);

-- --------- REPORTS ----------
-- Keep reports non-public
drop policy if exists read_reports on public.reports;
create policy read_reports on public.reports
  for select using (false);

-- Insert report (authenticated reporter)
drop policy if exists insert_report_insert on public.reports;
create policy insert_report_insert on public.reports
  for insert
  with check (auth.uid() = reporter_id);

-- 5) Auto-create profile row for new auth users
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
as $$
begin
  insert into public.profiles (user_id)
  values (new.id)
  on conflict (user_id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_user();

-- 6) Storage policies for bucket 'deal-images'
-- (Create the bucket in UI: Storage → Create bucket → name 'deal-images', Public ON)
drop policy if exists "deal-images public read" on storage.objects;
create policy "deal-images public read" on storage.objects
  for select using (bucket_id = 'deal-images');

drop policy if exists "deal-images auth upload" on storage.objects;
create policy "deal-images auth upload" on storage.objects
  for insert to authenticated
  with check (bucket_id = 'deal-images');

drop policy if exists "deal-images owner update" on storage.objects;
create policy "deal-images owner update" on storage.objects
  for update to authenticated
  using (bucket_id = 'deal-images' and owner = auth.uid())
  with check (bucket_id = 'deal-images' and owner = auth.uid());

drop policy if exists "deal-images owner delete" on storage.objects;
create policy "deal-images owner delete" on storage.objects
  for delete to authenticated
  using (bucket_id = 'deal-images' and owner = auth.uid());

-- 7) Enable realtime on public.deals (idempotent)
do $$
begin
  begin
    alter publication supabase_realtime add table public.deals;
  exception when duplicate_object then
    null;
  end;
end $$;

-- 8) Optional dev helpers (commented)
-- -- Promote yourself to vendor after first login:
-- update public.profiles set account_type='vendor', business_name='Debug Bakery'
-- where user_id = 'YOUR-UUID';
--
-- -- Insert a sample deal:
-- insert into public.deals(vendor_id, title, category, price, lat, lng, geohash, expires_at)
-- values ('YOUR-UUID','Flash: $1 croissant','Food','$1',33.7488,-84.3877,'dnhq9', now() + interval '45 minutes');
