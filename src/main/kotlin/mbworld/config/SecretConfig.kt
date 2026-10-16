package mbworld.config

import encore.annotation.runtime.VenueKey
import kotlinx.serialization.Serializable

/**
 * Application secret config definition.
 *
 * Place every venue-supplied configuration here, also annotate with [VenueKey].
 * After that, modify `venue.secret.xml` accordingly from this data class entries.
 * The secret file will not be tracked by Git.
 *
 * All field is preferred to be immutable.
 */
@Serializable
data class SecretConfig(
    val dummy: Int = 0
)
