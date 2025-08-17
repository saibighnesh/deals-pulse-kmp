-- DealsPulse Database Schema
-- Run this in your Supabase SQL editor

-- Enable necessary extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Create profiles table
CREATE TABLE IF NOT EXISTS profiles (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    account_type TEXT NOT NULL CHECK (account_type IN ('USER', 'VENDOR')),
    business_name TEXT,
    logo_url TEXT,
    address TEXT,
    lat DOUBLE PRECISION,
    lng DOUBLE PRECISION,
    phone TEXT,
    is_verified BOOLEAN DEFAULT FALSE,
    radius_miles INTEGER DEFAULT 10,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create deals table
CREATE TABLE IF NOT EXISTS deals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    vendor_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    category TEXT NOT NULL CHECK (category IN ('FOOD', 'SALON', 'FITNESS', 'RETAIL', 'ENTERTAINMENT', 'SERVICES', 'OTHER')),
    price DECIMAL(10,2) NOT NULL,
    original_price DECIMAL(10,2),
    image_url TEXT,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    geohash TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACTIVE', 'REJECTED', 'ENDED')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_promoted BOOLEAN DEFAULT FALSE
);

-- Create reports table
CREATE TABLE IF NOT EXISTS reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    post_id UUID NOT NULL REFERENCES deals(id) ON DELETE CASCADE,
    reporter_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    reason TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_deals_expires_at ON deals(expires_at);
CREATE INDEX IF NOT EXISTS idx_deals_vendor_expires ON deals(vendor_id, expires_at);
CREATE INDEX IF NOT EXISTS idx_deals_geohash_expires ON deals(geohash, expires_at);
CREATE INDEX IF NOT EXISTS idx_deals_status_expires ON deals(status, expires_at);
CREATE INDEX IF NOT EXISTS idx_deals_category_status ON deals(category, status);
CREATE INDEX IF NOT EXISTS idx_deals_location ON deals USING GIST (ll_to_earth(lat, lng));

