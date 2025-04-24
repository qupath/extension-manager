package qupath.ext.extensionmanager.core.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.Version;

import java.util.Collections;
import java.util.List;

/**
 * A specification of the minimum and maximum versions that an extension supports. Versions should follow the
 * specifications of {@link Version}.
 *
 * @param min the minimum/lowest version that this extension is known to be compatible with
 * @param max the maximum/highest version that this extension is known to be compatible with. Can be null
 * @param excludes any specific versions that are not compatible. This list is immutable and won't be null
 */
public record VersionRange(String min, String max, List<String> excludes) {

    private static final Logger logger = LoggerFactory.getLogger(VersionRange.class);

    /**
     * Create a version range.
     * <p>
     * It must respect the following requirements:
     * <ul>
     *     <li>The 'min' field must be defined.</li>
     *     <li>If 'max' is specified, it must correspond to a version higher than or equal to 'min'.</li>
     *     <li>If 'excludes' is specified, each of its element must correspond to a version higher than or equal to
     *     'min', and lower than or equal to 'max' if 'max' is defined.</li>
     *     <li>
     *         All versions must be specified in the form "v[MAJOR].[MINOR].[PATCH]" corresponding to
     *         semantic versions, although trailing release candidate qualifiers (eg, "-rc1") are also allowed.
     *     </li>
     * </ul>
     *
     * @param min the minimum/lowest version that this extension is known to be compatible with
     * @param max the maximum/highest version that this extension is known to be compatible with. Can be null
     * @param excludes any specific versions that are not compatible. Can be null
     * @throws IllegalArgumentException when the created version range is not valid (see the requirements above)
     */
    public VersionRange(String min, String max, List<String> excludes) {
        this.min = min;
        this.max = max;
        this.excludes = excludes == null ? List.of() : Collections.unmodifiableList(excludes);

        checkValidity();
    }

    /**
     * Indicate if this release range is compatible with the provided version.
     *
     * @param version the version to check if this release range is compatible with. It
     *                must be specified in the form "v[MAJOR].[MINOR].[PATCH]", although
     *                trailing release candidate qualifiers (eg, "-rc1") are also allowed.
     * @return a boolean indicating if the provided version is compatible with this release range
     * @throws IllegalArgumentException if the provided version doesn't match the required form
     * or if this release range is not valid
     * @throws NullPointerException if the provided version is null
     */
    public boolean isCompatible(String version) {
        Version versionObject = new Version(version);

        if (new Version(min).compareTo(versionObject) > 0) {
            logger.debug(
                    "This version range {} is not compatible with {} because of the minimum compatible version",
                    this,
                    version
            );
            return false;
        }

        if (max != null && versionObject.compareTo(new Version(max)) > 0) {
            logger.debug(
                    "This version range {} is not compatible with {} because of the maximum compatible version",
                    this,
                    version
            );
            return false;
        }

        if (excludes != null && excludes.stream().map(Version::new).anyMatch(versionObject::equals)) {
            logger.debug(
                    "This version range {} is not compatible with {} because of the excluded versions",
                    this,
                    version
            );
            return false;
        }

        return true;
    }

    private void checkValidity() {
        Utils.checkField(min, "min", "VersionRange");

        Version.isValid(min, false);

        if (max != null) {
            if (new Version(min).compareTo(new Version(max)) > 0) {
                throw new IllegalArgumentException(String.format(
                        "The min version '%s' must be lower than or equal to the max version '%s'",
                        min,
                        max
                ));
            }
        }

        if (excludes != null) {
            for (String version: excludes) {
                if (new Version(min).compareTo(new Version(version)) > 0) {
                    throw new IllegalArgumentException(String.format(
                            "The min version '%s' must be lower than or equal to the excluded version '%s'",
                            min,
                            version
                    ));
                }
            }
        }

        if (max != null && excludes != null) {
            for (String version: excludes) {
                if (new Version(version).compareTo(new Version(max)) > 0) {
                    throw new IllegalArgumentException(String.format(
                            "The excluded version '%s' must be lower than or equal to the max version '%s'",
                            version,
                            max
                    ));
                }
            }
        }
    }
}
