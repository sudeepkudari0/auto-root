package com.assistant.root.ai;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class RootCommandExecutor {
    private static final String TAG = "RootCommandExecutor";

    public interface ExecutionCallback {
        void onSuccess(String output);

        void onError(String error);
    }

    /**
     * Check if device has root access
     */
    public static boolean hasRootAccess() {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("exit\n");
            os.flush();

            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    /**
     * Execute single root command
     */
    public static void executeCommand(String command, ExecutionCallback callback) {
        if (command == null || command.trim().isEmpty()) {
            callback.onError("Empty command");
            return;
        }

        new Thread(() -> {
            Process process = null;
            DataOutputStream os = null;
            BufferedReader reader = null;
            BufferedReader errorReader = null;

            try {
                Log.d(TAG, "Executing: " + command);

                process = Runtime.getRuntime().exec("su");
                os = new DataOutputStream(process.getOutputStream());
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                // Write command
                os.writeBytes(command + "\n");
                os.writeBytes("exit\n");
                os.flush();

                // Read output
                StringBuilder output = new StringBuilder();
                StringBuilder error = new StringBuilder();

                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }

                while ((line = errorReader.readLine()) != null) {
                    error.append(line).append("\n");
                }

                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    Log.d(TAG, "Command executed successfully");
                    callback.onSuccess(output.toString());
                } else {
                    String errorMsg = error.toString();
                    if (errorMsg.isEmpty())
                        errorMsg = "Command failed with exit code: " + exitCode;
                    Log.e(TAG, "Command failed: " + errorMsg);
                    callback.onError(errorMsg);
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
                    if (errorReader != null)
                        errorReader.close();
                    if (process != null)
                        process.destroy();
                } catch (Exception e) {
                    Log.e(TAG, "Cleanup error: " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Execute multiple commands (multi-line)
     */
    public static void executeMultipleCommands(String commands, ExecutionCallback callback) {
        if (commands == null || commands.trim().isEmpty()) {
            callback.onError("Empty commands");
            return;
        }

        new Thread(() -> {
            Process process = null;
            DataOutputStream os = null;
            BufferedReader reader = null;

            try {
                Log.d(TAG, "Executing multiple commands:\n" + commands);

                process = Runtime.getRuntime().exec("su");
                os = new DataOutputStream(process.getOutputStream());
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                // Write all commands
                String[] commandLines = commands.split("\n");
                for (String cmd : commandLines) {
                    cmd = cmd.trim();
                    if (!cmd.isEmpty()) {
                        Log.d(TAG, "-> " + cmd);
                        os.writeBytes(cmd + "\n");
                        os.flush();

                        // Add delay for sleep commands
                        if (cmd.startsWith("sleep ")) {
                            try {
                                int seconds = Integer.parseInt(cmd.replace("sleep ", "").trim());
                                Thread.sleep(seconds * 1000L);
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }

                os.writeBytes("exit\n");
                os.flush();

                // Read output
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }

                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    Log.d(TAG, "Commands executed successfully");
                    callback.onSuccess(output.toString());
                } else {
                    callback.onError("Commands failed with exit code: " + exitCode);
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
     * Execute command with timeout
     */
    public static void executeCommandWithTimeout(String command, long timeoutMs, ExecutionCallback callback) {
        Thread executionThread = new Thread(() -> executeCommand(command, callback));
        executionThread.start();

        new Thread(() -> {
            try {
                executionThread.join(timeoutMs);
                if (executionThread.isAlive()) {
                    executionThread.interrupt();
                    callback.onError("Command execution timeout");
                }
            } catch (InterruptedException e) {
                callback.onError("Timeout check interrupted");
            }
        }).start();
    }
}