CREATE INDEX IF NOT EXISTS idx_profiles_account_type ON profiles(account_type);
CREATE INDEX IF NOT EXISTS idx_profiles_business_name ON profiles USING GIN (business_name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_profiles_verified ON profiles(account_type, is_verified);

CREATE INDEX IF NOT EXISTS idx_reports_post_id ON reports(post_id);
CREATE INDEX IF NOT EXISTS idx_reports_reporter ON reports(reporter_id);

-- Create updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at
CREATE TRIGGER update_profiles_updated_at BEFORE UPDATE ON profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create function to auto-expire deals
CREATE OR REPLACE FUNCTION expire_deals()
RETURNS void AS $$
BEGIN
    UPDATE deals 
    SET status = 'ENDED' 
    WHERE status = 'ACTIVE' AND expires_at < NOW();
END;
$$ LANGUAGE plpgsql;

-- Create a cron job to expire deals (runs every minute)
SELECT cron.schedule('expire-deals', '* * * * *', 'SELECT expire_deals();');

-- Enable Row Level Security
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE deals ENABLE ROW LEVEL SECURITY;
ALTER TABLE reports ENABLE ROW LEVEL SECURITY;

-- RLS Policies for profiles
CREATE POLICY "Public read access to profiles" ON profiles
    FOR SELECT USING (true);

CREATE POLICY "Users can update own profile" ON profiles
    FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own profile" ON profiles
    FOR INSERT WITH CHECK (auth.uid() = user_id);

-- RLS Policies for deals
CREATE POLICY "Public read access to active deals" ON deals
    FOR SELECT USING (status = 'ACTIVE' AND expires_at > NOW());

CREATE POLICY "Vendors can create deals" ON deals
    FOR INSERT WITH CHECK (
        auth.uid() = vendor_id AND 
        EXISTS (
            SELECT 1 FROM profiles 
            WHERE user_id = auth.uid() AND account_type = 'VENDOR'
        )
    );

CREATE POLICY "Vendors can update own deals" ON deals
    FOR UPDATE USING (auth.uid() = vendor_id);

CREATE POLICY "Vendors can delete own deals" ON deals
    FOR DELETE USING (auth.uid() = vendor_id);

-- RLS Policies for reports
CREATE POLICY "Users can create reports" ON reports
    FOR INSERT WITH CHECK (auth.uid() = reporter_id);

CREATE POLICY "No public read access to reports" ON reports
    FOR SELECT USING (false);

-- Create storage bucket for deal images
INSERT INTO storage.buckets (id, name, public) 
VALUES ('deal-images', 'deal-images', true)
ON CONFLICT (id) DO NOTHING;

-- Storage policies for deal-images bucket
CREATE POLICY "Public read access to deal images" ON storage.objects
    FOR SELECT USING (bucket_id = 'deal-images');

CREATE POLICY "Authenticated users can upload images" ON storage.objects
    FOR INSERT WITH CHECK (
        bucket_id = 'deal-images' AND 
        auth.role() = 'authenticated'
    );

CREATE POLICY "Users can update own images" ON storage.objects
    FOR UPDATE USING (
        bucket_id = 'deal-images' AND 
        auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "Users can delete own images" ON storage.objects
    FOR DELETE USING (
        bucket_id = 'deal-images' AND 
        auth.uid()::text = (storage.foldername(name))[1]
    );

-- Create function to generate geohash
CREATE OR REPLACE FUNCTION generate_geohash(lat DOUBLE PRECISION, lng DOUBLE PRECISION, precision INTEGER DEFAULT 6)
RETURNS TEXT AS $$
DECLARE
    base32 TEXT := '0123456789bcdefghjkmnpqrstuvwxyz';
    geohash TEXT := '';
    bit INTEGER := 0;
    ch INTEGER := 0;
    lat_min DOUBLE PRECISION := -90.0;
    lat_max DOUBLE PRECISION := 90.0;
    lng_min DOUBLE PRECISION := -180.0;
    lng_max DOUBLE PRECISION := 180.0;
BEGIN
    WHILE length(geohash) < precision LOOP
        IF bit % 2 = 0 THEN
            DECLARE
                lng_mid DOUBLE PRECISION := (lng_min + lng_max) / 2;
            BEGIN
                IF lng >= lng_mid THEN
                    ch := ch | (1 << (4 - bit % 5));
                    lng_min := lng_mid;
                ELSE
                    lng_max := lng_mid;
                END IF;
            END;
        ELSE
            DECLARE
                lat_mid DOUBLE PRECISION := (lat_min + lat_max) / 2;
            BEGIN
                IF lat >= lat_mid THEN
                    ch := ch | (1 << (4 - bit % 5));
                    lat_min := lat_mid;
                ELSE
                    lat_max := lat_mid;
                END IF;
            END;
        END IF;
        
        bit := bit + 1;
        IF bit % 5 = 0 THEN
            geohash := geohash || substr(base32, ch + 1, 1);
            ch := 0;
        END IF;
    END LOOP;
    
    RETURN geohash;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to auto-generate geohash
CREATE OR REPLACE FUNCTION generate_deal_geohash()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.lat IS NOT NULL AND NEW.lng IS NOT NULL THEN
        NEW.geohash := generate_geohash(NEW.lat, NEW.lng);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_generate_deal_geohash
    BEFORE INSERT OR UPDATE ON deals
    FOR EACH ROW
    EXECUTE FUNCTION generate_deal_geohash();

-- Create function to calculate distance between two points
CREATE OR REPLACE FUNCTION calculate_distance(
    lat1 DOUBLE PRECISION, 
    lng1 DOUBLE PRECISION, 
    lat2 DOUBLE PRECISION, 
    lng2 DOUBLE PRECISION
)
RETURNS DOUBLE PRECISION AS $$
BEGIN
    RETURN (
        3959 * acos(
            cos(radians(lat1)) * 
            cos(radians(lat2)) * 
            cos(radians(lng2) - radians(lng1)) + 
            sin(radians(lat1)) * 
            sin(radians(lat2))
        )
    );
END;
$$ LANGUAGE plpgsql;

-- Create view for deals with vendor info
CREATE OR REPLACE VIEW deals_with_vendors AS
SELECT 
    d.*,
    p.user_id as vendor_user_id,
    p.account_type,
    p.business_name,
    p.logo_url as vendor_logo_url,
    p.address as vendor_address,
    p.lat as vendor_lat,
    p.lng as vendor_lng,
    p.phone as vendor_phone,
    p.is_verified as vendor_verified
FROM deals d
JOIN profiles p ON d.vendor_id = p.user_id
WHERE d.status = 'ACTIVE' AND d.expires_at > NOW();

-- Grant necessary permissions
GRANT USAGE ON SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL TABLES IN SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO anon, authenticated;