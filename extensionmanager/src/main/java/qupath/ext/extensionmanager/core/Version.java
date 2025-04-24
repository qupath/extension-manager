package qupath.ext.extensionmanager.core;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A version that follows the semantic versions "v[MAJOR].[MINOR].[PATCH]".
 * <p>
 * The [MINOR] and [PATCH] numbers can be omitted, in which case the version will actually refer to a collection of versions.
 * For example, v0.5 refers to all versions v0.5.x, where x is a positive integer.
 * <p>
 * Trailing release candidate qualifiers (like "-rc1") are allowed and taken into account. Other types of qualifiers (like "-SNAPSHOT")
 * are also accepted but won't be retained (so they won't be taken into account in {@link #compareTo(Version)} for example).
 * <p>
 * This class is thread-safe.
 */
public class Version implements Comparable<Version> {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^v(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:-rc(\\d+))?");
    private final int major;
    private final int minor;
    private final int patch;
    private final int releaseCandidate;

    /**
     * Create a release from a text.
     *
     * @param version the text containing the release to parse. It must correspond to the
     *                specifications of this class.
     * @throws IllegalArgumentException if the provided text doesn't correspond to the
     * specifications of this class
     * @throws NullPointerException if the provided version is null
     */
    public Version(String version) {
        Matcher matcher = VERSION_PATTERN.matcher(version);

        if (matcher.find()) {
            major = Integer.parseInt(matcher.group(1));
            minor = matcher.group(2) == null ? -1 : Integer.parseInt(matcher.group(2));
            patch = matcher.group(3) == null ? -1 : Integer.parseInt(matcher.group(3));
            releaseCandidate = matcher.group(4) == null ? -1 : Integer.parseInt(matcher.group(4));
        } else {
            throw new IllegalArgumentException(String.format("Version not found in %s", version));
        }
    }

    @Override
    public String toString() {
        StringBuilder version = new StringBuilder("v");
        version.append(major);

        if (minor > -1) {
            version.append(".");
            version.append(minor);
        }
        if (patch > -1) {
            version.append(".");
            version.append(patch);
        }
        if (releaseCandidate > -1) {
            version.append("-rc");
            version.append(releaseCandidate);
        }

        return version.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Version version)) {
            return false;
        }
        return major == version.major && minor == version.minor && patch == version.patch && releaseCandidate == version.releaseCandidate;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, releaseCandidate);
    }

    @Override
    public int compareTo(Version version) {
        if (major != version.major) {
            return major - version.major;
        }
        if (minor < 0 || version.minor < 0) {
            return 0;
        }
        if (minor != version.minor) {
            return minor - version.minor;
        }
        if (patch < 0 || version.patch < 0) {
            return 0;
        }
        if (patch != version.patch) {
            return patch - version.patch;
        }
        if (releaseCandidate != -1 && version.releaseCandidate != -1) {
            return releaseCandidate - version.releaseCandidate;
        }
        if (releaseCandidate != -1) {
            return -1;
        }
        if (version.releaseCandidate != -1) {
            return 1;
        }

        return 0;
    }

    /**
     * Check if the provided text follows the specifications of this class.
     *
     * @param text the text containing the version to parse
     * @param includeMinorAndPath whether the created version should contain valid patch and minor numbers
     * @throws IllegalArgumentException when the provided text doesn't correspond to the specifications of this class
     * @throws NullPointerException if the provided version is null
     */
    public static void isValid(String text, boolean includeMinorAndPath) {
        Version version = new Version(text);

        if (includeMinorAndPath && (version.minor < 0 || version.patch < 0)) {
            throw new IllegalArgumentException(String.format("The provided version %s doesn't have a minor or patch number", text));
        }
    }
}
