/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.flipper.plugins.uidebugger.observers

import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import com.facebook.flipper.plugins.uidebugger.LogTag
import com.facebook.flipper.plugins.uidebugger.core.Context
import java.lang.ref.WeakReference

typealias DecorView = View

/** Responsible for subscribing to updates to the content view of an activity */
class DecorViewObserver(val context: Context) : TreeObserver<DecorView>() {

  val throttleTimeMs = 500

  private var nodeRef: WeakReference<View>? = null
  private var listener: ViewTreeObserver.OnPreDrawListener? = null

  override val type = "DecorView"

  override fun subscribe(node: Any) {
    node as View
    nodeRef = WeakReference(node)

    Log.i(LogTag, "Subscribing to decor view changes")

    // TODO: there's a problem with this. Some future changes may have been
    // ignored and not sent. Need to keep track of the last one, always and react
    // accordingly.
    listener =
        object : ViewTreeObserver.OnPreDrawListener {
          var lastSend = 0L
          override fun onPreDraw(): Boolean {
            if (System.currentTimeMillis() - lastSend > throttleTimeMs) {
              traverseAndSend(context, node)

              lastSend = System.currentTimeMillis()
            }
            return true
          }
        }

    node.viewTreeObserver.addOnPreDrawListener(listener)
  }

  override fun unsubscribe() {
    Log.i(LogTag, "Unsubscribing from decor view changes")

    listener.let {
      nodeRef?.get()?.viewTreeObserver?.removeOnPreDrawListener(it)
      listener = null
      nodeRef = null
    }
  }
}

object DecorViewTreeObserverBuilder : TreeObserverBuilder<DecorView> {
  override fun canBuildFor(node: Any): Boolean {
    return node.javaClass.simpleName.contains("DecorView")
  }

  override fun build(context: Context): TreeObserver<DecorView> {
    Log.i(LogTag, "Building DecorView observer")
    return DecorViewObserver(context)
  }
}
