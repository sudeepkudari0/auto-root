package com.assistant.root.context;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses UI elements from screen using uiautomator dump
 * Provides clickable elements with coordinates
 */
public class UIElementParser {
    private static final String TAG = "UIElementParser";

    public static class UIElement {
        public String resourceId;
        public String className;
        public String text;
        public String contentDesc;
        public boolean clickable;
        public int centerX;
        public int centerY;

        public UIElement(String resourceId, String text, String contentDesc,
                boolean clickable, int centerX, int centerY) {
            this.resourceId = resourceId;
            this.text = text;
            this.contentDesc = contentDesc;
            this.clickable = clickable;
            this.centerX = centerX;
            this.centerY = centerY;
        }

        public String getIdentifier() {
            if (text != null && !text.isEmpty())
                return text;
            if (contentDesc != null && !contentDesc.isEmpty())
                return contentDesc;
            if (resourceId != null)
                return resourceId.substring(resourceId.lastIndexOf("/") + 1);
            return "unknown";
        }
    }

    public static List<UIElement> getScreenElements() {
        List<UIElement> elements = new ArrayList<>();

        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            // Dump UI
            os.writeBytes("uiautomator dump /sdcard/window_dump.xml\n");
            os.flush();
            Thread.sleep(1000);

            // Read dump
            os.writeBytes("cat /sdcard/window_dump.xml\n");
            os.flush();

            StringBuilder uiDump = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                uiDump.append(line);
                if (line.contains("</hierarchy>"))
                    break;
            }

            // Cleanup
            os.writeBytes("rm /sdcard/window_dump.xml\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();

            reader.close();
            os.close();

            elements = parseUIXML(uiDump.toString());

        } catch (Exception e) {
            Log.e(TAG, "Failed to get elements: " + e.getMessage());
        }

        return elements;
    }

    private static List<UIElement> parseUIXML(String xml) {
        List<UIElement> elements = new ArrayList<>();
        Pattern nodePattern = Pattern.compile("<node([^>]+)/?>");
        Matcher nodeMatcher = nodePattern.matcher(xml);

        while (nodeMatcher.find()) {
            String attrs = nodeMatcher.group(1);

            String resourceId = extractAttr(attrs, "resource-id");
            String text = extractAttr(attrs, "text");
            String contentDesc = extractAttr(attrs, "content-desc");
            boolean clickable = "true".equals(extractAttr(attrs, "clickable"));
            String boundsStr = extractAttr(attrs, "bounds");

            if (clickable && boundsStr != null) {
                int[] coords = parseCenter(boundsStr);
                if (coords != null) {
                    elements.add(new UIElement(resourceId, text, contentDesc,
                            clickable, coords[0], coords[1]));
                }
            }
        }

        return elements;
    }

    private static String extractAttr(String attrs, String name) {
        Pattern pattern = Pattern.compile(name + "=\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(attrs);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static int[] parseCenter(String bounds) {
        try {
            Pattern p = Pattern.compile("\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]");
            Matcher m = p.matcher(bounds);
            if (m.find()) {
                int x1 = Integer.parseInt(m.group(1));
                int y1 = Integer.parseInt(m.group(2));
                int x2 = Integer.parseInt(m.group(3));
                int y2 = Integer.parseInt(m.group(4));
                return new int[] { (x1 + x2) / 2, (y1 + y2) / 2 };
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse bounds");
        }
        return null;
    }

    public static UIElement findByResourceId(List<UIElement> elements, String id) {
        for (UIElement el : elements) {
            if (el.resourceId != null && el.resourceId.contains(id)) {
                return el;
            }
        }
        return null;
    }

    public static UIElement findByText(List<UIElement> elements, String text) {
        text = text.toLowerCase();
        for (UIElement el : elements) {
            if (el.text != null && el.text.toLowerCase().contains(text)) {
                return el;
            }
            if (el.contentDesc != null && el.contentDesc.toLowerCase().contains(text)) {
                return el;
            }
        }
        return null;
    }

    public static String formatForAI(List<UIElement> elements) {
        StringBuilder sb = new StringBuilder();
        int count = 0;

        for (UIElement el : elements) {
            if (isMeaningful(el)) {
                sb.append(String.format("%d. %s at (%d,%d)\n",
                        ++count, el.getIdentifier(), el.centerX, el.centerY));
                if (count >= 15)
                    break;
            }
        }

        return sb.toString();
    }

    private static boolean isMeaningful(UIElement el) {
        return (el.text != null && !el.text.isEmpty()) ||
                (el.contentDesc != null && !el.contentDesc.isEmpty()) ||
                (el.resourceId != null && !el.resourceId.isEmpty() &&
                        !el.resourceId.contains("layout") && !el.resourceId.contains("frame"));
    }

    /**
     * Get formatted list of clickable elements for AI (legacy method)
     */
    public static String getElementsSummary() {
        List<UIElement> elements = getScreenElements();
        return formatForAI(elements);
    }

    /**
     * Find element by text (legacy method)
     */
    public static UIElement findElementByText(String text) {
        List<UIElement> elements = getScreenElements();
        return findByText(elements, text);
    }

    /**
     * Find element by content description (legacy method)
     */
    public static UIElement findElementByContentDesc(String contentDesc) {
        List<UIElement> elements = getScreenElements();
        for (UIElement el : elements) {
            if (el.contentDesc != null && el.contentDesc.toLowerCase().contains(contentDesc.toLowerCase())) {
                return el;
            }
        }
        return null;
    }

    /**
     * Find element by resource ID (legacy method)
     */
    public static UIElement findElementByResourceId(String resourceId) {
        List<UIElement> elements = getScreenElements();
        return findByResourceId(elements, resourceId);
    }

}
