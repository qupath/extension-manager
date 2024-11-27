package qupath.ext.extensionmanager.core.index.model;

import qupath.ext.extensionmanager.core.Version;

import java.util.List;

/**
 * A specification of the minimum and maximum QuPath versions that an extension supports.
 * Versions should be specified in the form "v[MAJOR].[MINOR].[PATCH]" or "v[MAJOR].[MINOR].[PATCH]-rc[RELEASE_CANDIDATE]"
 * corresponding to QuPath semantic versions.
 * <p>
 * Functions of this object may return null or throw exceptions if this object is not valid (see {@link #checkValidity()}).
 *
 * @param min the minimum/lowest QuPath release that this extension is known to be compatible with
 * @param max the maximum/highest QuPath release that this extension is known to be compatible with
 * @param excludes any specific versions that are not compatible
 */
public record QuPathVersionRange(String min, String max, List<String> excludes) {

    /**
     * Create a QuPathVersionRange.
     *
     * @param min the minimum/lowest QuPath release that this extension is known to be compatible with
     * @param max the maximum/highest QuPath release that this extension is known to be compatible with
     * @param excludes any specific versions that are not compatible
     * @throws IllegalStateException when the created object is not valid (see {@link #checkValidity()})
     */
    public QuPathVersionRange(String min, String max, List<String> excludes) {
        this.min = min;
        this.max = max;
        this.excludes = excludes;

        checkValidity();
    }

    /**
     * @return the maximum/highest QuPath release that this extension is known to be compatible with.
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
     *     <li>
     *         All versions must be specified in the form "v[MAJOR].[MINOR].[PATCH]" or
     *         "v[MAJOR].[MINOR].[PATCH]-rc[RELEASE_CANDIDATE]" corresponding to QuPath semantic versions.
     *     </li>
     * </ul>
     *
     * @throws IllegalStateException when this object is not valid
     */
    public void checkValidity() {
        Utils.checkField(min, "min", "QuPathVersionRange");

        try {
            Version.isValid(min);

            if (max != null) {
                Version.isValid(max);
            }

            if (excludes != null) {
                for (String version: excludes) {
                    Version.isValid(version);
                }
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Indicate if this release range is compatible with the provided QuPath release.
     *
     * @param qupathVersion the QuPath release to check if this release range is compatible with. It
     *                      must be specified in the form "v[MAJOR].[MINOR].[PATCH]" or
     *                      "v[MAJOR].[MINOR].[PATCH]-rc[RELEASE_CANDIDATE]"
     * @return a boolean indicating if the provided QuPath release is compatible with this release range
     * @throws IllegalArgumentException when the provided qupath release doesn't match the required form
     * or when this release range is not valid
     */
    public boolean isCompatible(String qupathVersion) {
        Version qupath = new Version(qupathVersion);

        if (new Version(min).compareTo(qupath) > 0) {
            return false;
        }

        if (max != null && qupath.compareTo(new Version(max)) > 0) {
            return false;
        }

        return excludes == null || excludes.stream().map(Version::new).noneMatch(qupath::equals);
    }
}

