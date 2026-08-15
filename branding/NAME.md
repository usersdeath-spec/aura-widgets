# Aura Widgets

## The name

Competitors, and what their names are doing:

| App | Rank | Name is… |
|---|---|---|
| Everything Widgets | 4.7★, 9K | a promise |
| Glass Widgets | #4 top paid | **a search term** |
| OneUI Widgets | #5 top paid | **a search term** |
| Material You Widgets | #9 top paid | **a search term** |

Three of the four top slots are held by apps named after a thing people already type into Play
search. "Prism" earns nothing: nobody searches for it, and it says nothing about the category.

**Aura Widgets** follows the pattern without borrowing anyone's design language. It reads as a
category name, it is short enough for the 30-character title limit with room for keywords, and the
word describes what the product does to a home screen.

Store title: **Aura Widgets — Glass, AMOLED, Minimal** (39 chars, so trim to
**Aura Widgets: Glass & AMOLED** at 29 if Play rejects it). The brand takes the front, the search
terms take the rest.

## The icon

Every top-seller in this category shares three properties, and each is doing a job:

1. **White ground.** The Play grid is dark. A white tile is the only thing that separates at
   thumbnail size, which is why three of the four leaders are light-on-white.
2. **The glyph is a widget arrangement.** OneUI uses four squares; Everything Widgets uses a circle,
   a heart and a pill. Users recognise the category before reading the name.
3. **Two or three flat shapes.** No text, no gradients on the shapes. Anything more disappears at
   48px.

Ours: a rounded tile, a circle, and a pill — a 2×2 home screen in miniature — over a single soft
violet-to-cyan glow. The composition is original; only the format conventions are shared. The glow
is the one gradient in the mark, and it is what the name refers to.

The themed (Android 13+) variant works because the three shapes differ in **form**, not just colour,
so the mark survives being painted a single system colour.

## What was not renamed

The Kotlin package stays `com.prism.studio`. It is invisible to users, and renaming it across 73
files risks breaking a build for no user-visible gain. `applicationId` — the identifier Play keys the
listing on, and the one that can never change after first publish — is now `com.aura.widgets`.
