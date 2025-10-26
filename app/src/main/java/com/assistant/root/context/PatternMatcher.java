package com.assistant.root.context;

import android.util.Log;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pattern matcher for instant command generation
 * Handles common commands without AI - INSTANT execution
 */
public class PatternMatcher {
    private static final String TAG = "PatternMatcher";

    public static class MatchResult {
        public boolean matched;
        public String command;
        public String description;

        public MatchResult(boolean matched, String command, String description) {
            this.matched = matched;
            this.command = command;
            this.description = description;
        }
    }

    /**
     * Try to match user input against known patterns
     * Returns command immediately if matched
     */
    public static MatchResult tryMatch(String userInput, List<UIElementParser.UIElement> elements,
            ContextDetector.AppContext context) {
        String lower = userInput.toLowerCase().trim();

        // Pattern 1: "type X and send" / "type X and click send"
        MatchResult result = matchTypeAndSend(lower, elements);
        if (result.matched)
            return result;

        // Pattern 2: "send message X" / "message X"
        result = matchSendMessage(lower, elements);
        if (result.matched)
            return result;

        // Pattern 3: "click X" / "tap X" / "press X"
        result = matchClick(lower, elements);
        if (result.matched)
            return result;

        // Pattern 4: "search X" / "search for X"
        result = matchSearch(lower, elements, context);
        if (result.matched)
            return result;

        // Pattern 5: "go back" / "go home"
        result = matchNavigation(lower);
        if (result.matched)
            return result;

        // Pattern 6: "open X" (if already in an app)
        result = matchOpenInApp(lower, elements, context);
        if (result.matched)
            return result;

        // Pattern 7: "type X" (just typing)
        result = matchTypeOnly(lower, elements);
        if (result.matched)
            return result;

        // No match found
        return new MatchResult(false, null, "No pattern matched");
    }

    /**
     * Pattern: "type hello and send"
     */
    private static MatchResult matchTypeAndSend(String input, List<UIElementParser.UIElement> elements) {
        Pattern pattern = Pattern.compile("type\\s+(.+?)\\s+and\\s+(send|click send)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            String message = matcher.group(1);

            // Find input field and send button
            UIElementParser.UIElement inputField = findInputField(elements);
            UIElementParser.UIElement sendButton = findSendButton(elements);

            if (inputField != null && sendButton != null) {
                String encodedMsg = message.replace(" ", "%s");
                String command = String.format(
                        "input tap %d %d\nsleep 0.5\ninput text '%s'\nsleep 0.5\ninput tap %d %d",
                        inputField.centerX, inputField.centerY,
                        encodedMsg,
                        sendButton.centerX, sendButton.centerY);

                Log.d(TAG, "✓ Matched: type and send");
                return new MatchResult(true, command, "Type and send pattern");
            }
        }

        return new MatchResult(false, null, null);
    }

    /**
     * Pattern: "send message hello" / "message hello"
     */
    private static MatchResult matchSendMessage(String input, List<UIElementParser.UIElement> elements) {
        Pattern pattern = Pattern.compile("(send message|message|send)\\s+(.+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            String message = matcher.group(2);

            UIElementParser.UIElement inputField = findInputField(elements);
            UIElementParser.UIElement sendButton = findSendButton(elements);

            if (inputField != null && sendButton != null) {
                String encodedMsg = message.replace(" ", "%s");
                String command = String.format(
                        "input tap %d %d\nsleep 0.5\ninput text '%s'\nsleep 0.5\ninput tap %d %d",
                        inputField.centerX, inputField.centerY,
                        encodedMsg,
                        sendButton.centerX, sendButton.centerY);

                Log.d(TAG, "✓ Matched: send message");
                return new MatchResult(true, command, "Send message pattern");
            }
        }

        return new MatchResult(false, null, null);
    }

    /**
     * Pattern: "click search" / "tap send" / "press back"
     */
    private static MatchResult matchClick(String input, List<UIElementParser.UIElement> elements) {
        Pattern pattern = Pattern.compile("(click|tap|press)\\s+(.+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            String elementName = matcher.group(2);

            UIElementParser.UIElement element = UIElementParser.findByText(elements, elementName);

            if (element != null) {
                String command = String.format("input tap %d %d", element.centerX, element.centerY);
                Log.d(TAG, "✓ Matched: click element");
                return new MatchResult(true, command, "Click pattern");
            }
        }

        return new MatchResult(false, null, null);
    }

