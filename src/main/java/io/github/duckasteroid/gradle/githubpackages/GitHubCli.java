package io.github.duckasteroid.gradle.githubpackages;

import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;

import java.io.File;
import java.util.List;

/**
 * Reads credentials from the GitHub CLI, so that a developer who has already signed in with
 * {@code gh auth login} needs no token stored on disk.
 *
 * <p>The executable is located on {@code PATH} before anything is run, so a machine without the
 * CLI installed resolves to no value rather than failing the build. A CLI that is present but not
 * signed in exits non-zero with no output, which likewise resolves to no value.
 */
final class GitHubCli {

    private GitHubCli() {
    }

    /**
     * Runs {@code gh} with the given arguments and returns its trimmed standard output, or no
     * value when the CLI is absent, not signed in, or produced nothing.
     */
    static Provider<String> output(ProviderFactory providers, List<String> arguments) {
        return providers.environmentVariable(CredentialKeys.ENV_PATH)
                .map(GitHubCli::findOnPath)
                .flatMap(executable -> providers.exec(spec -> {
                    spec.setExecutable(executable);
                    spec.setArgs(arguments);
                    spec.setIgnoreExitValue(true);
                }).getStandardOutput().getAsText().map(GitHubCli::trimToNull));
    }

    /** The first GitHub CLI executable on the given {@code PATH}, or {@code null} if there is none. */
    static String findOnPath(String path) {
        for (String entry : path.split(File.pathSeparator)) {
            if (entry.isBlank()) {
                continue;
            }
            for (String name : CredentialKeys.GH_CLI_EXECUTABLES) {
                File candidate = new File(entry, name);
                if (candidate.isFile() && candidate.canExecute()) {
                    return candidate.getAbsolutePath();
                }
            }
        }
        return null;
    }

    /** Returns {@code null} rather than an empty string, so that a provider resolves to no value. */
    static String trimToNull(String value) {
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
