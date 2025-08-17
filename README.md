# DealsPulse

A **real-time, hyper-local flash deals app** built with Kotlin Multiplatform and Compose Multiplatform.

**Vendors** post short-lived offers (minutes/hours). **Shoppers** nearby instantly see and redeem them.

## Features

### Core Functionality
- 📍 **Location-based deal discovery** - Find deals within customizable radius (5/10/20/50 miles)
- ⏰ **Real-time updates** - New deals appear instantly without refresh
- 🏷️ **Category filtering** - Food, Beauty, Fitness, Retail, Services, Entertainment
- ⚡ **Urgency indicators** - Color-coded countdown timers show deal expiration
- 🔔 **Live notifications** - Get notified when deals appear nearby (planned)

### For Shoppers
- Browse deals sorted by **soonest expiring** and **closest distance**
- Real-time feed with automatic updates via Supabase Realtime
- Category filters and radius selection
- Deal details with vendor information and countdown timers

### For Vendors
- Post flash deals with title, description, price, category, and photo
- Set custom expiration times (minutes to hours)
- Automatic location detection
- Real-time deal management

## Tech Stack

### Client (Kotlin Multiplatform + Compose)
- **UI:** Compose Multiplatform (Android + iOS)
- **Language:** Kotlin 2.x with Coroutines
- **Navigation:** Voyager Navigator
- **State Management:** Voyager ScreenModel + StateFlow
- **Serialization:** kotlinx.serialization
- **Location:** 
  - Android: Fused Location Provider (Google Play Services)
  - iOS: CoreLocation (expect/actual pattern)
- **Geospatial:** GeoHash for efficient location queries + Haversine distance

### Backend (Supabase - $0 cost)
- **Database:** PostgreSQL with Row Level Security (RLS)
- **Authentication:** Email/password via Supabase Auth
- **API:** PostgREST (auto-generated from schema)
- **Real-time:** Supabase Realtime for live deal updates
- **Storage:** Supabase Storage for deal images
- **Security:** RLS policies for data protection

### Key Dependencies
```kotlin
// Core KMP
kotlin = "1.9.21"
compose-plugin = "1.5.11"
kotlinx-coroutines = "1.7.3"
kotlinx-serialization = "1.6.2"
kotlinx-datetime = "0.5.0"

// Backend
supabase = "2.1.4" // PostgREST, GoTrue, Realtime, Storage
ktor-client = "2.3.7"

// Navigation & State
voyager = "1.0.0"
androidx-lifecycle = "2.7.0"

// Location & Geo
geohash = "1.4.0"
play-services-location = "21.0.1" // Android only
```

## Project Structure

```
composeApp/src/
├── commonMain/kotlin/com/dealspulse/app/
│   ├── config/           # AppConfig expect (Supabase credentials)
│   ├── model/            # Data models: Deal, Profile, Report
│   ├── util/             # GeoHash, Haversine, TimeUtils
│   ├── data/             # API layer
│   │   ├── Supa.kt       # Supabase client singleton
│   │   ├── DealApi.kt    # Deal CRUD operations
│   │   ├── ProfileApi.kt # User/vendor management
│   │   ├── StorageApi.kt # Image upload handling
│   │   └── Realtime.kt   # Live updates subscription
│   ├── location/         # LocationProvider expect interface
│   └── presentation/     # UI screens
│       ├── feed/         # Deal browsing (FeedScreen + ScreenModel)
│       └── post/         # Deal creation (CreateDealScreen + ScreenModel)
├── androidMain/kotlin/
│   ├── config/           # Android AppConfig actual
│   ├── location/         # Android LocationProvider (Fused Location)
│   └── MainActivity.kt
└── iosMain/kotlin/
    ├── config/           # iOS AppConfig actual  
    └── location/         # iOS LocationProvider (CoreLocation)
```

## Database Schema

### profiles
```sql
- user_id (uuid, FK to auth.users)
- account_type ('user' | 'vendor')
- business_name, address, lat, lng, phone
- is_verified (boolean)
- radius_miles (default user preference)
```

### deals
```sql
- id (uuid), vendor_id (FK to auth.users)
- title, description, category, price, image_url
- lat, lng, geohash (for efficient geo queries)
- status ('pending' | 'active' | 'rejected' | 'ended')
- created_at, expires_at
- is_promoted (future monetization)
```

### Indexes & Performance
- `deals(expires_at)` - Sort by urgency
- `deals(geohash, expires_at)` - Location + time queries
- `deals(vendor_id, expires_at)` - Vendor's deals

## Real-time Architecture

1. **Supabase Realtime** subscription to `public.deals` table
2. **Location filtering** in client (subscribe to area within radius)  
3. **Automatic UI updates** on INSERT/UPDATE/DELETE
4. **Distance calculation** using Haversine formula
5. **Smart sorting** by expiration time + proximity

## Getting Started

### Prerequisites
- Android Studio with KMP plugin
- Xcode (for iOS development)
- Supabase account (free tier)

### Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd DealsPulse
   ```

2. **Configure Supabase**
   - Create a new Supabase project
   - Update `composeApp/src/*/kotlin/com/dealspulse/app/config/AppConfig.*.kt`:
     ```kotlin
     actual val supabaseUrl: String = "https://your-project.supabase.co"
     actual val supabaseKey: String = "your-anon-key"
     ```

3. **Set up database schema**
   ```sql
   -- Run in Supabase SQL Editor
   CREATE TABLE profiles (
     user_id uuid REFERENCES auth.users PRIMARY KEY,
     account_type text CHECK (account_type IN ('user', 'vendor')),
     business_name text,
     address text,
     lat double precision,
     lng double precision,
     phone text,
     is_verified boolean DEFAULT false,
     radius_miles integer DEFAULT 10
   );

   CREATE TABLE deals (
     id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
     vendor_id uuid REFERENCES auth.users,
     title text NOT NULL,
     description text NOT NULL,
     category text NOT NULL,
     price text NOT NULL,
     image_url text,
     lat double precision NOT NULL,
     lng double precision NOT NULL,
     geohash text NOT NULL,
     status text CHECK (status IN ('pending', 'active', 'rejected', 'ended')) DEFAULT 'active',
     created_at timestamptz DEFAULT now(),
     expires_at timestamptz NOT NULL,
     is_promoted boolean DEFAULT false
   );

   -- Indexes
   CREATE INDEX deals_expires_at_idx ON deals(expires_at);
   CREATE INDEX deals_geohash_expires_at_idx ON deals(geohash, expires_at);
   CREATE INDEX deals_vendor_expires_at_idx ON deals(vendor_id, expires_at);

   -- RLS Policies
   ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
   ALTER TABLE deals ENABLE ROW LEVEL SECURITY;

   -- Public read access
   CREATE POLICY "Public profiles are viewable by everyone" ON profiles FOR SELECT USING (true);
   CREATE POLICY "Public deals are viewable by everyone" ON deals FOR SELECT USING (true);

   -- Users can insert/update own records
   CREATE POLICY "Users can insert own profile" ON profiles FOR INSERT WITH CHECK (auth.uid() = user_id);
   CREATE POLICY "Users can update own profile" ON profiles FOR UPDATE USING (auth.uid() = user_id);
   CREATE POLICY "Users can insert own deals" ON deals FOR INSERT WITH CHECK (auth.uid() = vendor_id);
   CREATE POLICY "Users can update own deals" ON deals FOR UPDATE USING (auth.uid() = vendor_id);
   ```

4. **Set up Storage**
   - Create bucket `deal-images` in Supabase Storage
   - Set policies for public read, authenticated write

5. **Enable Realtime**
   ```sql
   ALTER PUBLICATION supabase_realtime ADD TABLE deals;
   ```

6. **Build and run**
   ```bash
   ./gradlew :composeApp:assembleDebug          # Android
   ./gradlew :composeApp:iosSimulatorArm64Test  # iOS Simulator
   ```

## Key Features Implementation

### Real-time Deal Updates
```kotlin
// Subscribe to deals within radius
realtimeApi.subscribeToDealsNearby(lat, lng, radiusMiles)
    .collect { change ->
        when (change) {
            is DealChange.Insert -> addDealToList(change.deal)
            is DealChange.Update -> updateDealInList(change.deal)  
            is DealChange.Delete -> removeDealFromList(change.dealId)
        }
    }
```

### Location-based Queries
```kotlin
// Efficient geo queries using geohash + exact distance filtering
val geoHashPrefixes = GeoUtils.getGeoHashPrefixes(lat, lng, radiusMiles)
val deals = supabase.from("deals")
    .select("*")
    .like("geohash", "${geoHashPrefixes.first()}*")
    .eq("status", "active")
    .gt("expires_at", now)
    .decodeList<Deal>()
    .filter { GeoUtils.calculateDistance(lat, lng, it.lat, it.lng) <= radiusMiles }
```

### Cross-platform Location
```kotlin
// Common interface
expect class LocationProvider {
    suspend fun getCurrentLocation(): Location?
    fun getLocationUpdates(): Flow<Location>
}

// Android actual (Fused Location Provider)
// iOS actual (CoreLocation)
```

## Roadmap

### Phase 1 (MVP) ✅
- [x] Basic deal browsing and posting
- [x] Real-time updates
- [x] Location-based filtering
- [x] Category filtering
- [x] Image upload

### Phase 2 (Enhanced UX)
- [ ] Push notifications
- [ ] Deal detail screen with map
- [ ] Vendor profile pages
- [ ] Deal bookmarking
- [ ] Search functionality

### Phase 3 (Business Features)
- [ ] Deal analytics for vendors
- [ ] Promoted deals (monetization)
- [ ] User reviews and ratings
- [ ] Advanced filtering (price range, distance)

### Phase 4 (Scale)
- [ ] Offline caching with SQLDelight
- [ ] MapLibre integration
- [ ] Social sharing
- [ ] Deal redemption tracking

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

[Add your license here]

---

Built with ❤️ using Kotlin Multiplatform and Supabase