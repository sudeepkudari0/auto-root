# AI Integration Setup Guide

## Overview
Your Android app now includes AI-powered command generation using Google's Gemini AI. The AI can convert natural language commands into Android shell commands and execute them with root privileges.

## Setup Instructions

### 1. Get Google AI API Key
1. Go to [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Create a new API key
3. Copy the API key

### 2. Configure API Key
1. Open `app/src/main/java/com/assistant/root/ai/AICommandGenerator.java`
2. Find line 40: `"YOUR_API_KEY_HERE"`
3. Replace with your actual API key:
   ```java
   GenerativeModel gm = new GenerativeModel(
       "gemini-1.5-flash",
       "your-actual-api-key-here"
   );
   ```

### 3. Build and Install
1. Sync your project with Gradle files
2. Build the app: `./gradlew build`
3. Install on your rooted device: `./gradlew installDebug`

## How It Works

### Command Flow
1. **User Input**: "open whatsapp and send hi to +919876543210"
2. **AI Processing**: Gemini AI converts this to shell commands
3. **Command Generation**: 
   ```
   am start -n com.whatsapp/.HomeActivity
   sleep 2
   am start -a android.intent.action.VIEW -d 'https://api.whatsapp.com/send?phone=919876543210&text=hi'
   ```
4. **Execution**: Commands are executed with root privileges

### AI Skill Matching
The AI skill automatically handles complex commands that:
- Contain multiple actions ("open X and do Y")
- Involve app interactions ("send message to X")
- Require web actions ("open youtube and search for X")
- Need automation sequences

### Safety Features
- Command validation prevents dangerous operations
- Only allows safe Android commands (am, pm, input, settings)
- Blocks system-destructive commands (rm, format, etc.)

## Example Commands

### Simple App Opening
- "open whatsapp" → `am start -n com.whatsapp/.HomeActivity`
- "launch youtube" → `am start -n com.google.android.youtube/.HomeActivity`

### Complex Actions
- "open whatsapp and send hello to +919876543210"
- "open youtube and search for music"
- "open chrome and go to google.com"
- "type hello world and press enter"

### Automation Sequences
- "open instagram, wait 2 seconds, then tap at 500 300"
- "open settings, go to wifi, and turn it off"

## Testing

### Test Root Access
```java
// In your app, you can test root access:
CommandExecutor executor = new CommandExecutor(context);
boolean hasRoot = executor.hasRootAccess();
```

### Test AI Command Generation
```java
// Test AI command generation:
executor.executeAICommand("open whatsapp and send hi to +919876543210");

// Test AI with simple command:
executor.testAI("open whatsapp");
```

## Troubleshooting

### Common Issues

1. **Model Not Found Error**
   ```
   models/gemini-1.5-flash is not found for API version v1
   ```
   - **Solution**: The app now tries multiple model names automatically:
     - `gemini-pro` (primary)
     - `gemini-1.5-pro` (fallback)
     - `gemini-1.5-flash` (fallback)
     - `gemini-1.0-pro` (fallback)
   - Check logs to see which model successfully initializes

2. **API Key Error**
   - Ensure you've replaced "YOUR_API_KEY_HERE" with your actual key
   - Check that the API key is valid and has proper permissions
   - Verify the API key has access to Gemini models

3. **Root Access Denied**
   - Ensure your device is properly rooted
   - Grant root permissions to your app when prompted

4. **Network Issues**
   - Ensure device has internet connection for AI API calls
   - Check firewall settings if using corporate network

5. **Command Execution Fails**
   - Check that the generated commands are valid
   - Verify target apps are installed
   - Check root permissions

6. **AI Model Not Initialized**
   - Check internet connection
   - Verify API key is correct
   - Check logs for model initialization errors

### Debug Logs
Enable debug logging to see:
- AI-generated commands
- Command execution results
- Error messages

Look for logs with tags:
- `AICommandGenerator`
- `RootCommandExecutor`
- `CommandExecutor`

## Security Notes

- The AI only generates safe Android shell commands
- Dangerous commands are automatically blocked
- All commands require root access (which you already have)
- API key should be kept secure and not shared

## Next Steps

1. Set up your API key
2. Test with simple commands first
3. Try complex multi-step commands
4. Customize the AI prompt if needed for your specific use cases

The AI integration is now ready to use! You can speak natural language commands and the AI will convert them to appropriate shell commands for execution.
