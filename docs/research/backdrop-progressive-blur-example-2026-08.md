# Backdrop Progressive Blur Example

Research date: 2026-08-04

## Primary sources

- Example screen: https://github.com/Kyant0/AndroidLiquidGlass/blob/kmp/app/src/commonMain/kotlin/com/kyant/backdrop/catalog/destinations/ProgressiveBlurContent.kt
- Example scaffold: https://github.com/Kyant0/AndroidLiquidGlass/blob/kmp/app/src/androidMain/kotlin/com/kyant/backdrop/catalog/BackdropDemoScaffold.kt
- Progressive blur documentation: https://kyant.gitbook.io/backdrop/tutorials/progressive-blur.md

## Findings

The example uses `drawPlainBackdrop` with this effect order:

1. `blur(4f.dp.toPx())`
2. `runtimeShaderEffect("AlphaMask", ...)`

The shader evaluates `smoothstep(size.y, size.y * 0.5, coord.y)`. This makes the top of the overlay fully blurred and fades the blurred result toward transparent as the overlay reaches its lower half. The transparent part reveals the original content below, which creates the progressive transition.

The example creates one `rememberLayerBackdrop`, applies `Modifier.layerBackdrop(backdrop)` to the content source, and passes the same backdrop to `drawPlainBackdrop`. The source and effect are separate layers.

ClipFlow follows the same structure, but places the source layer inside `BackgroundWallpaperLayer` so the wallpaper remains outside the captured content. The title controls are drawn after the backdrop effect and stay sharp.

The example also mixes a white tint in the shader. ClipFlow omits that high-opacity tint because it creates the opaque white appearance reported during the previous implementation.
