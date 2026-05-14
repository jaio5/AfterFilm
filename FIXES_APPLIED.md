# PelisApp Configuration Fixes

## Issues Fixed

### ✅ 1. Book Title Column Too Long Error
**Problem:** Database error "Data too long for column 'title' at row 1" when importing books from Google Books
**Solution:** 
- Changed `Book.java` entity: `@Column(nullable = false, length = 500)`
- Created migration script: `src/main/resources/sql/fix_books_title_column.sql`
- Run this SQL before starting the app to fix the existing table

**To apply:**
```sql
mysql -u root -p PelisApp < src/main/resources/sql/fix_books_title_column.sql
```

### ✅ 2. Multiple TaskExecutor Bean Warning
**Problem:** "More than one TaskExecutor bean found within the context, and none is named 'taskExecutor'"
**Solution:** 
- Added a primary `taskExecutor` bean in `AsyncConfig.java`
- Named other specific executors: `tmdbTaskExecutor`, `bulkLoaderExecutor`, `seriesTaskExecutor`

### ✅ 3. JPA Open-in-View Warning
**Problem:** "spring.jpa.open-in-view is enabled by default"
**Solution:** 
- Set `spring.jpa.open-in-view=false` in `application.properties`

### ✅ 4. Server Port Changed
**Problem:** Server was running on port 8082, making it inaccessible from default Docker configuration
**Solution:** 
- Changed server port from 8082 to 8080 in `application.properties`
- URL is now: `http://localhost:8080`

### ⚠️ 5. TMDB API Key Invalid (Status 401)
**Problem:** TMDB API returning "Invalid API key: You must be granted a valid key"
**Solution Options:**

#### Option A: Get a Valid TMDB API Key
1. Go to https://www.themoviedb.org/settings/api
2. Create an account if you don't have one
3. Request an API key
4. Update `application.properties`:
   ```properties
   app.tmdb.api-key=YOUR_NEW_API_KEY_HERE
   ```

#### Option B: Use Bearer Token Authentication (Recommended)
1. Go to https://www.themoviedb.org/settings/api/read-access-tokens
2. Create a read-only token
3. Update `application.properties`:
   ```properties
   app.tmdb.bearer-token=YOUR_BEARER_TOKEN_HERE
   ```

#### Option C: Disable TMDB Loading on Startup (Temporary)
```properties
app.tmdb.load-on-startup=false
# Then load movies manually from admin panel
```

## Quick Start After Fixes

### 1. Apply Database Migration
```bash
mysql -u root -p PelisApp < src/main/resources/sql/fix_books_title_column.sql
```

### 2. Rebuild and Restart Application
```bash
./mvnw clean package -DskipTests
docker-compose down && docker-compose up -d
```

### 3. Verify Application is Running
```bash
curl http://localhost:8080
```

### 4. Access the Application
- **Web UI:** http://localhost:8080
- **Admin Panel:** http://localhost:8080/admin
- **API:** http://localhost:8080/api

### 5. Configure TMDB API (if needed)
- Get a valid API key from https://www.themoviedb.org
- Update environment variables or application.properties
- Restart the application

## Testing

After fixes are applied, you should see:
1. ✅ No database truncation errors
2. ✅ No TaskExecutor bean warnings
3. ✅ No JPA open-in-view warnings
4. ✅ Application accessible on port 8080
5. ⏳ TMDB API working (if configured with valid key)

## Additional Notes

- **Database migrations** use Hibernate's `ddl-auto=update` for non-destructive updates
- **Email configuration** is already working with Gmail SMTP
- **Image storage** is configured for `/app/data/images` in Docker
- **Video streaming** is configured for `/app/data/movies` in Docker

## Environment Variables for Production

```bash
# TMDB API
TMDB_API_KEY=your_api_key
TMDB_BEARER_TOKEN=your_bearer_token

# Database
SPRING_DATASOURCE_HOST=mysql-service
SPRING_DATASOURCE_PORT=3306
SPRING_DATASOURCE_DB=pelisapp
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your_password

# Mail
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# JWT
APP_JWT_SECRET=your-secret-key-at-least-32-characters

# Admin
APP_ADMIN_PASSWORD=secure_password

# Google Books API (optional)
GOOGLE_BOOKS_API_KEY=your_google_books_api_key
```
