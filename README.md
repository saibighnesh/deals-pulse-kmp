# DealsPulse

A **real-time, hyper-local flash deals app** built with Kotlin Multiplatform and Compose.

**Vendors** post short-lived offers (minutes/hours). **Shoppers** nearby instantly see and redeem them.

## 🚀 Features

- **Real-time deals feed** with location-based filtering
- **Urgency indicators** with countdown timers
- **Category filtering** (Food, Salon, Fitness, Retail, etc.)
- **Radius-based search** (5, 10, 20, 50 miles)
- **Live updates** via Supabase Realtime
- **Image uploads** for deals and vendor logos
- **Cross-platform** (Android + iOS) with single codebase

## 🏗️ Architecture

- **Frontend**: Kotlin Multiplatform + Compose Multiplatform
- **Backend**: Supabase (PostgreSQL + Auth + Realtime + Storage)
- **State Management**: MVVM with ViewModels
- **Navigation**: Voyager (planned)
- **Location**: Platform-specific (Fused Location Provider / CoreLocation)
- **Networking**: Ktor + Supabase Kotlin client

## 📱 Screenshots

*Coming soon - the app features a beautiful Material 3 design with:*
- Modern card-based deal listings
- Urgency badges (URGENT, SOON, ENDING, ACTIVE)
- Category filter chips
- Radius selection
- Real-time updates

## 🛠️ Tech Stack

### Client (Kotlin Multiplatform)
- **UI**: Compose Multiplatform
- **Language**: Kotlin 2.x
- **Async**: Coroutines
- **Serialization**: kotlinx.serialization
- **Navigation**: Voyager (planned)

### Backend (Supabase)
- **Database**: PostgreSQL with RLS
- **Auth**: Built-in email/password
- **API**: PostgREST
- **Realtime**: WebSocket subscriptions
- **Storage**: S3-compatible bucket
- **Edge Functions**: Auto-expiring deals

## 📋 Prerequisites

- Android Studio Hedgehog or later
- Kotlin 1.9.20+
- JDK 17+
- Supabase account (free tier)
- Android device/emulator for testing

## 🚀 Quick Start

### 1. Clone the Repository
```bash
git clone <your-repo-url>
cd DealsPulse
```

### 2. Set Up Supabase

1. Go to [supabase.com](https://supabase.com) and create a new project
2. Copy your project URL and anon key
3. Run the SQL schema in `database_schema.sql` in your Supabase SQL editor

### 3. Configure API Keys

#### Android
Create `composeApp/src/androidMain/kotlin/com/dealspulse/app/BuildConfig.kt`:

```kotlin
package com.dealspulse.app

object BuildConfig {
    const val SUPABASE_URL = "https://your-project.supabase.co"
    const val SUPABASE_ANON_KEY = "your-anon-key"
    const val SUPABASE_SERVICE_KEY = "your-service-key"
}
```

#### iOS
Update `composeApp/src/iosMain/kotlin/com/dealspulse/app/config/AppConfig.kt`:

```kotlin
actual object AppConfig {
    actual val supabaseUrl: String = "https://your-project.supabase.co"
    actual val supabaseAnonKey: String = "your-anon-key"
    actual val supabaseServiceKey: String = "your-service-key"
}
```

### 4. Build and Run

#### Android
```bash
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:installDebug
```

#### iOS
```bash
./gradlew :composeApp:iosSimulatorArm64Test
```

## 🗄️ Database Schema

The app uses three main tables:

- **`profiles`**: User and vendor information
- **`deals`**: Deal listings with location and expiration
- **`reports`**: User reports for moderation

Key features:
- **Geohash indexing** for location-based queries
- **Row Level Security** for data privacy
- **Auto-expiring deals** via cron jobs
- **Full-text search** on business names

## 🔐 Security

- **Row Level Security (RLS)** enabled on all tables
- **Public read access** to active deals only
- **Authenticated users** can create/update own content
- **Vendor verification** system for quality control
- **Rate limiting** via database triggers

## 📍 Location Services

### Android
- Fused Location Provider for high-accuracy location
- Background location updates
- Permission handling

### iOS
- CoreLocation integration
- Location permission requests
- Background location updates

## 🔄 Real-time Updates

- **Supabase Realtime** subscriptions
- **Live deal updates** (insert/update/delete)
- **Automatic feed refresh** when new deals appear
- **Smart filtering** based on current radius/category

## 🎨 UI Components

- **Material 3** design system
- **Responsive layouts** for different screen sizes
- **Dark/light theme** support
- **Accessibility** features
- **Pull-to-refresh** functionality

## 🧪 Testing

```bash
# Run all tests
./gradlew test

# Run Android tests only
./gradlew :composeApp:androidTest

# Run iOS tests only
./gradlew :composeApp:iosSimulatorArm64Test
```

## 📦 Dependencies

Key dependencies include:
- **Compose Multiplatform**: UI framework
- **Supabase Kotlin**: Backend client
- **Ktor**: HTTP networking
- **Coroutines**: Async programming
- **Voyager**: Navigation (planned)

## 🚧 Development Roadmap

### Phase 1 (Current)
- ✅ Basic project structure
- ✅ Data models and APIs
- ✅ Feed screen with real-time updates
- ✅ Location integration

### Phase 2 (Next)
- [ ] Deal detail screen
- [ ] Vendor profile screen
- [ ] Deal creation flow
- [ ] Image upload functionality

### Phase 3 (Future)
- [ ] Push notifications
- [ ] Offline caching with SQLDelight
- [ ] Maps integration
- [ ] Analytics and insights

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

- **Issues**: Create a GitHub issue
- **Discussions**: Use GitHub Discussions
- **Documentation**: Check the [docs](docs/) folder

## 🙏 Acknowledgments

- **Supabase** for the amazing backend platform
- **JetBrains** for Kotlin Multiplatform
- **Google** for Material Design and Compose
- **Open source community** for various libraries

---

**Built with ❤️ using Kotlin Multiplatform**