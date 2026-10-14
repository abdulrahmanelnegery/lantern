package dev.dawem.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** The Dawem mark (the loop with its check), drawn from the brand image; each platform loads its own copy. */
@Composable
expect fun BrandMark(modifier: Modifier)
