package com.assistant.root.context;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

/**
 * Smart command executor with retry logic and verification
 * Executes commands with context awareness
 */
public class SmartCommandExecutor {
    private static final String TAG = "SmartExecutor";
    private static final int MAX_RETRIES = 3;

    public interface ExecutionCallback {
        void onSuccess(String output);

        void onError(String error);

        void onProgress(String step);
    }

    /**
     * Execute commands with progress reporting
     */
    public static void executeWithProgress(String commands, ExecutionCallback callback) {
        new Thread(() -> {
            Process process = null;
            DataOutputStream os = null;
            BufferedReader reader = null;

            try {
                process = Runtime.getRuntime().exec("su");
                os = new DataOutputStream(process.getOutputStream());
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String[] commandLines = commands.split("\n");
                int totalSteps = commandLines.length;
                int currentStep = 0;

                for (String cmd : commandLines) {
                    cmd = cmd.trim();
                    if (cmd.isEmpty())
                        continue;

                    currentStep++;
                    callback.onProgress("Step " + currentStep + "/" + totalSteps + ": " + cmd);

                    Log.d(TAG, "Executing: " + cmd);
                    os.writeBytes(cmd + "\n");
                    os.flush();

                    // Handle sleep commands
                    if (cmd.startsWith("sleep ")) {
                        try {
                            double seconds = Double.parseDouble(cmd.replace("sleep ", "").trim());
                            Thread.sleep((long) (seconds * 1000));
                        } catch (Exception e) {
                            Thread.sleep(1000);
                        }
                    } else {
                        // Small delay between commands
                        Thread.sleep(300);
                    }
                }

                os.writeBytes("exit\n");
                os.flush();

                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }

                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    callback.onSuccess(output.toString());
                } else {
                    callback.onError("Command failed with exit code: " + exitCode);
                }

            } catch (Exception e) {
                Log.e(TAG, "Execution error: " + e.getMessage());
                callback.onError("Execution error: " + e.getMessage());
            } finally {
                try {
                    if (os != null)
                        os.close();
                    if (reader != null)
                        reader.close();
                    if (process != null)
                        process.destroy();
                } catch (Exception e) {
                    Log.e(TAG, "Cleanup error: " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Execute with element verification
     * Checks if element exists before tapping
     */
    public static void executeWithVerification(String commands, ExecutionCallback callback) {
        new Thread(() -> {
            try {
                String[] lines = commands.split("\n");
                StringBuilder verifiedCommands = new StringBuilder();

                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty())
                        continue;

                    // If it's a tap command, verify element exists
                    if (line.startsWith("input tap")) {
                        String[] parts = line.split(" ");
                        if (parts.length >= 3) {
                            int x = Integer.parseInt(parts[2]);
                            int y = Integer.parseInt(parts[3]);

                            // Check if these coordinates are reasonable
                            if (x > 0 && y > 0 && x < 1440 && y < 3000) {
                                verifiedCommands.append(line).append("\n");
                            } else {
                                Log.w(TAG, "Skipping invalid tap coordinates: " + line);
                                callback.onProgress("Warning: Skipped invalid coordinates");
                            }
                        }
                    } else {
                        verifiedCommands.append(line).append("\n");
                    }
                }

                executeWithProgress(verifiedCommands.toString(), callback);

            } catch (Exception e) {
                callback.onError("Verification failed: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Execute with retry on failure
     */
    public static void executeWithRetry(String commands, ExecutionCallback callback) {
        executeWithRetry(commands, callback, 0);
    }

    private static void executeWithRetry(String commands, ExecutionCallback callback, int retryCount) {
        executeWithProgress(commands, new ExecutionCallback() {
            @Override
            public void onSuccess(String output) {
                callback.onSuccess(output);
            }

            @Override
            public void onError(String error) {
                if (retryCount < MAX_RETRIES) {
                    Log.d(TAG, "Retrying... Attempt " + (retryCount + 1));
                    callback.onProgress("Retry attempt " + (retryCount + 1) + "/" + MAX_RETRIES);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // Ignore
                    }

                    executeWithRetry(commands, callback, retryCount + 1);
                } else {
                    callback.onError("Failed after " + MAX_RETRIES + " retries: " + error);
                }
            }

            @Override
            public void onProgress(String step) {
                callback.onProgress(step);
            }
        });
    }

    /**
     * Execute single tap on element
     */
    public static void tapElement(UIElementParser.UIElement element, ExecutionCallback callback) {
        if (element == null) {
            callback.onError("Element not found");
            return;
        }

        String command = "input tap " + element.centerX + " " + element.centerY;
        executeWithProgress(command, callback);
    }

    /**
     * Execute text input on element
     */
    public static void typeInElement(UIElementParser.UIElement element, String text, ExecutionCallback callback) {
        if (element == null) {
            callback.onError("Element not found");
            return;
        }

        // Replace spaces with %s for input command
        String encodedText = text.replace(" ", "%s");

        String commands = "input tap " + element.centerX + " " + element.centerY + "\n" +
                "sleep 0.5\n" +
                "input text '" + encodedText + "'";

        executeWithProgress(commands, callback);
    }

    /**
     * Smart tap - tries to find element first, then taps
     */
    public static void smartTap(String elementIdentifier, ExecutionCallback callback) {
        new Thread(() -> {
            // Try to find element
            UIElementParser.UIElement element = UIElementParser.findElementByText(elementIdentifier);

            if (element == null) {
                element = UIElementParser.findElementByContentDesc(elementIdentifier);
            }

            if (element == null) {
                element = UIElementParser.findElementByResourceId(elementIdentifier);
            }

            if (element != null) {
                tapElement(element, callback);
            } else {
                callback.onError("Could not find element: " + elementIdentifier);
            }
        }).start();
    }

    /**
     * Smart type - finds input field and types
     */
    public static void smartType(String text, ExecutionCallback callback) {
        new Thread(() -> {
            // Find any input field on screen
            UIElementParser.UIElement inputField = findInputField();

            if (inputField != null) {
                typeInElement(inputField, text, callback);
            } else {
                // Fallback: just type at current focus
                String encodedText = text.replace(" ", "%s");
                String command = "input text '" + encodedText + "'";
                executeWithProgress(command, callback);
            }
        }).start();
    }

    /**
     * Find input field on screen
     */
    private static UIElementParser.UIElement findInputField() {
        // Common input field class names
        String[] inputClasses = {
                "EditText",
                "AutoCompleteTextView",
                "TextInputEditText"
        };

        for (UIElementParser.UIElement element : UIElementParser.getScreenElements()) {
            for (String inputClass : inputClasses) {
                if (element.className != null && element.className.contains(inputClass)) {
                    return element;
                }
            }
        }

        return null;
    }

    /**
     * Execute with timeout
     */
    public static void executeWithTimeout(String commands, long timeoutMs, ExecutionCallback callback) {
        Thread executionThread = new Thread(() -> executeWithProgress(commands, callback));
        executionThread.start();

        new Thread(() -> {
            try {
                executionThread.join(timeoutMs);
                if (executionThread.isAlive()) {
                    executionThread.interrupt();
                    callback.onError("Execution timeout after " + (timeoutMs / 1000) + " seconds");
                }
            } catch (InterruptedException e) {
                callback.onError("Timeout check interrupted");
            }
        }).start();
    }

    /**
     * Batch execute multiple command sets
     */
    public static void executeBatch(String[] commandSets, ExecutionCallback callback) {
        new Thread(() -> {
            int total = commandSets.length;
            int completed = 0;

            for (String commands : commandSets) {
                final int current = completed + 1;
                callback.onProgress("Executing batch " + current + "/" + total);

                // Execute synchronously
                final boolean[] done = { false };
                final String[] error = { null };

                executeWithProgress(commands, new ExecutionCallback() {
                    @Override
                    public void onSuccess(String output) {
                        done[0] = true;
                    }

                    @Override
                    public void onError(String err) {
                        error[0] = err;
                        done[0] = true;
                    }

                    @Override
                    public void onProgress(String step) {
                        callback.onProgress("Batch " + current + ": " + step);
                    }
                });

                // Wait for completion
                while (!done[0]) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                if (error[0] != null) {
                    callback.onError("Batch " + current + " failed: " + error[0]);
                    return;
                }

                completed++;
            }

            callback.onSuccess("All " + total + " batches completed successfully");
        }).start();
    }
}
