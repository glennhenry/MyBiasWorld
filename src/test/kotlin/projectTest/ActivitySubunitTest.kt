package projectTest

import kotlinx.coroutines.test.runTest
import mbworld.domain.activity.ActivityReceiver
import mbworld.domain.activity.ActivitySubunit
import mbworld.domain.activity.model.Activity
import mbworld.domain.activity.model.ActivitySource
import mbworld.domain.cafe.CafeActivity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ActivitySubunitTest {
    @Test
    fun `receive successfully with source`() = runTest {
        val received = mutableListOf<String>()
        val subunit = ActivitySubunit(this.backgroundScope)
        subunit.register(
            object : ActivityReceiver {
                override val sources: Set<ActivitySource> = setOf(ActivitySource.Cafe)
                override val types: Set<String> = setOf()

                override fun onActivity(activity: Activity) {
                    received.add(activity.type)
                }
            }
        )
        subunit.start()
        subunit.publish(
            Activity(
                source = ActivitySource.Cafe,
                type = CafeActivity.TopicLiked,
                timestamp = 1,
                metadata = emptyMap()
            )
        )
        subunit.publish(
            Activity(
                source = ActivitySource.Cafe,
                type = CafeActivity.ReplyLiked,
                timestamp = 1,
                metadata = emptyMap()
            )
        )
        subunit.publish(
            Activity(
                source = ActivitySource.Cafe,
                type = CafeActivity.ReplyUnliked,
                timestamp = 1,
                metadata = emptyMap()
            )
        )
        assertTrue {
            received.contains(CafeActivity.TopicLiked) &&
                    received.contains(CafeActivity.ReplyLiked) &&
                    received.contains(CafeActivity.ReplyUnliked)
        }
    }

    @Test
    fun `receive successfully with type`() = runTest {
        var received = 0
        val subunit = ActivitySubunit(this.backgroundScope)
        subunit.register(
            object : ActivityReceiver {
                override val sources: Set<ActivitySource> = setOf()
                override val types: Set<String> = setOf(CafeActivity.TopicLiked)

                override fun onActivity(activity: Activity) {
                    received += 1
                }
            }
        )
        subunit.start()
        subunit.publish(
            Activity(
                source = ActivitySource.Cafe,
                type = CafeActivity.TopicLiked,
                timestamp = 1,
                metadata = emptyMap()
            )
        )
        subunit.publish(
            Activity(
                source = ActivitySource.Cafe,
                type = CafeActivity.ReplyLiked,
                timestamp = 1,
                metadata = emptyMap()
            )
        )
        assertEquals(1, received)
    }

    @Test
    fun `receive only once with duplicate source and types`() = runTest {
        var received = 0
        val subunit = ActivitySubunit(this.backgroundScope)
        subunit.register(
            object : ActivityReceiver {
                override val sources: Set<ActivitySource> = setOf(ActivitySource.Cafe)
                override val types: Set<String> = setOf(CafeActivity.TopicLiked)

                override fun onActivity(activity: Activity) {
                    received += 1
                }
            }
        )
        subunit.start()
        subunit.publish(
            Activity(
                source = ActivitySource.Cafe,
                type = CafeActivity.TopicLiked,
                timestamp = 1,
                metadata = emptyMap()
            )
        )
        assertEquals(1, received)
    }
}
