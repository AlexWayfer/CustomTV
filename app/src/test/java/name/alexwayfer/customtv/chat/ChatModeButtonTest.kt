package name.alexwayfer.customtv.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatModeButtonTest {
    @Test
    fun followersOnlyLoggedInOffersFollow() {
        assertEquals(
            ChatModeButton.Follow,
            chatModeButton(ChatModePlaque.FollowersNeedFollow("Streamer"), signedIn = true),
        )
    }

    @Test
    fun followersOnlyLoggedOutOffersLogIn() {
        assertEquals(
            ChatModeButton.LogIn,
            chatModeButton(ChatModePlaque.FollowersNeedFollow("Streamer"), signedIn = false),
        )
    }

    @Test
    fun subscribersOnlyLoggedInOffersSubscribe() {
        assertEquals(
            ChatModeButton.Subscribe,
            chatModeButton(ChatModePlaque.Subscribers(offerSubscribe = true), signedIn = true),
        )
    }

    @Test
    fun subscribersOnlyLoggedOutOffersLogIn() {
        assertEquals(
            ChatModeButton.LogIn,
            chatModeButton(ChatModePlaque.Subscribers(offerSubscribe = true), signedIn = false),
        )
    }

    @Test
    fun subscribersOnlyForTheBroadcasterOffersNothing() {
        assertNull(chatModeButton(ChatModePlaque.Subscribers(offerSubscribe = false), signedIn = true))
    }

    @Test
    fun aPlaqueWithoutAnActionOffersNothingEvenLoggedOut() {
        assertNull(chatModeButton(ChatModePlaque.Emotes, signedIn = false))
    }
}
