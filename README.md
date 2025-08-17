# DealsPulse

A **real-time, hyper-local flash deals app** built with Kotlin Multiplatform and Compose Multiplatform.

## 🚀 What We're Building

**DealsPulse** connects local vendors with nearby shoppers through time-sensitive offers:

- **Vendors** post short-lived deals (minutes/hours) to drive same-day foot traffic
- **Shoppers** discover "what's hot near me right now" with urgency and clear value
- **Real-time updates** via Supabase Realtime for instant deal visibility
- **Location-based filtering** with configurable radius (5/10/20/50 miles)
- **Category filtering** (Food, Salon, Fitness, Retail, etc.)

## 🏗️ Architecture

### Tech Stack
- **Frontend**: Kotlin Multiplatform + Compose Multiplatform (Android + iOS)
- **Backend**: Supabase (PostgreSQL + Auth + Realtime + Storage)
- **Location**: Fused Location Provider (Android) + CoreLocation (iOS)
- **State Management**: MVVM with ViewModels
- **Navigation**: Voyager (planned)

### Project Structure
```
composeApp/
├── src/
│   ├── commonMain/           # Shared Kotlin code
│   │   ├── config/          # App configuration
│   │   ├── model/           # Data models (Deal, Profile, Report)
│   │   ├── util/            # Utilities (GeoHash, TimeUtils)
│   │   ├── data/            # API layer (Supabase clients)
│   │   ├── location/        # Location provider interface
│   │   └── presentation/    # UI components and screens
│   ├── androidMain/         # Android-specific implementations
│   └── iosMain/            # iOS-specific implementations
```

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- Kotlin 1.9.20+
- JDK 17+
- Supabase account

### 1. Clone and Setup
```bash
git clone <your-repo>
cd DealsPulse
```

