package com.jubileegroup.jbuzz

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.jubileegroup.jbuzz.ui.theme.JBuzzTheme
import kotlin.math.abs

/**
 * Shows a composable as a drop-down (heads-up style) notification: it slides in from the top of the
 * screen, over whatever app is open, and then stays there. There is no timeout. It goes away when the
 * user swipes it away (up, or sideways), when the content calls its `dismiss` (the SMS card's close
 * button does), or when [dismiss] is called. (It is also hidden by the system while the device is
 * locked, and drawn again once it is unlocked.)
 *
 * Android 12+ no longer lets an app fully replace the layout of a normal notification, so this is
 * drawn as an overlay window instead. That needs the "Display over other apps" permission
 * ([canShow]); send the user to `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` to grant it.
 *
 * The notification can outlive the screen that showed it, so don't capture an Activity in the
 * content lambda.
 *
 * While it is showing, an [OverlayService] runs in the foreground so that Android does not freeze or
 * kill the process (and the window with it) once the app has been left.
 *
 * ```
 * DropDownNotification.show(context) { dismiss ->
 *     TruecallerSmsCard(modifier = Modifier.fillMaxWidth(), onClose = dismiss)
 * }
 * ```
 */
object DropDownNotification {

    // Transparent space between the card and the screen edges / status bar (from the reference).
    private val EdgeGap = 9.dp
    private val TopGap = 11.5.dp

    // How far a finger has to drag the card, up or sideways, to throw it away.
    private val SwipeDismissDistance = 32.dp

    private val main = Handler(Looper.getMainLooper())

    // Only ever holds the application context (see show), so nothing Activity-scoped can leak.
    @SuppressLint("StaticFieldLeak")
    private var current: Overlay? = null

    /** Whether the app may draw over other apps (Settings > Display over other apps). */
    fun canShow(context: Context): Boolean = Settings.canDrawOverlays(context)

    /** Whether a notification is on screen (or still sliding away). Call on the main thread. */
    fun isShowing(): Boolean = current != null

    /**
     * Slides [content] down from the top of the screen and keeps it there until `dismiss` is called
     * from code (by the content, or as [dismiss]). Does nothing if [canShow] is false. A notification
     * that is already showing is replaced.
     */
    fun show(
        context: Context,
        content: @Composable (dismiss: () -> Unit) -> Unit
    ) {
        val appContext = context.applicationContext
        main.post {
            if (!canShow(appContext)) return@post
            // The service is already running when a notification is being replaced, so keep it.
            current?.remove(stopService = false)
            current = Overlay(appContext, content).also { it.attach() }
            OverlayService.start(appContext)
        }
    }

    /** Slides away the notification that is currently showing, if any. */
    fun dismiss() {
        main.post { current?.hide() }
    }

    /** One overlay window hosting the composable. It is its own lifecycle owner, as Compose needs one. */
    private class Overlay(
        private val context: Context,
        private val content: @Composable (dismiss: () -> Unit) -> Unit
    ) : SavedStateRegistryOwner {

        private val windowManager = context.getSystemService(WindowManager::class.java)
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateController = SavedStateRegistryController.create(this)

        // Starts hidden and immediately targets visible, so the slide-in plays on first composition.
        private val visibleState = MutableTransitionState(false).apply { targetState = true }
        private var view: ComposeView? = null

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateController.savedStateRegistry

        fun attach() {
            savedStateController.performRestore(null)
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED

            val composeView = ComposeView(context).apply {
                setViewTreeLifecycleOwner(this@Overlay)
                setViewTreeSavedStateRegistryOwner(this@Overlay)
                // Not named Content(): inside this apply block that would resolve to ComposeView.Content()
                // and recurse forever.
                setContent { JBuzzTheme { NotificationContent() } }
            }
            view = composeView
            windowManager.addView(composeView, layoutParams())
        }

        /** Starts the slide-away animation; the window is removed once it has finished. */
        fun hide() {
            visibleState.targetState = false
        }

        fun remove(stopService: Boolean = true) {
            val composeView = view ?: return
            view = null
            if (current === this) current = null
            runCatching { windowManager.removeViewImmediate(composeView) }
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            if (stopService) OverlayService.stop(context)
        }

        @Composable
        private fun NotificationContent() {
            val dismiss = remember { { hide() } }

            // Take the window down once the slide-away has finished.
            if (visibleState.isIdle && !visibleState.currentState) {
                LaunchedEffect(Unit) { main.post { remove() } }
            }

            AnimatedVisibility(
                visibleState = visibleState,
                enter = slideInVertically(tween(320, easing = LinearOutSlowInEasing)) { -it } +
                    fadeIn(tween(200)),
                exit = slideOutVertically(tween(260, easing = FastOutLinearInEasing)) { -it } +
                    fadeOut(tween(200))
            ) {
                // Transparent gap between the card and the screen edges / status bar. The card itself
                // has no margin, shadow or backdrop. Content narrower than the window is centred.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dismissOnSwipe(SwipeDismissDistance, dismiss)
                        .padding(start = EdgeGap, top = TopGap, end = EdgeGap),
                    contentAlignment = Alignment.TopCenter
                ) {
                    content(dismiss)
                }
            }
        }

        private fun layoutParams() = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Never steal focus, and let touches outside the card reach the app underneath.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
        }
    }
}

/**
 * Calls [onDismiss] once a finger has dragged [distance] up, or [distance] to either side. A drag only
 * starts after the touch slop has been passed, so taps on the content are not affected.
 */
private fun Modifier.dismissOnSwipe(distance: Dp, onDismiss: () -> Unit): Modifier =
    pointerInput(onDismiss) {
        val threshold = distance.toPx()
        var up = 0f
        var sideways = 0f
        var dismissed = false
        detectDragGestures(
            onDragStart = {
                up = 0f
                sideways = 0f
                dismissed = false
            },
            onDrag = { _, drag ->
                up -= drag.y
                sideways += drag.x
                if (!dismissed && (up >= threshold || abs(sideways) >= threshold)) {
                    dismissed = true
                    onDismiss()
                }
            }
        )
    }
