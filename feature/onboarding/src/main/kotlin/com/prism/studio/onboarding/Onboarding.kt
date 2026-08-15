package com.prism.studio.onboarding

import com.prism.studio.model.FamilyId
import com.prism.studio.model.HomeSetup

/**
 * The first sixty seconds.
 *
 * The shape of this flow is a product decision, not a template. Four steps, and the user is looking
 * at a real rendered widget by the end of step two — because the fastest way to explain what this
 * app does is to show the thing it makes, at full quality, before asking for anything.
 *
 * Three rules the flow follows:
 *
 * 1. **Nothing is explained that can be shown.** There is no "swipe through five feature cards"
 *    stage. Step 2 hands the user a real choice with live previews; the features explain themselves
 *    as a consequence.
 * 2. **No permission is requested here.** Not one. Prism needs zero permissions to show a clock, a
 *    battery widget, or a wallpaper, and the permissions it can use (calendar, location, activity)
 *    are requested at the moment a widget that needs them is placed, with the reason attached. An
 *    onboarding permission wall is the most common reason a paid app gets refunded in the first
 *    minute.
 * 3. **It can be skipped from step one**, and skipping lands on the setup gallery, which is a
 *    perfectly good first screen. Onboarding that traps you is onboarding that gets one star.
 *
 * The "wow" moment is [Step.Preview]: the user's chosen look rendered onto a live home-screen
 * preview with their own wallpaper behind it, before they have committed to anything.
 */
enum class Step {
    /**
     * One sentence and the mark. No carousel.
     *
     * Copy: "Prism makes home screens. 59 design families, 708 widgets, 143 wallpapers — all
     * yours, once." The numbers are read from the catalog at runtime, never hardcoded, so the
     * first thing the user reads can never be out of date.
     */
    Welcome,

    /**
     * Pick a starting point: six setups shown as full-screen previews, not a grid of chips.
     *
     * Six because it fits one screen without scrolling on a small phone, and because a choice
     * between six feels curated while a choice between twenty-eight feels like work. The six span
     * the range deliberately — minimal, glass, AMOLED, luxury, nature, productivity — so whatever
     * the user came for is represented.
     */
    Choose,

    /**
     * The wow moment. Their pick, rendered live over their current wallpaper.
     *
     * This is the same render path the home screen uses, so nothing here is a mock-up. The user can
     * swap wallpapers and watch every widget re-tint through Match Wallpaper — which demonstrates
     * the single most impressive thing the app does without a word of explanation.
     */
    Preview,

    /**
     * Place the first widget, walked through.
     *
     * Android's widget picker is genuinely confusing, and a paid customisation app that leaves a
     * first-time user stranded in it has lost them. This step shows a short animated demonstration
     * of the long-press → Widgets → Prism path, then hands off. It is the only step with any
     * instructional content, and it exists because the platform makes it necessary.
     */
    Place;

    companion object {
        /** Skippable from the very first step. */
        val skippableFrom = Welcome
    }
}

/**
 * The six setups offered at [Step.Choose].
 *
 * Chosen to span the range rather than to show off. If someone opens this app wanting a black
 * screen with a clock on it, they must see that option immediately, not on page three.
 */
val ONBOARDING_PICKS = listOf(
    "quiet-hours",    // minimal
    "clear-morning",  // liquid glass
    "lights-out",     // amoled
    "after-hours",    // luxury
    "greenhouse",     // nature
    "the-week",       // productivity
)

/**
 * Permission requests, each bound to the widget that needs it.
 *
 * The rationale is written as a trade, not a plea: what the user gets, what we read, and what
 * happens if they say no. Every one of these is genuinely optional — declining degrades one widget
 * to a placeholder and nothing else, and the copy says so, because a rationale that hides the
 * decline path is a dark pattern.
 */
enum class PermissionAsk(
    val permission: String,
    val trigger: String,
    val rationale: String,
    val ifDeclined: String,
) {
    Calendar(
        "android.permission.READ_CALENDAR",
        "Placing an Agenda or Month widget",
        "Agenda widgets show your next events. Prism reads your calendar on your phone to draw them, and sends nothing anywhere.",
        "The widget shows the month grid without event marks. Everything else works.",
    ),
    Location(
        "android.permission.ACCESS_COARSE_LOCATION",
        "Placing a Weather or Sunrise widget",
        "Weather needs a rough location — city level, not your street — to know which forecast to show.",
        "You can set a city by hand instead. Nothing else changes.",
    ),
    Activity(
        "android.permission.ACTIVITY_RECOGNITION",
        "Placing a Steps or Activity widget",
        "Step widgets read the step counter your phone already keeps. Prism stores nothing and uploads nothing.",
        "The widget shows a placeholder and can be removed. No other widget is affected.",
    ),
    Notifications(
        "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
        "Placing a Now Playing widget",
        "Now Playing reads the media session of whatever app is currently playing, so the widget can show the track and controls.",
        "The music widget stays empty. Consider removing it rather than living with a blank card.",
    ),
}

/** What the flow produces. Applied only if the user confirms at [Step.Preview]. */
data class OnboardingResult(
    val chosenSetup: HomeSetup?,
    val chosenFamily: FamilyId?,
    val applyWallpaper: Boolean,
    val skipped: Boolean,
)
