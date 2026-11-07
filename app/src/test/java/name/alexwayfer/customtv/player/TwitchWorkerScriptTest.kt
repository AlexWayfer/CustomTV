package name.alexwayfer.customtv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TwitchWorkerScriptTest {
    @Test
    fun recognizesOnlyThePublicHttpsWorkerAsset() {
        val worker = "https://assets.twitch.tv/assets/amazon-ivs-wasmworker.min-1da59c3161bea7d07190.js"
        assertTrue(isTwitchWorkerScript(worker))
        assertTrue(isTwitchWorkerScript("$worker?cache=1"))
        assertFalse(isTwitchWorkerScript(worker.replace("https:", "http:")))
        assertFalse(isTwitchWorkerScript(worker.replace("assets.twitch.tv", "example.com")))
        assertFalse(isTwitchWorkerScript(worker.replace("assets.twitch.tv", "assets.twitch.tv.example.com")))
        assertFalse(isTwitchWorkerScript(worker.replace("assets.twitch.tv", "assets.twitch.tv:8443")))
        assertFalse(isTwitchWorkerScript(worker.replace("wasmworker", "player")))
        assertFalse(isTwitchWorkerScript("not a URL"))
    }

    @Test
    fun replacesTheSinkRemovalAndPreservesTheRestOfTheWorker() {
        val source = "before,t.remove=$REMOVE,after"
        assertEquals("before,t.remove=$CLEANUP,after", patchTwitchWorkerScript(source, CLEANUP))
    }

    @Test
    fun recognizesRenamedMinifierVariables() {
        val renamed = "function(a,b){for(var buffers=this.sourceBuffers,index=0,keys=Object.keys(buffers);" +
            "index<keys.length;index++)buffers[keys[index]].remove(a,b)}"
        assertEquals(CLEANUP, patchTwitchWorkerScript(renamed, CLEANUP))
    }

    @Test
    fun doesNotRewriteAChangedOrUnrelatedRemovalFunction() {
        assertNull(patchTwitchWorkerScript("function(e,t){this.sourceBuffers.remove(e,t)}", CLEANUP))
        assertNull(patchTwitchWorkerScript(REMOVE.replace("this.sourceBuffers", "this.otherBuffers"), CLEANUP))
        assertNull(patchTwitchWorkerScript(REMOVE.replace(".remove(e,t)", ".remove(t,e)"), CLEANUP))
    }

    @Test
    fun rejectsMultipleMatchesInsteadOfGuessingWhichSinkIsActive() {
        assertNull(patchTwitchWorkerScript("first=$REMOVE,second=$REMOVE", CLEANUP))
    }

    @Test
    fun doesNotPatchAnAlreadyReplacedWorkerAgain() {
        val patched = patchTwitchWorkerScript("t.remove=$REMOVE", CLEANUP)
        assertEquals("t.remove=$CLEANUP", patched)
        assertNull(patchTwitchWorkerScript(patched!!, CLEANUP))
    }

    companion object {
        const val REMOVE = "function(e,t){for(var r=this.sourceBuffers,n=0,i=Object.keys(r);" +
            "n<i.length;n++)r[i[n]].remove(e,t)}"
        private const val CLEANUP = "(function(start,end){removeAllRequestedTrackData(start,end)})"
    }
}
