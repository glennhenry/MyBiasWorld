package encore.definition

/**
 * Describes rules, policies, and static data.
 *
 * A `DataDefinition` encapsulates the data and logic that describe
 * how a particular domain of the platform behaves.
 *
 * Examples:
 * - `LevelConfig` may define the available levels, EXP threshold, and rewards.
 * - `BadgeList` may define the available badges.
 *
 * Instances are typically produced from a [FileDataSource]
 * via a corresponding [FileDataLoader].
 *
 * See example `encoreTest.definition.DataReferenceTest`.
 */
interface DataDefinition
