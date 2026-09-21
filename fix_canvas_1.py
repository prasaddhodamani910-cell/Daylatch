import re

with open("/storage/emulated/0/Projects/Daylatch/app/src/main/java/com/prasad/daylatch/MainActivity.kt", "r") as f:
    content = f.read()

# Fix CompletionMixDetailScreen
old_canvas = """                Canvas(Modifier.fillMaxSize()) {
                    val stroke = size.minDimension * .20f
                    var start = -90f
                    if (aCompSweep > 0f) drawArc(LocalDaylatchColors.current.primary, start, aCompSweep - 2f, false, style = Stroke(stroke))
                    start += aCompSweep
                    if (aSkipSweep > 0f) drawArc(LocalDaylatchColors.current.accent, start, aSkipSweep - 2f, false, style = Stroke(stroke))
                    start += aSkipSweep
                    if (aOpenSweep > 0f) drawArc(LocalDaylatchColors.current.mutedFg, start, aOpenSweep - 2f, false, style = Stroke(stroke))
                }"""
new_canvas = """                val cPrimary = LocalDaylatchColors.current.primary
                val cAccent = LocalDaylatchColors.current.accent
                val cMutedFg = LocalDaylatchColors.current.mutedFg
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = size.minDimension * .20f
                    var start = -90f
                    if (aCompSweep > 0f) drawArc(cPrimary, start, aCompSweep - 2f, false, style = Stroke(stroke))
                    start += aCompSweep
                    if (aSkipSweep > 0f) drawArc(cAccent, start, aSkipSweep - 2f, false, style = Stroke(stroke))
                    start += aSkipSweep
                    if (aOpenSweep > 0f) drawArc(cMutedFg, start, aOpenSweep - 2f, false, style = Stroke(stroke))
                }"""
content = content.replace(old_canvas, new_canvas)

# Fix MonthlyDonutChart
old_donut = """    Canvas(modifier) {
        val stroke = size.minDimension * .20f
        var start = -90f
        listOf(LocalDaylatchColors.current.primary to completed, LocalDaylatchColors.current.accent to skipped, LocalDaylatchColors.current.mutedFg to open).forEach { (color, amount) ->"""
new_donut = """    val cPrimary = LocalDaylatchColors.current.primary
    val cAccent = LocalDaylatchColors.current.accent
    val cMutedFg = LocalDaylatchColors.current.mutedFg
    Canvas(modifier) {
        val stroke = size.minDimension * .20f
        var start = -90f
        listOf(cPrimary to completed, cAccent to skipped, cMutedFg to open).forEach { (color, amount) ->"""
content = content.replace(old_donut, new_donut)

with open("/storage/emulated/0/Projects/Daylatch/app/src/main/java/com/prasad/daylatch/MainActivity.kt", "w") as f:
    f.write(content)
