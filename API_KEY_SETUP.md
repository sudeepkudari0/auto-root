# API Key Setup Guide

## Google AI API Key Configuration

This app uses Google's Gemini AI for natural language command processing. You need to set up your API key to use the AI features.

### Step 1: Get Your API Key

1. Go to [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Sign in with your Google account
3. Click "Create API Key"
4. Copy the generated API key

### Step 2: Configure the API Key

1. **Copy the template file:**
   ```bash
   cp local.properties.template local.properties
   ```

2. **Edit `local.properties` and add your API key:**
   ```properties
   # Android SDK location
   sdk.dir=/path/to/your/Android/Sdk
   
   # Google AI API Key
   GOOGLE_AI_API_KEY=your_actual_api_key_here
   ```

### Step 3: Verify Setup

1. Build the app:
   ```bash
   ./gradlew app:assembleDebug
   ```

2. Check the logs for:
   ```
   ✅ Google AI API key loaded successfully
   ```

### Security Notes

- ✅ **`local.properties` is in `.gitignore`** - your API key won't be committed to version control
- ✅ **API key is loaded at build time** - not hardcoded in source code
- ✅ **Template file provided** - easy setup for other developers

### Troubleshooting

**Error: "Google AI API key not found!"**
- Make sure `local.properties` exists in the project root
- Verify `GOOGLE_AI_API_KEY` is set in `local.properties`
- Check that the API key is valid and active

**Error: "AI model not initialized"**
- Check your internet connection
- Verify the API key has proper permissions
- Try regenerating the API key

### For Team Development

1. **Never commit `local.properties`** - it's already in `.gitignore`
2. **Share `local.properties.template`** - this shows the required format
3. **Each developer needs their own API key** - or use a shared development key

### API Key Limits

- Google AI API has usage limits
- Monitor your usage in [Google AI Studio](https://aistudio.google.com/)
- Consider implementing rate limiting for production use
