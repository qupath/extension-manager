package qupath.ext.extensionmanager.core.index;

import qupath.ext.extensionmanager.core.Version;

import java.util.List;

/**
 * A specification of the minimum and maximum versions that an extension supports. Versions should be specified in the
 * form "v[MAJOR].[MINOR].[PATCH]" corresponding to semantic versions, although trailing release candidate qualifiers
 * (eg, "-rc1") are also allowed.
 * <p>
 * Functions of this object may return null or throw exceptions if this object is not valid (see {@link #checkValidity()}).
 *
 * @param min the minimum/lowest version that this extension is known to be compatible with
 * @param max the maximum/highest version that this extension is known to be compatible with
 * @param excludes any specific versions that are not compatible
 */
public record VersionRange(String min, String max, List<String> excludes) {

    /**
     * Create a version range.
     *
     * @param min the minimum/lowest version that this extension is known to be compatible with
     * @param max the maximum/highest version that this extension is known to be compatible with
     * @param excludes any specific versions that are not compatible
     * @throws IllegalStateException if the created object is not valid (see {@link #checkValidity()})
     */
    public VersionRange(String min, String max, List<String> excludes) {
        this.min = min;
        this.max = max;
        this.excludes = excludes;

        checkValidity();
    }

    /**
     * @return the maximum/highest release that this extension is known to be compatible with.
     * Can be null
     */
    @Override
    public String max() {
        return max;
    }

    @Override
    public List<String> excludes() {
        return excludes == null ? List.of() : excludes;
    }

    /**
     * Check that this object is valid:
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
     * @throws IllegalStateException if this object is not valid
     */
    public void checkValidity() {
        Utils.checkField(min, "min", "VersionRange");

        try {
            Version.isValid(min);

            if (max != null) {
                if (new Version(min).compareTo(new Version(max)) > 0) {
                    throw new IllegalStateException(String.format(
                            "The min version '%s' must be lower than or equal to the max version '%s'",
                            min,
                            max
                    ));
                }
            }

            if (excludes != null) {
                for (String version: excludes) {
                    if (new Version(min).compareTo(new Version(version)) > 0) {
                        throw new IllegalStateException(String.format(
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
                        throw new IllegalStateException(String.format(
                                "The excluded version '%s' must be lower than or equal to the max version '%s'",
                                version,
                                max
                        ));
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e);
        }
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
     */
    public boolean isCompatible(String version) {
        Version versionObject = new Version(version);

        if (new Version(min).compareTo(versionObject) > 0) {
            return false;
        }

        if (max != null && versionObject.compareTo(new Version(max)) > 0) {
            return false;
        }

        return excludes == null || excludes.stream().map(Version::new).noneMatch(versionObject::equals);
    }
}