    /**
     * Pattern: "search restaurants" / "search for music"
     */
    private static MatchResult matchSearch(String input, List<UIElementParser.UIElement> elements,
            ContextDetector.AppContext context) {
        Pattern pattern = Pattern.compile("search\\s+(for\\s+)?(.+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            String query = matcher.group(2);

            // Find search button/field
            UIElementParser.UIElement searchElement = findSearchElement(elements);

            if (searchElement != null) {
                String encodedQuery = query.replace(" ", "%s");
                String command = String.format(
                        "input tap %d %d\nsleep 1\ninput text '%s'\nsleep 0.5\ninput keyevent 66",
                        searchElement.centerX, searchElement.centerY, encodedQuery);

                Log.d(TAG, "✓ Matched: search");
                return new MatchResult(true, command, "Search pattern");
            }
        }

        return new MatchResult(false, null, null);
    }

    /**
     * Pattern: "go back" / "go home" / "press back"
     */
    private static MatchResult matchNavigation(String input) {
        if (input.matches(".*(go back|press back|back button).*")) {
            Log.d(TAG, "✓ Matched: go back");
            return new MatchResult(true, "input keyevent 4", "Back navigation");
        }

        if (input.matches(".*(go home|home button|press home).*")) {
            Log.d(TAG, "✓ Matched: go home");
            return new MatchResult(true, "input keyevent 3", "Home navigation");
        }

        if (input.matches(".*(press enter|hit enter).*")) {
            Log.d(TAG, "✓ Matched: press enter");
            return new MatchResult(true, "input keyevent 66", "Enter key");
        }

        return new MatchResult(false, null, null);
    }

    /**
     * Pattern: "open settings" (when in an app)
     */
    private static MatchResult matchOpenInApp(String input, List<UIElementParser.UIElement> elements,
            ContextDetector.AppContext context) {
        Pattern pattern = Pattern.compile("open\\s+(.+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            String target = matcher.group(1);

            // Try to find element with that name
            UIElementParser.UIElement element = UIElementParser.findByText(elements, target);

            if (element != null) {
                String command = String.format("input tap %d %d", element.centerX, element.centerY);
                Log.d(TAG, "✓ Matched: open in-app");
                return new MatchResult(true, command, "Open in-app pattern");
            }
        }

        return new MatchResult(false, null, null);
    }

    /**
     * Pattern: "type hello" (just typing, no send)
     */
    private static MatchResult matchTypeOnly(String input, List<UIElementParser.UIElement> elements) {
        Pattern pattern = Pattern.compile("type\\s+(.+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            String text = matcher.group(1);

            UIElementParser.UIElement inputField = findInputField(elements);

            if (inputField != null) {
                String encodedText = text.replace(" ", "%s");
                String command = String.format(
                        "input tap %d %d\nsleep 0.5\ninput text '%s'",
                        inputField.centerX, inputField.centerY, encodedText);

                Log.d(TAG, "✓ Matched: type only");
                return new MatchResult(true, command, "Type pattern");
            }
        }

        return new MatchResult(false, null, null);
    }

    // Helper methods
    private static UIElementParser.UIElement findInputField(List<UIElementParser.UIElement> elements) {
        // Try common input field IDs
        String[] inputIds = { "entry", "edit", "input", "text", "message", "search" };

        for (String id : inputIds) {
            UIElementParser.UIElement el = UIElementParser.findByResourceId(elements, id);
            if (el != null)
                return el;
        }

        return null;
    }

    private static UIElementParser.UIElement findSendButton(List<UIElementParser.UIElement> elements) {
        // Try common send button IDs
        UIElementParser.UIElement el = UIElementParser.findByResourceId(elements, "send");
        if (el != null)
            return el;

        return UIElementParser.findByText(elements, "send");
    }

    private static UIElementParser.UIElement findSearchElement(List<UIElementParser.UIElement> elements) {
        UIElementParser.UIElement el = UIElementParser.findByResourceId(elements, "search");
        if (el != null)
            return el;

        return UIElementParser.findByText(elements, "search");
    }
}