### 2. Configure Supabase
1. Create a new project at [supabase.com](https://supabase.com)
2. Get your project URL and anon key
3. Update `composeApp/src/commonMain/kotlin/com/dealspulse/app/config/AppConfig.kt`:
   ```kotlin
   actual val supabaseUrl: String = "https://your-project.supabase.co"
   actual val supabaseAnonKey: String = "your-anon-key"
   ```

### 3. Database Setup
Run this SQL in your Supabase SQL editor:

```sql
-- Enable necessary extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create profiles table
CREATE TABLE profiles (
    user_id UUID REFERENCES auth.users(id) PRIMARY KEY,
    account_type TEXT NOT NULL CHECK (account_type IN ('user', 'vendor')),
    business_name TEXT,
    logo_url TEXT,
    address TEXT,
    lat DOUBLE PRECISION,
    lng DOUBLE PRECISION,
    phone TEXT,
    is_verified BOOLEAN DEFAULT false,
    radius_miles INTEGER DEFAULT 10,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create deals table
CREATE TABLE deals (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    vendor_id UUID REFERENCES auth.users(id) NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    category TEXT NOT NULL CHECK (category IN ('food', 'salon', 'fitness', 'retail', 'entertainment', 'services', 'other')),
    price DECIMAL(10,2) NOT NULL,
    original_price DECIMAL(10,2),
    image_url TEXT,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    geohash TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'active', 'rejected', 'ended')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_promoted BOOLEAN DEFAULT false
);

-- Create reports table
CREATE TABLE reports (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    post_id UUID REFERENCES deals(id) NOT NULL,
    reporter_id UUID REFERENCES auth.users(id) NOT NULL,
    reason TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX idx_deals_expires_at ON deals(expires_at);
CREATE INDEX idx_deals_vendor_expires ON deals(vendor_id, expires_at);
CREATE INDEX idx_deals_geohash_expires ON deals(geohash, expires_at);
CREATE INDEX idx_deals_status ON deals(status);
CREATE INDEX idx_profiles_account_type ON profiles(account_type);

-- Enable Row Level Security
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE deals ENABLE ROW LEVEL SECURITY;
ALTER TABLE reports ENABLE ROW LEVEL SECURITY;

-- RLS Policies
-- Profiles: public read, owner write
CREATE POLICY "Profiles are viewable by everyone" ON profiles FOR SELECT USING (true);
CREATE POLICY "Users can insert their own profile" ON profiles FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can update their own profile" ON profiles FOR UPDATE USING (auth.uid() = user_id);

-- Deals: public read, owner write
CREATE POLICY "Deals are viewable by everyone" ON deals FOR SELECT USING (true);
CREATE POLICY "Vendors can insert their own deals" ON deals FOR INSERT WITH CHECK (auth.uid() = vendor_id);
CREATE POLICY "Vendors can update their own deals" ON deals FOR UPDATE USING (auth.uid() = vendor_id);
CREATE POLICY "Vendors can delete their own deals" ON deals FOR DELETE USING (auth.uid() = vendor_id);

-- Reports: authenticated users can insert, no public read
CREATE POLICY "Authenticated users can report deals" ON reports FOR INSERT WITH CHECK (auth.role() = 'authenticated');
CREATE POLICY "Reports are not publicly viewable" ON reports FOR SELECT USING (false);

-- Create storage bucket for deal images
INSERT INTO storage.buckets (id, name, public) VALUES ('deal-images', 'deal-images', true);

-- Storage policies
CREATE POLICY "Images are publicly accessible" ON storage.objects FOR SELECT USING (bucket_id = 'deal-images');
CREATE POLICY "Authenticated users can upload images" ON storage.objects FOR INSERT WITH CHECK (bucket_id = 'deal-images' AND auth.role() = 'authenticated');
CREATE POLICY "Users can update their own images" ON storage.objects FOR UPDATE USING (bucket_id = 'deal-images' AND auth.uid()::text = (storage.foldername(name))[1]);
CREATE POLICY "Users can delete their own images" ON storage.objects FOR DELETE USING (bucket_id = 'deal-images' AND auth.uid()::text = (storage.foldername(name))[1]);
```

### 4. Build and Run
```bash
# Android
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:installDebug

# iOS (requires Xcode)
./gradlew :composeApp:iosSimulatorArm64Test
```

## 📱 Key Features

### Feed Screen
- **Location-based filtering** with radius selection
- **Category filtering** for targeted browsing
- **Real-time updates** via Supabase Realtime
- **Pull-to-refresh** for manual updates
- **Deal cards** with urgency indicators and countdown timers

### Deal Management
- **Vendor posting flow** with image upload
- **Status management** (pending/active/rejected/ended)
- **Automatic expiration** handling
- **Geolocation** with geohash indexing

### User Experience
- **Urgency indicators** (color-coded countdown badges)
- **Distance calculations** using Haversine formula
- **Offline-ready** architecture (planned with SQLDelight)
- **Push notifications** (planned with FCM/APNs)

## 🔧 Development

### Adding New Features
1. **Models**: Add to `commonMain/model/`
2. **APIs**: Extend classes in `commonMain/data/`
3. **UI**: Create composables in `commonMain/presentation/`
4. **Platform-specific**: Implement in `androidMain/` or `iosMain/`

### Testing
```bash
# Run tests
./gradlew test
./gradlew :composeApp:androidTest
```

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable names
- Add KDoc comments for public APIs
- Keep functions focused and small

## 🚀 Deployment

### Android
1. Update version in `build.gradle.kts`
2. Build release APK: `./gradlew :composeApp:assembleRelease`
3. Upload to Google Play Console

### iOS
1. Update version in Xcode project
2. Archive and upload via Xcode
3. Submit for App Store review

## 📊 Analytics & Monitoring

### Metrics to Track
- **User engagement**: Deal views, saves, shares
- **Vendor success**: Deal creation, redemption rates
- **App performance**: Load times, crash rates
- **Business metrics**: User retention, growth

### Tools (Future)
- Firebase Analytics
- Crashlytics
- Performance monitoring

## 💰 Monetization Strategy

### Phase 1 (MVP)
- Free for vendors and shoppers
- Focus on user acquisition and engagement

### Phase 2 (Growth)
- **Premium vendor features**: Promoted deals, analytics dashboard
- **Subscription tiers**: Basic (free) vs Premium ($9.99/month)
- **Pay-per-push**: $0.99 per promotional notification

### Phase 3 (Scale)
- **Enterprise features**: Multi-location management, advanced analytics
- **White-label solutions**: Custom branded apps for franchises
- **API access**: Third-party integrations

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

- **Issues**: Create GitHub issues for bugs or feature requests
- **Discussions**: Use GitHub Discussions for questions
- **Documentation**: Check the [docs/](docs/) folder

---

**Built with ❤️ using Kotlin Multiplatform and Compose Multiplatform**