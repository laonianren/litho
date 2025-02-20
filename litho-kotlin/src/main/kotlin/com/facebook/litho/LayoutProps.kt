/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho

import androidx.annotation.ColorInt
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaPositionType

inline fun <C : Component.Builder<C>> ComponentContext.Padding(
    all: Dp,
    content: ComponentContext.() -> C
): C =
    content().paddingDip(YogaEdge.ALL, all.dp.toFloat())

inline fun <C : Component.Builder<C>> ComponentContext.Padding(
    horizontal: Dp = 0.dp,
    vertical: Dp = 0.dp,
    content: ComponentContext.() -> C
): C =
    content()
        .paddingDip(YogaEdge.HORIZONTAL, horizontal.dp.toFloat())
        .paddingDip(YogaEdge.VERTICAL, vertical.dp.toFloat())

inline fun <C : Component.Builder<C>> ComponentContext.Padding(
    left: Dp = 0.dp,
    top: Dp = 0.dp,
    right: Dp = 0.dp,
    bottom: Dp = 0.dp,
    content: ComponentContext.() -> C
): C =
    content()
        .paddingDip(YogaEdge.LEFT, left.dp.toFloat())
        .paddingDip(YogaEdge.TOP, top.dp.toFloat())
        .paddingDip(YogaEdge.RIGHT, right.dp.toFloat())
        .paddingDip(YogaEdge.BOTTOM, bottom.dp.toFloat())

/**
 * Builder for positioning a child component absolutely within its parent's edges, independent of
 * its siblings. [left], [top], [right], [bottom] specify the offset of the child's respective side
 * from the same side of the parent.
 */
inline fun <C : Component.Builder<C>> ComponentContext.Position(
    left: Dp? = null,
    top: Dp? = null,
    right: Dp? = null,
    bottom: Dp? = null,
    content: ComponentContext.() -> C
): C =
    content()
        .positionType(YogaPositionType.ABSOLUTE)
        .apply {
          left?.let { positionDip(YogaEdge.LEFT, it.dp.toFloat()) }
          top?.let { positionDip(YogaEdge.TOP, it.dp.toFloat()) }
          right?.let { positionDip(YogaEdge.RIGHT, it.dp.toFloat()) }
          bottom?.let { positionDip(YogaEdge.BOTTOM, it.dp.toFloat()) }
        }

/**
 * Builder for decorating a child component with [backgroundColor] or [foregroundColor].
 */
inline fun <C : Component.Builder<C>> ComponentContext.Decoration(
    @ColorInt foregroundColor: Int? = null,
    @ColorInt backgroundColor: Int? = null,
    content: ComponentContext.() -> C
): C =
    content()
        .apply {
          foregroundColor?.let { foregroundColor(it) }
          backgroundColor?.let { backgroundColor(it) }
        }

/**
 * Builder for decorating a child component with [backgroundColor] or [foregroundColor].
 */
inline fun <C : Component.Builder<C>> ComponentContext.Decoration(
    @ColorInt foregroundColor: Long? = null,
    @ColorInt backgroundColor: Long? = null,
    content: ComponentContext.() -> C
): C =
    Decoration(foregroundColor?.toInt(), backgroundColor?.toInt(), content)
