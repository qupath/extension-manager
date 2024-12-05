package qupath.ext.extensionmanager.core;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A version that follows the specification "v[MAJOR].[MINOR].[PATCH]" or "v[MAJOR].[MINOR].[PATCH]-rc[RELEASE_CANDIDATE]"
 * corresponding to QuPath semantic versions.
 * <p>
 * This class is thread-safe.
 */
public class Version implements Comparable<Version> {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^v(\\d+)\\.(\\d+)\\.(\\d+)(?:-rc(\\d+))?");
    private final int major;
    private final int minor;
    private final int patch;
    private final int releaseCandidate;

    /**
     * Create a release from a text.
     *
     * @param version the text containing the release to parse. It must correspond to the
     *                specifications of this class.
     * @throws IllegalArgumentException when the provided text doesn't correspond to the
     * specifications of this class
     */
    public Version(String version) {
        Matcher matcher = VERSION_PATTERN.matcher(version);

        if (matcher.find() && matcher.groupCount() > 3) {
            major = Integer.parseInt(matcher.group(1));
            minor = Integer.parseInt(matcher.group(2));
            patch = Integer.parseInt(matcher.group(3));
            releaseCandidate = matcher.group(4) == null ? -1 : Integer.parseInt(matcher.group(4));
        } else {
            throw new IllegalArgumentException(String.format("Version not found in %s", version));
        }
    }

    @Override
    public String toString() {
        if (releaseCandidate == -1) {
            return String.format("v%d.%d.%d", major, minor, patch);
        } else {
            return String.format("v%d.%d.%d-rc%d", major, minor, patch, releaseCandidate);
        }
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
        if (minor != version.minor) {
            return minor - version.minor;
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
     * @param version the text containing the release to parse
     * @throws IllegalArgumentException when the provided text doesn't correspond
     * to the specifications of this class
     */
    public static void isValid(String version) {
        new Version(version);
    }
}
