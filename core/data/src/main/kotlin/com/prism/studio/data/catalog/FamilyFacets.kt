package com.prism.studio.data.catalog

import com.prism.studio.model.Facet
import com.prism.studio.model.Facet.*

/**
 * The browse vocabulary, per family.
 *
 * Hand-tagged rather than derived. Deriving facets from palette maths was the first attempt and it
 * produced things like Luxury Gold tagged "Dark, Monochrome" — technically true of its pixels and
 * useless to a person browsing. What a family *feels* like is an authoring decision, so it is
 * authored.
 *
 * Rule of thumb applied throughout: three to five facets per family. One is unfindable, seven means
 * the family has no point of view.
 */
internal val FAMILY_FACETS: Map<String, Set<Facet>> = mapOf(
    // Foundation
    "minimal-mono" to setOf(Minimal, Monochrome, Dark, Calm),
    "amoled-black" to setOf(Amoled, Dark, Minimal, Technical),
    "luxury-gold" to setOf(Luxury, Dark, Warm, Editorial),

    // Liquid Glass collection
    "liquid-glass-clear" to setOf(Glass, Cool, Minimal, Luxury),
    "liquid-glass-smoked" to setOf(Glass, Dark, Luxury, Cool),
    "liquid-glass-prism" to setOf(Glass, Colourful, Luxury, Futuristic),

    // Glass & Material
    "frosted-crystal" to setOf(Glass, Cool, Light, Minimal),
    "sea-glass" to setOf(Glass, Calm, Nature, Cool),
    "chrome-liquid" to setOf(Futuristic, Bold, Cool, Textured),
    "marble" to setOf(Luxury, Light, Textured, Editorial),
    "origami" to setOf(Geometric, Playful, Warm, Light),
    "paper-cut" to setOf(Minimal, Light, Playful, Productivity),

    // Structure & Restraint
    "scandinavian" to setOf(Minimal, Light, Calm, Warm),
    "japanese-zen" to setOf(Minimal, Calm, Light, Editorial),
    "swiss-grid" to setOf(Minimal, Geometric, Light, Editorial),
    "bauhaus-primary" to setOf(Geometric, Colourful, Bold, Playful),
    "brutalist-slab" to setOf(Bold, Dark, Geometric, Textured),
    "ink-serif" to setOf(Editorial, Dark, Luxury, Calm),
    "material-you" to setOf(Colourful, Minimal, Playful),
    "neumorph-soft" to setOf(Minimal, Light, Calm, Textured),

    // Colour & Light
    "aurora" to setOf(Colourful, Dark, Cool, Space),
    "gradient-flow" to setOf(Colourful, Bold, Cool),
    "sunset-fade" to setOf(Colourful, Warm, Bold),
    "candy-pop" to setOf(Playful, Colourful, Bold, Light),
    "terracotta" to setOf(Warm, Nature, Textured, Calm),
    "botanical" to setOf(Nature, Dark, Calm, Warm),
    "nordic-frost" to setOf(Cool, Dark, Minimal, Calm),
    "seasonal-bloom" to setOf(Warm, Playful, Nature, Light),

    // Depth
    "cosmic-drift" to setOf(Space, Dark, Colourful, Calm),
    "deep-space" to setOf(Space, Amoled, Dark, Technical),
    "monolith" to setOf(Dark, Minimal, Amoled, Bold),

    // Signal
    "cyberpunk-neon" to setOf(Futuristic, Dark, Bold, Gaming),
    "hud-tactical" to setOf(Futuristic, Technical, Amoled, Gaming),
    "rgb-gaming" to setOf(Gaming, Bold, Dark, Futuristic),
    "pixel-retro" to setOf(Retro, Playful, Gaming, Colourful),
    "crt-amber" to setOf(Retro, Technical, Amoled, Warm),
    "blueprint" to setOf(Technical, Geometric, Cool, Dark),

    // Purpose
    "executive-slate" to setOf(Productivity, Dark, Minimal, Cool),
    "ledger" to setOf(Finance, Light, Editorial, Productivity),
    "pulse" to setOf(Fitness, Amoled, Bold, Dark),
    "vinyl" to setOf(Music, Warm, Dark, Retro),
    "focus-grid" to setOf(Productivity, Dark, Minimal, Technical),

    // Precious Materials
    "titanium" to setOf(Textured, Cool, Technical, Luxury),
    "mercury" to setOf(Futuristic, Luxury, Cool, Bold),
    "quartz" to setOf(Glass, Light, Geometric, Cool),
    "velvet" to setOf(Luxury, Dark, Warm, Calm),
    "obsidian" to setOf(Dark, Luxury, Minimal, Amoled),
    "carbon" to setOf(Technical, Dark, Textured, Gaming),
    "pearl" to setOf(Light, Luxury, Colourful, Calm),
    "gemstone" to setOf(Luxury, Bold, Colourful, Dark),
    "satin" to setOf(Glass, Calm, Minimal, Cool),

    // Phenomena
    "holographic" to setOf(Colourful, Playful, Futuristic, Bold),
    "neon-frost" to setOf(Glass, Futuristic, Dark, Colourful),
    "eclipse" to setOf(Space, Amoled, Dark, Luxury),
    "solar" to setOf(Warm, Bold, Colourful, Space),
    "abyssal" to setOf(Nature, Dark, Cool, Calm),
    "horizon" to setOf(Nature, Cool, Minimal, Warm),
    "mirage" to setOf(Warm, Retro, Textured, Calm),
    "studio" to setOf(Minimal, Light, Calm, Editorial),
)
