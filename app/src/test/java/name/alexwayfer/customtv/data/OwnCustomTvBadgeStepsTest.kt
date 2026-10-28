package name.alexwayfer.customtv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OwnCustomTvBadgeStepsTest {
    private val hour = OWN_BADGE_REFRESH.inWholeMilliseconds
    private val free = OwnBadgeKey("42", CustomTvBadgeEdition.Free)
    private val premium = OwnBadgeKey("42", CustomTvBadgeEdition.Premium)

    @Test
    fun freeBuildShowsTheFreeBadgeAndPremiumWithAccessThePremiumOne() {
        assertEquals(free, ownBadgeKey("42", CustomTvBadgeDevice.Free))
        assertEquals(premium, ownBadgeKey("42", CustomTvBadgeDevice.Premium("secret")))
    }

    @Test
    fun loggedOutOrLockedShowsNone() {
        assertNull(ownBadgeKey(null, CustomTvBadgeDevice.Free))
        assertNull(ownBadgeKey("42", CustomTvBadgeDevice.Locked))
    }

    @Test
    fun badgeTurnedOnIsRegisteredAtFirst() {
        assertEquals(OwnBadgeStep.Register(free), ownBadgeStep(true, free, registered = null, nowMillis = 0))
    }

    @Test
    fun registeredBadgeWaitsAnHourAndIsConfirmedAgain() {
        val registered = OwnBadgeRegistration(free, atMillis = 1_000)

        assertEquals(OwnBadgeStep.Wait, ownBadgeStep(true, free, registered, nowMillis = 1_000 + hour - 1))
        assertEquals(OwnBadgeStep.Register(free), ownBadgeStep(true, free, registered, nowMillis = 1_000 + hour))
    }

    @Test
    fun anotherAccountOrEditionIsRegisteredAtOnce() {
        val registered = OwnBadgeRegistration(free, atMillis = 1_000)
        val other = OwnBadgeKey("7", CustomTvBadgeEdition.Free)

        assertEquals(OwnBadgeStep.Register(other), ownBadgeStep(true, other, registered, nowMillis = 1_001))
        assertEquals(OwnBadgeStep.Register(premium), ownBadgeStep(true, premium, registered, nowMillis = 1_001))
    }

    @Test
    fun badgeTurnedOffIsTakenOffOnce() {
        val registered = OwnBadgeRegistration(premium, atMillis = 1_000)

        assertEquals(OwnBadgeStep.Remove(premium), ownBadgeStep(false, premium, registered, nowMillis = 1_001))
        assertEquals(OwnBadgeStep.Wait, ownBadgeStep(false, premium, registered = null, nowMillis = 1_001))
    }

    @Test
    fun loggingOutTakesNothingOff() {
        val registered = OwnBadgeRegistration(free, atMillis = 1_000)

        assertEquals(OwnBadgeStep.Wait, ownBadgeStep(true, wanted = null, registered, nowMillis = 1_000 + hour))
    }

    @Test
    fun waitLastsUntilTheRegistrationIsDueOrAWholeRefresh() {
        val registered = OwnBadgeRegistration(free, atMillis = 1_000)

        assertEquals(hour - 500, ownBadgeWait(free, registered, nowMillis = 1_500).inWholeMilliseconds)
        assertEquals(hour, ownBadgeWait(free, registered = null, nowMillis = 1_500).inWholeMilliseconds)
        assertEquals(hour, ownBadgeWait(wanted = null, registered, nowMillis = 1_500).inWholeMilliseconds)
        assertEquals(0L, ownBadgeWait(free, registered, nowMillis = 1_000 + 2 * hour).inWholeMilliseconds)
    }
}
